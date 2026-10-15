package de.emgeem.daplayer.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Playlist {

    private final String name;
    private final List<Track> tracks = new ArrayList<>();
    private int currentIndex = -1;

    public Playlist(String name) {this.name = name;}

    public String getName() {return name;}

    /** Nur-Lese-Sicht; Änderungen über addTrack/removeTrack/clear. */
    public List<Track> getTracks() {return Collections.unmodifiableList(tracks);}
    public int size() {return tracks.size();}
    public boolean isEmpty() {return tracks.isEmpty();}

    public Track getCurrentTrack() {
        if (currentIndex < 0 || currentIndex >= tracks.size()) {return null;}
        return tracks.get(currentIndex);
    }
    public int getCurrentIndex() {return currentIndex;}
    public void setCurrentIndex(int index) {if (index >= 0 && index < tracks.size()) {currentIndex = index;}}

    public boolean nextTrack() {
        if (currentIndex + 1 < tracks.size()) {
            currentIndex++;
            return true;
        }
        return false;
    }
    public boolean previousTrack() {
        if (currentIndex > 0) {
            currentIndex--;
            return true;
        }
        return false;
    }
    public boolean hasNextTrack() {return currentIndex >= 0 && currentIndex + 1 < tracks.size();}

    public void addTrack(Track track) {
        tracks.add(track);
        if (currentIndex == -1) {currentIndex = 0;}
    }

    public void addTracks(Collection<Track> newTracks) {
        for (Track track : newTracks) {addTrack(track);}
    }

    /** Entfernt den Track an index und hält currentIndex konsistent. */
    public Track removeTrack(int index) {
        if (index < 0 || index >= tracks.size()) {return null;}
        Track removed = tracks.remove(index);
        if (tracks.isEmpty()) {
            currentIndex = -1;
        } else if (index < currentIndex) {
            currentIndex--;
        } else if (currentIndex >= tracks.size()) {
            currentIndex = tracks.size() - 1;
        }
        return removed;
    }

    public void removeTrack(Track track) {
        int index = tracks.indexOf(track);
        if (index >= 0) {removeTrack(index);}
    }

    public void clear() {tracks.clear(); currentIndex = -1;}

}
