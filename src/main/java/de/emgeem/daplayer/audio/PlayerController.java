package de.emgeem.daplayer.audio;

import de.emgeem.daplayer.model.Playlist;
import de.emgeem.daplayer.model.Track;

public class PlayerController {

    private final AudioEngine engine;
    private final Playlist playlist;

    public PlayerController(AudioEngine engine, Playlist playlist) {
        this.engine = engine;
        this.playlist = playlist;
    }

    public void play() {
        Track track = playlist.getCurrentTrack();
        if (track == null) return;

        engine.play(track);
    }

    public void pause() {
        engine.pause();
    }

    public void resume() {
        engine.resume();
    }

    public void stop() {
        engine.stop();
    }

    public void next() {
        playlist.nextTrack();
        play();
    }

    public void previous() {
        playlist.previousTrack();
        play();
    }

    public boolean isPlaying() {
        return engine.isPlaying();
    }

    public boolean isPaused() {
        return engine.isPaused();
    }
}
