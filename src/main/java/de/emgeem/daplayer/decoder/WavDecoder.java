package de.emgeem.daplayer.decoder;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import de.emgeem.daplayer.model.Track;


public class WavDecoder implements AudioDecoder {

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
            int sampleRate = 0;
            int channels = 0;
            int bitDepth = 0;
            long dataSize = 0;
            while (raf.getFilePointer() + 8 <= raf.length()) {
                String chunk = readString(raf, 4);
                long chunkSize = readUInt32(raf);
                long chunkEnd = raf.getFilePointer() + chunkSize;
                if (chunk.equals("fmt ")) {
                    int audioFormat = readUInt16(raf);
                    channels = readUInt16(raf);
                    sampleRate = (int) readUInt32(raf);
                    raf.skipBytes(4);
                    raf.skipBytes(2);
                    bitDepth = readUInt16(raf);
                    if (audioFormat != 1) {return track;}
                } else if (chunk.equals("data")) {dataSize = chunkSize;                    
                } else if (chunk.equals("LIST")) {readInfoChunk(raf, chunkEnd, track);
                } else if (chunk.equals("bext")) {readBextChunk(raf, chunkEnd);}
                raf.seek(chunkEnd);
                if ((chunkSize & 1) != 0) {raf.skipBytes(1);}
            }
            if (sampleRate > 0 && channels > 0 && bitDepth > 0) {
                track.setSampleRate(sampleRate);
                track.setChannels(channels);
                track.setBitDepth(bitDepth);
                long bytesPerSecond = (long) sampleRate * channels * bitDepth / 8;
                if (bytesPerSecond > 0) {track.setDuration(dataSize * 1000 / bytesPerSecond);}
            }
        } catch (IOException e) {System.err.println("Could not read WAV file: " + file.getName());}
        if (track.getTitle() == null || track.getTitle().isBlank()) {track.setTitle(track.getFileName());}
        return track;
    }

    private void readInfoChunk(RandomAccessFile raf, long chunkEnd, Track track) throws IOException {
        if (raf.getFilePointer() + 4 > chunkEnd) return;
        String listType = readString(raf, 4);
System.out.println("LIST type: " + listType);
        if (!listType.equals("INFO")) return;
        while (raf.getFilePointer() + 8 <= chunkEnd) {
            String id = readString(raf, 4);
            long size = readUInt32(raf);
System.out.println("INFO chunk: " + id + " (" + size + " bytes)");
            if(size > chunkEnd - raf.getFilePointer()) break;
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
        return new String(buffer, java.nio.charset.StandardCharsets.US_ASCII);
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

        System.out.println("bext Description: " + description);
        System.out.println("bext Originator: " + originator);
        System.out.println("bext Reference: " + originatorReference);
        System.out.println("bext Date: " + originationDate);
        System.out.println("bext Time: " + originationTime);
    }
}
