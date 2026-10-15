package de.emgeem.daplayer.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import de.emgeem.daplayer.model.Playlist;
import de.emgeem.daplayer.model.Track;

/**
 * Steuert die Wiedergabe der Playlist. Alle öffentlichen Methoden werden auf dem
 * UI-Thread aufgerufen; Ereignisse der Engine werden über {@code uiExecutor} dorthin übergeben.
 */
public class PlayerController {

    public enum State {STOPPED, PLAYING, PAUSED}

    /** "PREV" innerhalb dieser Zeit springt zum vorherigen Track, sonst an den Anfang. */
    private static final long RESTART_THRESHOLD_MILLIS = 3000;

    private final AudioEngine engine;
    private final Playlist playlist;
    private final Consumer<Runnable> uiExecutor;
    private final List<Integer> shuffleOrder = new ArrayList<>();
    private final Random random;

    private Consumer<State> stateListener;
    private Runnable trackChangedListener;
    private BiConsumer<Track, String> errorListener;

    private State state = State.STOPPED;
    private boolean repeat = false;
    private boolean shuffle = false;
    private int shufflePosition = -1;

    public PlayerController(AudioEngine engine, Playlist playlist) {
        this(engine, playlist, javafx.application.Platform::runLater, new Random());
    }

    public PlayerController(AudioEngine engine, Playlist playlist, Consumer<Runnable> uiExecutor, Random random) {
        this.engine = engine;
        this.playlist = playlist;
        this.uiExecutor = uiExecutor;
        this.random = random;
        engine.setTrackFinishedListener(track -> uiExecutor.accept(() -> onTrackFinished(track)));
        engine.setErrorListener((track, error) -> uiExecutor.accept(() -> onTrackError(track, error)));
    }

    // ============================================================
    // TRANSPORT
    // ============================================================

    public void play() {
        Track track = playlist.getCurrentTrack();
        if (track == null) {
            setState(State.STOPPED);
            return;
        }
        engine.play(track);
        setState(State.PLAYING);
    }

    /** Spielt den Track an index (z. B. Doppelklick in der Playlist). */
    public void playIndex(int index) {
        if (index < 0 || index >= playlist.size()) {return;}
        playlist.setCurrentIndex(index);
        if (shuffle) {syncShufflePosition();}
        notifyTrackChanged();
        play();
    }

    /** PLAY/PAUSE-Taster: Start, Pause oder Fortsetzen. */
    public void togglePlayPause() {
        switch (state) {
            case STOPPED -> play();
            case PLAYING -> pause();
            case PAUSED -> resume();
        }
    }

    public void pause() {
        if (state != State.PLAYING) {return;}
        engine.pause();
        setState(State.PAUSED);
    }

    public void resume() {
        if (state != State.PAUSED) {return;}
        engine.resume();
        setState(State.PLAYING);
    }

    public void stop() {
        engine.stop();
        setState(State.STOPPED);
    }

    public void next() {
        int nextIndex = advanceToNextIndex();
        if (nextIndex < 0) {
            stop();
            rewindAfterEnd();
            return;
        }
        moveTo(nextIndex);
    }

    public void previous() {
        if (state != State.STOPPED && engine.getCurrentPositionMillis() > RESTART_THRESHOLD_MILLIS) {
            play(); // Track von vorne
            return;
        }
        int previousIndex;
        if (shuffle) {
            ensureShuffleOrder();
            if (shufflePosition > 0) {
                shufflePosition--;
                previousIndex = shuffleOrder.get(shufflePosition);
            } else {
                previousIndex = playlist.getCurrentIndex();
            }
        } else {
            previousIndex = playlist.getCurrentIndex() > 0 ? playlist.getCurrentIndex() - 1
                          : (repeat ? playlist.size() - 1 : playlist.getCurrentIndex());
        }
        if (previousIndex < 0) {return;}
        moveTo(previousIndex);
    }

    private void moveTo(int index) {
        playlist.setCurrentIndex(index);
        notifyTrackChanged();
        play();
    }

    private void onTrackFinished(Track finished) {
        if (state == State.STOPPED) {return;}
        int nextIndex = advanceToNextIndex();
        if (nextIndex < 0) {
            engine.stop();
            setState(State.STOPPED);
            rewindAfterEnd();
            return;
        }
        playlist.setCurrentIndex(nextIndex);
        notifyTrackChanged();
        play();
    }

    /** Nach dem Ende der Playlist: PLAY startet wieder von vorne bzw. mit einer neuen Zufallsrunde. */
    private void rewindAfterEnd() {
        if (playlist.isEmpty()) {return;}
        if (shuffle) {
            createShuffleOrder(-1);
            playlist.setCurrentIndex(shuffleOrder.get(0));
        } else {
            playlist.setCurrentIndex(0);
        }
        notifyTrackChanged();
    }

    private void onTrackError(Track track, Exception error) {
        String message = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
        if (errorListener != null) {errorListener.accept(track, message);}
        setState(State.STOPPED);
    }

    /**
     * Ermittelt den nächsten Index (berücksichtigt Shuffle/Repeat) und zählt die Shuffle-Position weiter.
     * @return -1 am Ende der Playlist
     */
    private int advanceToNextIndex() {
        if (playlist.isEmpty()) {return -1;}
        if (shuffle) {
            ensureShuffleOrder();
            if (shufflePosition + 1 < shuffleOrder.size()) {
                shufflePosition++;
                return shuffleOrder.get(shufflePosition);
            }
            if (!repeat) {return -1;}
            if (playlist.size() == 1) {return playlist.getCurrentIndex();}
            createShuffleOrder(-1);
            // nicht denselben Track direkt zweimal hintereinander
            if (shuffleOrder.get(0) == playlist.getCurrentIndex()) {Collections.swap(shuffleOrder, 0, shuffleOrder.size() - 1);}
            shufflePosition = 0;
            return shuffleOrder.get(0);
        }
        if (playlist.hasNextTrack()) {return playlist.getCurrentIndex() + 1;}
        return repeat ? 0 : -1;
    }

    // ============================================================
    // PLAYLIST-ÄNDERUNGEN
    // ============================================================

    /** Entfernt einen Track; läuft er gerade, wird die Wiedergabe gestoppt. */
    public void removeTrack(int index) {
        if (index < 0 || index >= playlist.size()) {return;}
        boolean removingCurrent = index == playlist.getCurrentIndex();
        if (removingCurrent && state != State.STOPPED) {stop();}
        playlist.removeTrack(index);
        shuffleOrder.clear();
        notifyTrackChanged();
    }

    public void clearPlaylist() {
        stop();
        playlist.clear();
        shuffleOrder.clear();
        shufflePosition = -1;
        notifyTrackChanged();
    }

    /** Muss nach dem Hinzufügen von Tracks aufgerufen werden. */
    public void playlistChanged() {
        if (shuffle && shuffleOrder.size() != playlist.size()) {
            // neue Tracks hinten zufällig einmischen, bereits gespielte Reihenfolge bleibt erhalten
            List<Integer> missing = new ArrayList<>();
            for (int i = 0; i < playlist.size(); i++) {if (!shuffleOrder.contains(i)) {missing.add(i);}}
            if (shuffleOrder.isEmpty() || shuffleOrder.stream().anyMatch(i -> i >= playlist.size())) {
                createShuffleOrder(playlist.getCurrentIndex());
            } else {
                Collections.shuffle(missing, random);
                shuffleOrder.addAll(missing);
            }
        }
    }

    // ============================================================
    // STATUS
    // ============================================================

    public State getState() {return state;}
    public boolean isPlaying() {return state != State.STOPPED;}
    public boolean isPaused() {return state == State.PAUSED;}

    /** Track, der gerade in der Engine läuft (oder null). */
    public Track getPlayingTrack() {return state == State.STOPPED ? null : engine.getCurrentTrack();}

    public long getCurrentPositionMillis() {return state == State.STOPPED ? 0 : engine.getCurrentPositionMillis();}

    public long getTotalDurationMillis() {
        if (state != State.STOPPED) {return engine.getTotalDurationMillis();}
        Track track = playlist.getCurrentTrack();
        return track != null ? track.getDuration() : 0;
    }

    public boolean isSeekable() {return state != State.STOPPED && engine.isSeekable();}

    public void seekToMillis(long positionMillis) {engine.seekToMillis(positionMillis);}

    /** Relativer Sprung, z. B. per Pfeiltaste. */
    public void seekBy(long deltaMillis) {
        if (!isSeekable()) {return;}
        engine.seekToMillis(Math.max(0, engine.getCurrentPositionMillis() + deltaMillis));
    }

    /** Startet den aktuellen Track an gleicher Position neu (z. B. nach Gerätewechsel). */
    public void restartOnNewDevice() {
        if (state == State.STOPPED) {return;}
        Track track = engine.getCurrentTrack();
        if (track == null) {return;}
        long position = engine.isSeekable() ? engine.getCurrentPositionMillis() : 0;
        boolean wasPaused = state == State.PAUSED;
        engine.play(track, position);
        if (wasPaused) {engine.pause();}
    }

    public void setStateListener(Consumer<State> listener) {this.stateListener = listener;}
    public void setTrackChangedListener(Runnable listener) {this.trackChangedListener = listener;}
    public void setErrorListener(BiConsumer<Track, String> listener) {this.errorListener = listener;}

    /** Kompatibilität zu 0.2.0. */
    public void setPlaybackStoppedListener(Runnable listener) {
        Consumer<State> previous = stateListener;
        setStateListener(state -> {
            if (previous != null) {previous.accept(state);}
            if (state == State.STOPPED) {listener.run();}
        });
    }

    private void setState(State newState) {
        state = newState;
        if (stateListener != null) {stateListener.accept(newState);}
    }

    private void notifyTrackChanged() {
        if (trackChangedListener != null) {trackChangedListener.run();}
    }

    // ============================================================
    // MODI
    // ============================================================

    public void setRepeat(boolean repeat) {this.repeat = repeat;}
    public boolean isRepeat() {return repeat;}

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
        if (shuffle) {createShuffleOrder(playlist.getCurrentIndex());}
        else {
            shuffleOrder.clear();
            shufflePosition = -1;
        }
    }
    public boolean isShuffle() {return shuffle;}

    private void ensureShuffleOrder() {
        if (shuffleOrder.size() != playlist.size()) {playlistChanged();}
        if (shuffleOrder.size() != playlist.size()) {createShuffleOrder(playlist.getCurrentIndex());}
        syncShufflePosition();
    }

    private void syncShufflePosition() {
        int position = shuffleOrder.indexOf(playlist.getCurrentIndex());
        if (position >= 0) {shufflePosition = position;}
    }

    /** Neue Zufallsreihenfolge; first (falls >= 0) steht vorne. */
    private void createShuffleOrder(int first) {
        shuffleOrder.clear();
        for (int i = 0; i < playlist.size(); i++) {shuffleOrder.add(i);}
        Collections.shuffle(shuffleOrder, random);
        if (first >= 0 && first < playlist.size()) {
            shuffleOrder.remove(Integer.valueOf(first));
            shuffleOrder.add(0, first);
        }
        shufflePosition = shuffleOrder.isEmpty() ? -1 : 0;
    }
}
