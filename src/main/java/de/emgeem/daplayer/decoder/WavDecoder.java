package de.emgeem.daplayer.decoder;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import javax.sound.sampled.AudioFormat;

import de.emgeem.daplayer.model.Track;

public class WavDecoder implements AudioDecoder {

    private RandomAccessFile raf;
    private long dataStart;
    private long dataEnd;
    private AudioFormat format;
    private File file;

    private int sampleRate;
    private int channels;
    private int bitDepth;

    @Override
    public boolean supports(File file) {
        return file.getName().toLowerCase().endsWith(".wav");
    }

    @Override
    public Track readTrack(File file) {
        Track track = new Track(file);
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            if (!readString(raf, 4).equals("RIFF")) {return track;}
            raf.skipBytes(4);
            if (!readString(raf, 4).equals("WAVE")) {return track;}
            int sr = 0;
            int ch = 0;
            int bd = 0;
            long dataSize = 0;
            while (raf.getFilePointer() + 8 <= raf.length()) {
                String chunk = readString(raf, 4);
                long chunkSize = readUInt32(raf);
                long chunkEnd = raf.getFilePointer() + chunkSize;
                if (chunk.equals("fmt ")) {
                    int audioFormat = readUInt16(raf);
                    ch = readUInt16(raf);
                    sr = (int) readUInt32(raf);
                    raf.skipBytes(4);
                    raf.skipBytes(2);
                    bd = readUInt16(raf);
                    if (audioFormat != 1) {return track;}
                } else if (chunk.equals("data")) {
                    dataSize = chunkSize;
                } else if (chunk.equals("LIST")) {
                    readInfoChunk(raf, chunkEnd, track);
                } else if (chunk.equals("bext")) {
                    readBextChunk(raf, chunkEnd);
                }
                raf.seek(chunkEnd);
                if ((chunkSize & 1) != 0) {raf.skipBytes(1);}
            }
            if (sr > 0 && ch > 0 && bd > 0) {
                track.setSampleRate(sr);
                track.setChannels(ch);
                track.setBitDepth(bd);
                long bytesPerSecond = (long) sr * ch * bd / 8;
                if (bytesPerSecond > 0) {track.setDuration(dataSize * 1000 / bytesPerSecond);}
            }
        } catch (IOException e) {
            System.err.println("Could not read WAV file: " + file.getName());
        }
        if (track.getTitle() == null || track.getTitle().isBlank()) {
            track.setTitle(track.getFileName());
        }
        return track;
    }

    private void readInfoChunk(RandomAccessFile raf, long chunkEnd, Track track) throws IOException {
        if (raf.getFilePointer() + 4 > chunkEnd) return;
        String listType = readString(raf, 4);
        if (!listType.equals("INFO")) return;
        while (raf.getFilePointer() + 8 <= chunkEnd) {
            String id = readString(raf, 4);
            long size = readUInt32(raf);
            if (size > chunkEnd - raf.getFilePointer()) break;
            byte[] data = new byte[(int) size];
            raf.readFully(data);
            String value = new String(data, StandardCharsets.ISO_8859_1).replace("\0", "").trim();
            switch (id) {
                case "INAM" -> track.setTitle(value);
                case "IART" -> track.setArtist(value);
                case "IPRD" -> track.setAlbum(value);
            }
            if ((size & 1) != 0) {raf.skipBytes(1);}
        }
    }

    private String readString(RandomAccessFile raf, int length) throws IOException {
        byte[] buffer = new byte[length];
        raf.readFully(buffer);
        return new String(buffer, StandardCharsets.US_ASCII);
    }

    private int readUInt16(RandomAccessFile raf) throws IOException {
        int b0 = raf.readUnsignedByte();
        int b1 = raf.readUnsignedByte();
        return b0 | (b1 << 8);
    }

    private long readUInt32(RandomAccessFile raf) throws IOException {
        long b0 = raf.readUnsignedByte();
        long b1 = raf.readUnsignedByte();
        long b2 = raf.readUnsignedByte();
        long b3 = raf.readUnsignedByte();
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    private void readBextChunk(RandomAccessFile raf, long chunkEnd) throws IOException {
        byte[] data = new byte[(int) (chunkEnd - raf.getFilePointer())];
        raf.readFully(data);
        String description = new String(data, 0, 256, StandardCharsets.ISO_8859_1).replace("\0", "").trim();
        String originator = new String(data, 256, 32, StandardCharsets.ISO_8859_1).replace("\0", "").trim();
        String originatorReference = new String(data, 288, 32, StandardCharsets.ISO_8859_1).replace("\0", "").trim();
        String originationDate = new String(data, 320, 10, StandardCharsets.US_ASCII).replace("\0", "").trim();
        String originationTime = new String(data, 330, 8, StandardCharsets.US_ASCII).replace("\0", "").trim();

    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    @Override
    public void open(File file) throws Exception {
        this.file = file;
        raf = new RandomAccessFile(file, "r");

        if (!readString(4).equals("RIFF")) {
            throw new IOException("Not a RIFF file");
        }

        raf.skipBytes(4); // file size

        if (!readString(4).equals("WAVE")) {
            throw new IOException("Not a WAVE file");
        }

        sampleRate = 0;
        channels = 0;
        bitDepth = 0;

        while (raf.getFilePointer() + 8 <= raf.length()) {
            String chunkId = readString(4);
            long chunkSize = readUInt32();
            long chunkEnd = raf.getFilePointer() + chunkSize;

            if (chunkId.equals("fmt ")) {
                int audioFormat = readUInt16();
                channels = readUInt16();
                sampleRate = (int) readUInt32();
                raf.skipBytes(4); // byteRate
                raf.skipBytes(2); // blockAlign
                bitDepth = readUInt16();

                if (audioFormat != 1) {
                    throw new IOException("Unsupported WAV format (not PCM)");
                }

                format = new AudioFormat(
                    sampleRate,
                    bitDepth,      // wichtig: original BitTiefe beibehalten
                    channels,
                    true,
                    false
                );

            } else if (chunkId.equals("data")) {
                dataStart = raf.getFilePointer();
                dataEnd = dataStart + chunkSize;
                raf.seek(dataStart);
                break;
            }

            raf.seek(chunkEnd);
            if ((chunkSize & 1) != 0) raf.skipBytes(1);
        }

        if (format == null) {
            throw new IOException("Missing fmt chunk");
        }
    }

    @Override
    public int decode(byte[] buffer) throws Exception {
        if (raf == null) return -1;

        long remaining = dataEnd - raf.getFilePointer();
        if (remaining <= 0) return -1;

        // 24-Bit WAV → manuelle Konvertierung
        if (bitDepth == 24) {

            int maxSamples = buffer.length / 2;      // 2 Bytes pro 16-Bit Sample
            int rawBytesToRead = maxSamples * 3;     // 3 Bytes pro 24-Bit Sample

            rawBytesToRead = (int) Math.min(rawBytesToRead, remaining);

            byte[] raw = new byte[rawBytesToRead];
            int read = raf.read(raw);
            if (read <= 0) return -1;


            int samples = read / 3;
            int outLen = samples * 2;

            for (int i = 0; i < samples; i++) {
                int b0 = raw[i * 3] & 0xFF;
                int b1 = raw[i * 3 + 1] & 0xFF;
                int b2 = raw[i * 3 + 2];

                int sample24 = (b2 << 16) | (b1 << 8) | b0;
                short sample16 = (short) (sample24 >> 8);

                buffer[i * 2]     = (byte) (sample16 & 0xFF);
                buffer[i * 2 + 1] = (byte) ((sample16 >> 8) & 0xFF);
            }

            return outLen;
        }

        // 16-Bit WAV → direkt lesen
        int toRead = (int) Math.min(buffer.length, remaining);
        return raf.read(buffer, 0, toRead);
    }

    @Override
    public AudioFormat getAudioFormat() {
        return format;
    }

    @Override
    public void reset() throws Exception {
        close();
        open(file);
    }

    @Override
    public void close() throws Exception {
        if (raf != null) {
            raf.close();
            raf = null;
        }
    }

    // --- Hilfsfunktionen ---
    private String readString(int len) throws IOException {
        byte[] buf = new byte[len];
        raf.readFully(buf);
        return new String(buf, StandardCharsets.US_ASCII);
    }

    private int readUInt16() throws IOException {
        int b0 = raf.readUnsignedByte();
        int b1 = raf.readUnsignedByte();
        return b0 | (b1 << 8);
    }

    private long readUInt32() throws IOException {
        long b0 = raf.readUnsignedByte();
        long b1 = raf.readUnsignedByte();
        long b2 = raf.readUnsignedByte();
        long b3 = raf.readUnsignedByte();
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    private Track readMetadata(File file) {
        return new Track(file);
    }
}
