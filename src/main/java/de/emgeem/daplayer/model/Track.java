package de.emgeem.daplayer.model;

import java.io.File;

public class Track {

    private final File file;
    private String title;
    private String artist;
    private String album;
    private long duration;
    private final String format;
    private int sampleRate;
    private int channels;
    private int bitDepth;

    public Track(File file) {
        this.file = file;
        this.format = getExtension(file);
    }

// GETTER ++++++++++==========++++++++++
    public File getFile() {return file;}
    public String getFileName() {return file.getName();}
    public String getTitle() {return title;}
    public String getArtist() {return artist;}
    public String getAlbum() {return album;}
    public long getDuration() {return duration;}
    public String getFormat() {return format;}
    public int getSampleRate() {return sampleRate;}
    public int getChannels() {return channels;}
    public int getBitDepth() {return bitDepth;}

    private String getExtension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf(".");
        return dot > 0 ? name.substring(dot + 1).toUpperCase() :  "";
    }

// SETTER ++++++++++==========++++++++++
    public void setTitle(String title) {this.title = title;}
    public void setArtist(String artist) {this.artist = artist;}
    public void setAlbum(String album) {this.album = album;}
    public void setDuration(long duration) {this.duration = duration;}
    public void setSampleRate(int sampleRate) {this.sampleRate = sampleRate;}
    public void setChannels(int channels) {this.channels = channels;}
    public void setBitDepth(int bitDepth) {this.bitDepth = bitDepth;}
    

}
