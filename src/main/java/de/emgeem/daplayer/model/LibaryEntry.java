package de.emgeem.daplayer.model;

import java.io.File;

public class LibaryEntry {

    public static final String UNKNOWN_ARTIST = "UNKNOWN ARTIST";
    public static final String UNKNOWN_ALBUM = "UNKNOWN ALBUM";

    private String path;
    private long fileSize;
    private long lastModified;

    private String title;
    private String artist;
    private String album;
    private String albumArtist;
    private String genre;
    private String format;
    private int year;
    private int trackNumber;
    private int discNumber;
    private long duration;
    private int sampleRate;
    private int bitDepth;
    private int channels;
    private int bitRate;

    public LibaryEntry() {}

    public static LibaryEntry fromTrack(Track track) {
        File file = track.getFile();
        LibaryEntry entry = new LibaryEntry();
        entry.path = file.getAbsolutePath();
        entry.fileSize = file.length();
        entry.lastModified = file.lastModified();
        entry.title = track.getTitle();
        entry.artist = track.getArtist();
        entry.album = track.getAlbum();
        entry.albumArtist = track.getAlbumArtist();
        entry.genre = track.getGenre();
        entry.format = track.getFormat();
        entry.year = track.getYear();
        entry.trackNumber = track.getTrackNumber();
        entry.discNumber = track.getDiscNumber();
        entry.duration = track.getDuration();
        entry.sampleRate = track.getSampleRate();
        entry.bitDepth = track.getBitDepth();
        entry.channels = track.getChannels();
        entry.bitRate = track.getBitRate();
        return entry;
    }

    public Track toTrack() {
        Track track = new Track(new File(path));
        track.setTitle(title);
        track.setArtist(artist);
        track.setAlbum(album);
        track.setAlbumArtist(albumArtist);
        track.setGenre(genre);
        track.setYear(year);
        track.setTrackNumber(trackNumber);
        track.setDiscNumber(discNumber);
        track.setDuration(duration);
        track.setSampleRate(sampleRate);
        track.setBitDepth(bitDepth);
        track.setChannels(channels);
        track.setBitRate(bitRate);
        return track;
    }

    public boolean isUpToDate(File file) {return file.length() == fileSize && file.lastModified() == lastModified;}

    public String getDisplayArtist() {
        if (!isBlank(albumArtist)) {return albumArtist.trim();}
        if (!isBlank(artist)) {return artist.trim();}
        return UNKNOWN_ARTIST;
    }
    public String getDisplayAlbum() {
        if (!isBlank(album)) {return album.trim();}
        File parent = new File(path).getParentFile();
        return parent != null ? parent.getName() : UNKNOWN_ALBUM;
    }
    public String getDisplayTitle() {return !isBlank(title) ? title.trim() : new File(path).getName();}
    private static boolean isBlank(String value) {return value == null || value.isBlank();}

    public String getPath() {return path;}
    public long getFileSize() {return fileSize;}
    public long getLastModified() {return lastModified;}
    public String getTitle() {return title;}
    public String getArtist() {return artist;}
    public String getAlbum() {return album;}
    public String getAlbumArtist() {return albumArtist;}
    public String getGenre() {return genre;}
    public String getFormat() {return format;}
    public int getYear() {return year;}
    public int getTrackNumber() {return trackNumber;}
    public int getDiscNumber() {return discNumber;}
    public long getDuration() {return duration;}
}
