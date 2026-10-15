package de.emgeem.daplayer.decoder;

import java.io.File;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import de.emgeem.daplayer.model.Track;

/**
 * Formatunabhängiges Auslesen von Tags über JAudioTagger.
 * Nutzt FieldKey statt ID3-Frame-IDs, damit MP3 (ID3v1/v2.3/v2.4),
 * WMA (ASF), FLAC/OGG (Vorbis Comments), M4A und AIFF gleich funktionieren.
 */
final class TagReader {

    private TagReader() {}

    /** @return true, wenn die Datei von JAudioTagger gelesen werden konnte. */
    static boolean read(File file, Track track) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            AudioHeader header = audioFile.getAudioHeader();
            if (header != null) {
                double precise = header.getPreciseTrackLength();
                long millis = precise > 0 ? Math.round(precise * 1000.0) : header.getTrackLength() * 1000L;
                if (millis > 0) {track.setDuration(millis);}
                track.setSampleRate(header.getSampleRateAsNumber());
                track.setBitDepth(Math.max(0, header.getBitsPerSample()));
                track.setChannels(parseLeadingInt(header.getChannels()));
                track.setBitRate((int) Math.max(0, header.getBitRateAsNumber()));
            }
            Tag tag = audioFile.getTag();
            if (tag != null) {
                track.setTitle(first(tag, FieldKey.TITLE));
                track.setArtist(first(tag, FieldKey.ARTIST));
                track.setAlbum(first(tag, FieldKey.ALBUM));
                track.setAlbumArtist(first(tag, FieldKey.ALBUM_ARTIST));
                track.setGenre(first(tag, FieldKey.GENRE));
                track.setYear(parseLeadingInt(first(tag, FieldKey.YEAR)));
                track.setTrackNumber(parseLeadingInt(first(tag, FieldKey.TRACK)));
                track.setDiscNumber(parseLeadingInt(first(tag, FieldKey.DISC_NO)));
            }
            return true;
        } catch (Exception | LinkageError e) {
            System.err.println("Tags nicht lesbar: " + file.getName() + " (" + e.getMessage() + ")");
            return false;
        } finally {
            if (track.getTitle() == null || track.getTitle().isBlank()) {track.setTitle(track.getFileName());}
        }
    }

    private static String first(Tag tag, FieldKey key) {
        try {
            String value = tag.getFirst(key);
            return value == null || value.isBlank() ? null : value.trim();
        } catch (Exception e) {
            return null;
        }
    }

    /** "2004-05-01" → 2004, "3/12" → 3, "Stereo"/"2" → 0/2, null → 0. */
    static int parseLeadingInt(String value) {
        if (value == null) {return 0;}
        value = value.trim();
        int end = 0;
        while (end < value.length() && Character.isDigit(value.charAt(end)) && end < 9) {end++;}
        if (end == 0) {return 0;}
        return Integer.parseInt(value.substring(0, end));
    }
}
