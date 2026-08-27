package de.emgeem.daplayer.model;

import java.io.File;

public class Track {

    private final File file;

    public Track(File file) {this.file = file;}

    public File getFile() {return file;}

    public String getFileName() {return file.getName();}

}
