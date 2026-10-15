package de.emgeem.daplayer.model;

import java.io.File;
import java.util.Locale;

public class Track {

    private final File file;
    private final String location;
    private final String format;

    private int bitDepth;
    private int bitRate;
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

    /** Lokale Datei. */
    public Track(File file) {
        this.file = file;
        this.location = file.getAbsolutePath();
        this.format = getExtension(file.getName());
    }

    /** Netzwerk-Stream bzw. URL (z. B. Internetradio). */
    public Track(String url) {
        this.file = null;
        this.location = url;
        String ext = getExtension(stripQuery(url));
        this.format = ext.isEmpty() || ext.length() > 5 ? "STREAM" : ext;
        this.title = url;
    }

    public static boolean isUrl(String location) {
        return location != null && location.matches("(?i)^[a-z][a-z0-9+.-]*://.+");
    }

// GETTER ++++++++++==========++++++++++
    /** Lokale Datei oder {@code null} bei Streams. */
    public File getFile() {return file;}
    /** Absoluter Dateipfad oder URL. */
    public String getLocation() {return location;}
    public boolean isStream() {return file == null;}
    public String getFormat() {return format;}

    public int getBitDepth() {return bitDepth;}
    public int getBitRate() {return bitRate;}
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

    public String getFileName() {return file != null ? file.getName() : location;}

    /** Titel oder – falls leer – Dateiname. */
    public String getDisplayTitle() {
        return title != null && !title.isBlank() ? title : getFileName();
    }

    /** z. B. "FLAC  44.1 kHz / 16 bit / 2 ch / 950 kbps" */
    public String getFormatDescription() {
        StringBuilder sb = new StringBuilder(format);
        StringBuilder details = new StringBuilder();
        if (sampleRate > 0) {
            double khz = sampleRate / 1000.0;
            details.append(khz == Math.floor(khz) ? String.format(Locale.ROOT, "%.0f kHz", khz) : String.format(Locale.ROOT, "%.1f kHz", khz));
        }
        if (bitDepth > 0) {append(details, bitDepth + " bit");}
        if (channels > 0) {append(details, channels + " ch");}
        if (bitRate > 0) {append(details, bitRate + " kbps");}
        if (details.length() > 0) {sb.append("  ").append(details);}
        return sb.toString();
    }

    private static void append(StringBuilder sb, String value) {
        if (sb.length() > 0) {sb.append(" / ");}
        sb.append(value);
    }

    private static String stripQuery(String url) {
        int q = url.indexOf('?');
        if (q >= 0) {url = url.substring(0, q);}
        int slash = url.lastIndexOf('/');
        return slash >= 0 ? url.substring(slash + 1) : url;
    }

    private static String getExtension(String name) {
        int dot = name.lastIndexOf(".");
        return dot > 0 ? name.substring(dot + 1).toUpperCase(Locale.ROOT) :  "";
    }

// SETTER ++++++++++==========++++++++++
    public void setBitDepth(int bitDepth) {this.bitDepth = bitDepth;}
    public void setBitRate(int bitRate) {this.bitRate = bitRate;}
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
