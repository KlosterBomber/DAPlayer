package de.emgeem.daplayer.audio;

import de.emgeem.daplayer.model.Playlist;
import de.emgeem.daplayer.model.Track;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerController {

    private final AudioEngine engine;
    private final Playlist playlist;
    private final List<Integer> shuffleOrder = new ArrayList<>();

    private Runnable playbackStoppedListener;
    private Runnable trackChangedListener;

    private boolean repeat = false;
    private boolean shuffle = false;
    private int shufflePosition = -1;

    public PlayerController(AudioEngine engine, Playlist playlist) {
        this.engine = engine;
        this.playlist = playlist;
        engine.setTrackFinishedListener(track ->
            Platform.runLater(() -> {
                if (shuffle) {                
                    if (shufflePosition + 1 < shuffleOrder.size()) {
                        shufflePosition++;
                        playlist.setCurrentIndex(shuffleOrder.get(shufflePosition));
                        notifyTrackChanged();
                        play();                    
                    } else if (repeat) {
                        createShuffleOrder();
                        shufflePosition = 1;
                        playlist.setCurrentIndex(shuffleOrder.get(shufflePosition));
                        notifyTrackChanged();
                        play();                    
                    } else {
                        engine.stop();                    
                        if (playbackStoppedListener != null) {playbackStoppedListener.run();}
                    }                
                } else if (playlist.hasNextTrack()) {                
                    playlist.nextTrack();
                    notifyTrackChanged();
                    play();                
                } else if (repeat) {                
                    playlist.setCurrentIndex(0);
                    notifyTrackChanged();
                    play();                
                } else {                
                    engine.stop();                
                    if (playbackStoppedListener != null) {playbackStoppedListener.run();}
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
        if (playlist.nextTrack()) {
            notifyTrackChanged();
            play();
        } else {engine.stop();}
    }

    public void previous() {
        if (playlist.previousTrack()) {
            notifyTrackChanged();
            play();
        }
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

    public void setPlaybackStoppedListener(Runnable listener) {
        this.playbackStoppedListener = listener;
    }

    public void setTrackChangedListener(Runnable listener) {
        this.trackChangedListener = listener;
    }

    private void notifyTrackChanged() {
        if (trackChangedListener != null) {
            trackChangedListener.run();
        }
    }

    public void setRepeat(boolean repeat) {this.repeat = repeat;}
    public boolean isRepeat() {return repeat;}

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
        if (shuffle) {createShuffleOrder();}
        else {
            shuffleOrder.clear();
            shufflePosition = -1;
        }
    }
    public boolean isShuffle() {return shuffle;}

    private void createShuffleOrder() {
        shuffleOrder.clear();
        for (int i = 0; i < playlist.getTracks().size(); i++) {shuffleOrder.add(i);}
        Collections.shuffle(shuffleOrder);
        int currentIndex = playlist.getCurrentIndex();
        shuffleOrder.remove(Integer.valueOf(currentIndex));
        shuffleOrder.add(0, currentIndex);
        shufflePosition = 0;
    }
}
