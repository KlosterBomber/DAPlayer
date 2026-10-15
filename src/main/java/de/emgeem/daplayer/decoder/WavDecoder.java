package de.emgeem.daplayer.decoder;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import javax.sound.sampled.AudioFormat;

import de.emgeem.daplayer.model.Track;

/**
 * WAV-Decoder für PCM 8/16/24/32 Bit (Integer), 32/64 Bit Float sowie
 * WAVE_FORMAT_EXTENSIBLE. Ausgabe ist immer 16 Bit signed little-endian.
 */
public class WavDecoder implements AudioDecoder {

    static final int FORMAT_PCM = 0x0001;
    static final int FORMAT_FLOAT = 0x0003;
    static final int FORMAT_EXTENSIBLE = 0xFFFE;

    private RandomAccessFile raf;
    private long dataStart;
    private long dataEnd;
    private AudioFormat format;
    private File file;

    private int sampleRate;
    private int channels;
    private int outChannels;
    private int bitDepth;
    private int blockAlign;
    private boolean floatingPoint;
    private byte[] raw = new byte[0];

    @Override
    public boolean supports(File file) {
        return file.getName().toLowerCase().endsWith(".wav");
    }

    @Override
    public Track readTrack(File file) {
        Track track = new Track(file);
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            if (!readString(raf, 4).equals("RIFF")) {return finish(track);}
            raf.skipBytes(4);
            if (!readString(raf, 4).equals("WAVE")) {return finish(track);}
            FmtInfo fmt = null;
            long dataSize = 0;
            while (raf.getFilePointer() + 8 <= raf.length()) {
                String chunk = readString(raf, 4);
                long chunkSize = readUInt32(raf);
                long chunkEnd = raf.getFilePointer() + chunkSize;
                if (chunk.equals("fmt ")) {
                    fmt = readFmt(raf, chunkSize);
                } else if (chunk.equals("data")) {
                    dataSize = Math.min(chunkSize, raf.length() - raf.getFilePointer());
                } else if (chunk.equals("LIST")) {
                    readInfoChunk(raf, chunkEnd, track);
                }
                raf.seek(chunkEnd);
                if ((chunkSize & 1) != 0) {raf.skipBytes(1);}
            }
            if (fmt != null && fmt.sampleRate > 0 && fmt.blockAlign > 0) {
                track.setSampleRate(fmt.sampleRate);
                track.setChannels(fmt.channels);
                track.setBitDepth(fmt.bitDepth);
                track.setBitRate((int) ((long) fmt.sampleRate * fmt.blockAlign * 8 / 1000));
                track.setDuration(dataSize / fmt.blockAlign * 1000L / fmt.sampleRate);
            }
        } catch (IOException e) {
            System.err.println("Could not read WAV file: " + file.getName());
        }
        return finish(track);
    }

    private Track finish(Track track) {
        if (track.getTitle() == null || track.getTitle().isBlank()) {track.setTitle(track.getFileName());}
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
            String value = decodeText(data);
            switch (id) {
                case "INAM" -> track.setTitle(value);
                case "IART" -> track.setArtist(value);
                case "IPRD" -> track.setAlbum(value);
                case "IGNR" -> track.setGenre(value);
                case "ICRD" -> track.setYear(TagReader.parseLeadingInt(value));
                case "ITRK", "IPRT" -> track.setTrackNumber(TagReader.parseLeadingInt(value));
                default -> { }
            }
            if ((size & 1) != 0 && raf.getFilePointer() < chunkEnd) {raf.skipBytes(1);}
        }
    }

    /** INFO-Chunks sind oft UTF-8, ältere Dateien ISO-8859-1. */
    private static String decodeText(byte[] data) {
        String utf8 = new String(data, StandardCharsets.UTF_8);
        String value = utf8.contains("�") ? new String(data, StandardCharsets.ISO_8859_1) : utf8;
        return value.replace("\0", "").trim();
    }

    private record FmtInfo(int formatTag, int channels, int sampleRate, int blockAlign, int bitDepth) {}

    private static FmtInfo readFmt(RandomAccessFile raf, long chunkSize) throws IOException {
        int formatTag = readUInt16(raf);
        int ch = readUInt16(raf);
        int sr = (int) readUInt32(raf);
        raf.skipBytes(4); // byteRate
        int align = readUInt16(raf);
        int bits = readUInt16(raf);
        if (formatTag == FORMAT_EXTENSIBLE && chunkSize >= 40) {
            readUInt16(raf); // cbSize
            raf.skipBytes(2); // validBitsPerSample (zum Lesen zählt die Container-Breite)
            raf.skipBytes(4); // channelMask
            formatTag = readUInt16(raf); // erste 2 Bytes der SubFormat-GUID: 1 = PCM, 3 = Float
        }
        return new FmtInfo(formatTag, ch, sr, align, bits);
    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    @Override
    public void open(File file) throws Exception {
        this.file = file;
        raf = new RandomAccessFile(file, "r");
        format = null;

        if (!readString(raf, 4).equals("RIFF")) {
            throw new IOException("Not a RIFF file");
        }
        raf.skipBytes(4); // file size
        if (!readString(raf, 4).equals("WAVE")) {
            throw new IOException("Not a WAVE file");
        }

        FmtInfo fmt = null;
        while (raf.getFilePointer() + 8 <= raf.length()) {
            String chunkId = readString(raf, 4);
            long chunkSize = readUInt32(raf);
            long chunkEnd = raf.getFilePointer() + chunkSize;

            if (chunkId.equals("fmt ")) {
                fmt = readFmt(raf, chunkSize);
            } else if (chunkId.equals("data")) {
                dataStart = raf.getFilePointer();
                dataEnd = Math.min(dataStart + chunkSize, raf.length());
                break;
            }
            raf.seek(chunkEnd);
            if ((chunkSize & 1) != 0) raf.skipBytes(1);
        }

        if (fmt == null) {throw new IOException("Missing fmt chunk");}
        if (dataEnd <= dataStart) {throw new IOException("Missing data chunk");}

        floatingPoint = fmt.formatTag == FORMAT_FLOAT;
        if (fmt.formatTag != FORMAT_PCM && !floatingPoint) {
            throw new IOException("Unsupported WAV format: 0x" + Integer.toHexString(fmt.formatTag));
        }
        bitDepth = fmt.bitDepth;
        boolean supportedDepth = floatingPoint ? (bitDepth == 32 || bitDepth == 64)
                                               : (bitDepth == 8 || bitDepth == 16 || bitDepth == 24 || bitDepth == 32);
        if (!supportedDepth || fmt.channels <= 0 || fmt.sampleRate <= 0) {
            throw new IOException("Unsupported WAV: " + bitDepth + " bit, " + fmt.channels + " ch");
        }
        channels = fmt.channels;
        sampleRate = fmt.sampleRate;
        int bytesPerSample = bitDepth / 8;
        blockAlign = Math.max(fmt.blockAlign, bytesPerSample * channels);

        // Mehrkanal (z. B. 5.1) wird auf Stereo heruntergemischt
        outChannels = Math.min(channels, 2);
        format = new AudioFormat(sampleRate, 16, outChannels, true, false);
        raf.seek(dataStart);
    }

    @Override
    public int decode(byte[] buffer) throws Exception {
        if (raf == null) return -1;
        long remaining = dataEnd - raf.getFilePointer();
        if (remaining < blockAlign) return -1;

        int outFrameSize = outChannels * 2;
        int frames = (int) Math.min(buffer.length / outFrameSize, remaining / blockAlign);
        if (frames <= 0) return -1;

        // 16 Bit Mono/Stereo: direkt lesen
        if (bitDepth == 16 && !floatingPoint && channels <= 2 && blockAlign == outFrameSize) {
            raf.readFully(buffer, 0, frames * blockAlign);
            return frames * outFrameSize;
        }

        int rawLength = frames * blockAlign;
        if (raw.length < rawLength) {raw = new byte[rawLength];}
        raf.readFully(raw, 0, rawLength);

        int bytesPerSample = bitDepth / 8;
        int out = 0;
        for (int f = 0; f < frames; f++) {
            int frameStart = f * blockAlign;
            if (channels <= 2) {
                for (int c = 0; c < channels; c++) {
                    short s = toPcm16(raw, frameStart + c * bytesPerSample);
                    buffer[out++] = (byte) (s & 0xFF);
                    buffer[out++] = (byte) ((s >> 8) & 0xFF);
                }
            } else {
                // WAV-Kanalreihenfolge: FL, FR, FC, LFE, BL, BR, ...
                double fl = toPcm16(raw, frameStart);
                double fr = toPcm16(raw, frameStart + bytesPerSample);
                double fc = toPcm16(raw, frameStart + 2 * bytesPerSample);
                double bl = channels > 4 ? toPcm16(raw, frameStart + 4 * bytesPerSample) : 0;
                double br = channels > 5 ? toPcm16(raw, frameStart + 5 * bytesPerSample) : 0;
                double norm = 1.0 + 0.7071 + (channels > 4 ? 0.7071 : 0);
                short l = clip16((fl + 0.7071 * fc + 0.7071 * bl) / norm);
                short r = clip16((fr + 0.7071 * fc + 0.7071 * br) / norm);
                buffer[out++] = (byte) (l & 0xFF);
                buffer[out++] = (byte) ((l >> 8) & 0xFF);
                buffer[out++] = (byte) (r & 0xFF);
                buffer[out++] = (byte) ((r >> 8) & 0xFF);
            }
        }
        return out;
    }

    private static short clip16(double v) {
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(v)));
    }

    private short toPcm16(byte[] b, int i) {
        if (floatingPoint) {
            double v;
            if (bitDepth == 32) {
                int bits = (b[i] & 0xFF) | (b[i + 1] & 0xFF) << 8 | (b[i + 2] & 0xFF) << 16 | (b[i + 3] & 0xFF) << 24;
                v = Float.intBitsToFloat(bits);
            } else {
                long bits = 0;
                for (int k = 7; k >= 0; k--) {bits = (bits << 8) | (b[i + k] & 0xFF);}
                v = Double.longBitsToDouble(bits);
            }
            if (Double.isNaN(v)) {v = 0;}
            v = Math.max(-1.0, Math.min(1.0, v));
            return (short) Math.round(v * 32767.0);
        }
        return switch (bitDepth) {
            case 8 -> (short) (((b[i] & 0xFF) - 128) << 8);              // 8 Bit ist unsigned
            case 16 -> (short) ((b[i] & 0xFF) | (b[i + 1] << 8));
            case 24 -> (short) ((b[i + 1] & 0xFF) | (b[i + 2] << 8));     // obere 16 Bit
            case 32 -> (short) ((b[i + 2] & 0xFF) | (b[i + 3] << 8));     // obere 16 Bit
            default -> 0;
        };
    }

    @Override
    public AudioFormat getAudioFormat() {
        return format;
    }

    @Override
    public void seekToMillis(long positionMillis) throws Exception {
        if (raf == null) {return;}
        positionMillis = Math.max(0, positionMillis);
        if (blockAlign <= 0 || sampleRate <= 0) {return;}
        long targetFrame = positionMillis * sampleRate / 1000L;
        long targetPosition = dataStart + targetFrame * blockAlign;
        long maxPosition = dataStart + (dataEnd - dataStart) / blockAlign * blockAlign;
        raf.seek(Math.max(dataStart, Math.min(targetPosition, maxPosition)));
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
    private static String readString(RandomAccessFile raf, int length) throws IOException {
        byte[] buffer = new byte[length];
        raf.readFully(buffer);
        return new String(buffer, StandardCharsets.US_ASCII);
    }

    private static int readUInt16(RandomAccessFile raf) throws IOException {
        int b0 = raf.readUnsignedByte();
        int b1 = raf.readUnsignedByte();
        return b0 | (b1 << 8);
    }

    private static long readUInt32(RandomAccessFile raf) throws IOException {
        long b0 = raf.readUnsignedByte();
        long b1 = raf.readUnsignedByte();
        long b2 = raf.readUnsignedByte();
        long b3 = raf.readUnsignedByte();
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }
}
