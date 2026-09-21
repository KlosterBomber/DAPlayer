package de.emgeem.daplayer.decoder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;

import de.emgeem.daplayer.model.Track;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

public class Mp3Decoder implements AudioDecoder {

private File file;
private Bitstream bitstream;
private Decoder decoder;
private AudioFormat format;

    @Override
    public boolean supports(File file) {
        return file.getName().toLowerCase().endsWith(".mp3");
    }

    @Override
    public Track readTrack(File file) {
        Track track = new Track(file);
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            if (tag != null) {
                track.setTitle(tag.getFirst("TIT2"));
                if (track.getTitle() == null || track.getTitle().isBlank()) {track.setTitle(track.getFileName());}
                track.setArtist(tag.getFirst("TPE1"));
                track.setAlbum(tag.getFirst("TALB"));
                track.setAlbumArtist(tag.getFirst("TPE2"));
                track.setGenre(tag.getFirst("TCON"));
                String year = tag.getFirst("TYER");
                if (year != null && !year.isBlank()) {track.setYear(Integer.parseInt(year));}
                String trackNumber = tag.getFirst("TRCK");
                if (trackNumber != null && !trackNumber.isBlank()) {track.setTrackNumber(parseNumber(trackNumber));}
                String discNumber = tag.getFirst("TPOS");
                if (discNumber != null && !discNumber.isBlank()) {track.setDiscNumber(parseNumber(discNumber));}
            }
            var header = audioFile.getAudioHeader();
            track.setDuration(header.getTrackLength() * 1000L);
        } catch (Exception e) {
            System.err.println("Could not read MP3: " + file.getName());
            e.printStackTrace();
        }
        return track;
    }

// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

@Override 
public void open(File file) throws Exception {
    this.file = file;
    InputStream in = new BufferedInputStream(new FileInputStream(file));
    bitstream = new Bitstream(in);
    decoder = new Decoder();
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
    Header header = bitstream.readFrame();
    if (header == null) {return -1;}
    SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
    short[] pcm = output.getBuffer();
    int pcmBytes = pcm.length * 2;
    if (buffer.length < pcmBytes) throw new IllegalArgumentException("buffer too small: need " + pcmBytes);
    for (int i = 0; i < pcm.length; i++) {
        buffer[i * 2] = (byte) (pcm[i] & 0xff);
        buffer[i * 2 + 1] = (byte) ((pcm[i] >> 8) & 0xff);
    }
    bitstream.closeFrame();
    return pcmBytes;
}
@Override 
public AudioFormat getAudioFormat() {return format;}
@Override 
public void reset() throws Exception {close(); open(file);}
@Override 
public void close() throws Exception {if (bitstream != null) {bitstream.close();}}

// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    private int parseNumber(String value) {
        try {
            int slash = value.indexOf('/');
            if (slash >= 0) {value = value.substring(0, slash);}
            return Integer.parseInt(value);            
        } catch (NumberFormatException e) {return 0;}
    }
}
