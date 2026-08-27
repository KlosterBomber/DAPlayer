package de.emgeem.daplayer.model;

import java.util.ArrayList;
import java.util.List;

public class Playlist {

    private final String name;
    private final List<Track> tracks = new ArrayList<>();

    public Playlist(String name) {this.name = name;}

    public String getName() {return name;}

    public List<Track> getTracks() {return tracks;}

    public void addTrack(Track track) {tracks.add(track);}
    
    public void removeTrack(Track track) {tracks.remove(track);}

    public void clear() {tracks.clear();}

}
