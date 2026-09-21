package de.emgeem.daplayer.audio;

import de.emgeem.daplayer.model.Playlist;
import de.emgeem.daplayer.model.Track;
import javafx.application.Platform;

public class PlayerController {

    private final AudioEngine engine;
    private final Playlist playlist;

    public PlayerController(AudioEngine engine, Playlist playlist) {
        this.engine = engine;
        this.playlist = playlist;
        engine.setTrackFinishedListener(track ->
            Platform.runLater(() -> {
                if (playlist.hasNextTrack()) {
                    playlist.nextTrack();
                    play();
                } else {
                    engine.stop();
                }
            })
        );
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
        if (playlist.nextTrack()) {play();}
        else {engine.stop();}
    }

    public void previous() {
        if (playlist.previousTrack()) {play();}
    }

    public boolean isPlaying() {
        return engine.isPlaying();
    }

    public boolean isPaused() {
        return engine.isPaused();
    }

    public long getCurrentPositionMillis() {
        return engine.getCurrentPositionMillis();
    }

    public long getTotalDurationMillis() {
        return engine.getTotalDurationMillis();
    }

    public void seekToMillis(long positionMillis) {
        engine.seekToMillis(positionMillis);
    }
}
