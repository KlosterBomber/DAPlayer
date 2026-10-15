package de.emgeem.daplayer.decoder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;

import de.emgeem.daplayer.model.Track;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

public class Mp3Decoder implements AudioDecoder {

    private static final int MAX_SKIPPED_FRAMES = 64;

    private File file;
    private Bitstream bitstream;
    private Decoder decoder;
    private AudioFormat format;
    /** Rest eines dekodierten Frames, der nicht mehr in den Ausgabepuffer passte. */
    private byte[] pending = new byte[0];
    private int pendingOffset;

    @Override
    public boolean supports(File file) {
        return file.getName().toLowerCase().endsWith(".mp3");
    }

    @Override
    public Track readTrack(File file) {
        Track track = new Track(file);
        TagReader.read(file, track);
        if (track.getChannels() == 0 || track.getSampleRate() == 0) {
            try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
                Bitstream bs = new Bitstream(in);
                Header header = bs.readFrame();
                if (header != null) {
                    track.setSampleRate(header.frequency());
                    track.setChannels(header.mode() == Header.SINGLE_CHANNEL ? 1 : 2);
                }
                bs.close();
            } catch (Exception ignored) {}
        }
        return track;
    }

    @Override
    public void open(File file) throws Exception {
        this.file = file;
        bitstream = new Bitstream(new BufferedInputStream(new FileInputStream(file), 64 * 1024));
        decoder = new Decoder();
        pending = new byte[0];
        pendingOffset = 0;
        Header header = bitstream.readFrame();
        if (header == null) {
            throw new IOException("MP3 header not readable");
        }
        int sampleRate = header.frequency();
        int channels = header.mode() == Header.SINGLE_CHANNEL ? 1 : 2;
        format = new AudioFormat(sampleRate, 16, channels, true, false);
        bitstream.unreadFrame();
    }

    @Override
    public int decode(byte[] buffer) throws Exception {
        if (pendingOffset >= pending.length && !decodeNextFrame()) {return -1;}
        int frameSize = format.getFrameSize();
        int available = pending.length - pendingOffset;
        int toCopy = Math.min(available, buffer.length - buffer.length % frameSize);
        System.arraycopy(pending, pendingOffset, buffer, 0, toCopy);
        pendingOffset += toCopy;
        return toCopy;
    }

    /** Dekodiert den nächsten gültigen Frame nach {@link #pending}. Beschädigte Frames werden übersprungen. */
    private boolean decodeNextFrame() throws BitstreamException {
        for (int attempt = 0; attempt < MAX_SKIPPED_FRAMES; attempt++) {
            Header header = bitstream.readFrame();
            if (header == null) {return false;}
            try {
                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                // WICHTIG: getBuffer() ist größer als der gültige Bereich -> getBufferLength() verwenden
                short[] pcm = output.getBuffer();
                int samples = output.getBufferLength();
                int outChannels = format.getChannels();
                int decodedChannels = output.getChannelCount();
                byte[] bytes;
                if (decodedChannels == outChannels) {
                    bytes = new byte[samples * 2];
                    for (int i = 0; i < samples; i++) {
                        bytes[i * 2] = (byte) (pcm[i] & 0xff);
                        bytes[i * 2 + 1] = (byte) ((pcm[i] >> 8) & 0xff);
                    }
                } else {
                    // Kanalwechsel mitten im Stream (selten): auf Ausgabeformat abbilden
                    int frames = samples / decodedChannels;
                    bytes = new byte[frames * outChannels * 2];
                    for (int f = 0; f < frames; f++) {
                        for (int c = 0; c < outChannels; c++) {
                            short s = pcm[f * decodedChannels + Math.min(c, decodedChannels - 1)];
                            int o = (f * outChannels + c) * 2;
                            bytes[o] = (byte) (s & 0xff);
                            bytes[o + 1] = (byte) ((s >> 8) & 0xff);
                        }
                    }
                }
                pending = bytes;
                pendingOffset = 0;
                if (bytes.length > 0) {return true;}
            } catch (DecoderException | RuntimeException e) {
                // beschädigter Frame -> überspringen
            } finally {
                bitstream.closeFrame();
            }
        }
        return false;
    }

    @Override
    public AudioFormat getAudioFormat() {return format;}

    @Override
    public void seekToMillis(long positionMillis) throws Exception {
        if (bitstream == null) {return;}
        positionMillis = Math.max(0, positionMillis);
        reset();
        double currentMillis = 0;
        while (currentMillis < positionMillis) {
            Header header = bitstream.readFrame();
            if (header == null) {break;}
            currentMillis += header.ms_per_frame();
            bitstream.closeFrame();
        }
        // Decoder-Zustand (Bit-Reservoir) neu aufbauen
        decoder = new Decoder();
    }

    @Override
    public void reset() throws Exception {close(); open(file);}

    @Override
    public void close() throws Exception {
        if (bitstream != null) {
            bitstream.close();
            bitstream = null;
        }
        pending = new byte[0];
        pendingOffset = 0;
    }
}
