package de.emgeem.daplayer.model;

import java.io.File;

public class Track {

    private final File file;
    private final String format;

    private int bitDepth;
    private int channels;
    private int discNumber;
    private int sampleRate;
    private int trackNumber;
    private int year;

    private long duration;

    private String album;
    private String albumArtist;
    private String artist;
    private String genre;
    private String title;

    
    public Track(File file) {
        this.file = file;
        this.format = getExtension(file);
    }

// GETTER ++++++++++==========++++++++++
    public File getFile() {return file;}
    public String getFormat() {return format;}
    
    public int getBitDepth() {return bitDepth;}
    public int getChannels() {return channels;}
    public int getDiscNumber() {return discNumber;}
    public int getSampleRate() {return sampleRate;}
    public int getTrackNumber() {return trackNumber;}
    public int getYear() {return year;}

    public long getDuration() {return duration;}

    public String getAlbum() {return album;}
    public String getAlbumArtist () {return albumArtist;}
    public String getArtist() {return artist;}
    public String getGenre() {return genre;}
    public String getTitle() {return title;}
   
    public String getFileName() {return file.getName();}
    private String getExtension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf(".");
        return dot > 0 ? name.substring(dot + 1).toUpperCase() :  "";
    }

// SETTER ++++++++++==========++++++++++
    public void setBitDepth(int bitDepth) {this.bitDepth = bitDepth;}
    public void setChannels(int channels) {this.channels = channels;}
    public void setDiscNumber(int discNumber) {this.discNumber = discNumber;}
    public void setSampleRate(int sampleRate) {this.sampleRate = sampleRate;}
    public void setTrackNumber(int trackNumber) {this.trackNumber = trackNumber;}
    public void setYear(int year) {this.year = year;}
    
    public void setDuration(long duration) {this.duration = duration;}
    
    public void setAlbum(String album) {this.album = album;}
    public void setAlbumArtist(String albumArtist) {this.albumArtist = albumArtist;}
    public void setArtist(String artist) {this.artist = artist;}
    public void setGenre(String genre) {this.genre = genre;}
    public void setTitle(String title) {this.title = title;}
}
