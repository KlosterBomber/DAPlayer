package de.emgeem.daplayer.model;

import java.util.ArrayList;
import java.util.List;

public class Playlist {

    private final String name;
    private final List<Track> tracks = new ArrayList<>();
    private int currentIndex = -1;

    public Playlist(String name) {this.name = name;}

    public String getName() {return name;}

    public List<Track> getTracks() {return tracks;}

    public Track getCurrentTrack() {
        if (currentIndex < 0 || currentIndex >= tracks.size()) {return null;}
        return tracks.get(currentIndex);
    }
    public int getCurrentIndex() {return currentIndex;}
    public void setCurrentIndex(int index) {if (index >= 0 && index < tracks.size()) {currentIndex = index;}}

    public void nextTrack() {if (currentIndex < tracks.size()) {currentIndex++;}}
    public void previousTrack() {if (currentIndex > 0) {currentIndex--;}}

    public void addTrack(Track track) {
        tracks.add(track);
        if (currentIndex == -1) {currentIndex = 0;}
    }
    
    public void removeTrack(Track track) {tracks.remove(track);}

    public void clear() {tracks.clear(); currentIndex = -1;}

}
