package de.emgeem.daplayer.decoder;

import java.io.File;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;

import de.emgeem.daplayer.model.Track;

public class Mp3Decoder implements AudioDecoder {

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

    private int parseNumber(String value) {
        try {
            int slash = value.indexOf('/');
            if (slash >= 0) {value = value.substring(0, slash);}
            return Integer.parseInt(value);            
        } catch (NumberFormatException e) {return 0;}
    }
}
