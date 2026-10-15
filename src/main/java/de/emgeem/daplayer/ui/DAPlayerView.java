package de.emgeem.daplayer.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import de.emgeem.daplayer.audio.AudioDeviceManager;
import de.emgeem.daplayer.audio.AudioEngine;
import de.emgeem.daplayer.audio.Equalizer;
import de.emgeem.daplayer.audio.EqualizerSettings;
import de.emgeem.daplayer.audio.PlayerController;
import de.emgeem.daplayer.decoder.DecoderManager;
import de.emgeem.daplayer.model.Playlist;
import de.emgeem.daplayer.model.Track;
import de.emgeem.daplayer.playlist.PlaylistManager;
import de.emgeem.daplayer.ui.background.PlayerPanel;
import de.emgeem.daplayer.ui.background.PlayerViewBackground;
import de.emgeem.daplayer.ui.control.MenuButton;
import de.emgeem.daplayer.ui.control.OffButton;
import de.emgeem.daplayer.ui.control.PlayModeButton;
import de.emgeem.daplayer.ui.control.RoundControlButton;
import de.emgeem.daplayer.ui.control.VolumeKnob;
import de.emgeem.daplayer.ui.display.PlayerDisplay;
import de.emgeem.daplayer.ui.display.TrackListDisplay;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

public class DAPlayerView extends StackPane {

    private static final double DISPLAY_TOP_MARGIN = 15;
    private static final double DISPLAY_SIDE_MARGIN = 21;
    private static final double DISPLAY_HEIGHT = 400;
    private static final double CONTROL_TOP_MARGIN = 15;
    private static final double CONTROL_BOTTOM_MARGIN = 15;
    private static final double CONTROL_AREA_HEIGHT = 240;
    private static final double CONTROL_SPACING = 50;

    private static final double VOLUME_STEP = 0.05;
    private static final long SEEK_STEP_MILLIS = 5_000;
    private static final long SEEK_STEP_LARGE_MILLIS = 30_000;
    private static final int LOAD_BATCH_SIZE = 25;

    private static final String PREF_VOLUME = "volume";
    private static final String PREF_SHUFFLE = "shuffle";
    private static final String PREF_REPEAT = "repeat";
    private static final String PREF_SESSION_INDEX = "sessionIndex";
    private static final String PREF_LAST_DIRECTORY = "lastDirectory";

    private final Preferences prefs = Preferences.userNodeForPackage(DAPlayerView.class);
    private final Playlist playlist = new Playlist("Current");
    private final DecoderManager decoderManager = new DecoderManager();
    private final PlayerDisplay display;
    private final AudioDeviceManager audioDeviceManager = new AudioDeviceManager();
    private final PlaylistManager playlistManager = new PlaylistManager();
    private final ExecutorService loader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DAPlayer-Loader");
        t.setDaemon(true);
        return t;
    });
    /** Wird bei CLEAR/LOAD erhöht, damit laufende Ladevorgänge verworfen werden. */
    private volatile int loadGeneration;

    private final PlayerPanel playerPanel;
    private final RoundControlButton previousButton;
    private final RoundControlButton stopButton;
    private final RoundControlButton playPauseButton;
    private final RoundControlButton nextButton;
    private final VolumeKnob volumeKnob;
    private final OffButton offButton;
    private final MenuButton playlistButton;
    private final MenuButton trackinfoButton;
    private final MenuButton reserveButton;
    private final MenuButton settingsButton;
    private final MenuButton visualsButton;
    private final MenuButton eqButton;
    private final MenuButton moveButton;
    private final AudioEngine audioEngine;
    private final PlayerController controller;
    private final Equalizer equalizer;
    private final PlayModeButton shuffleButton;
    private final PlayModeButton repeatButton;
    private final AnimationTimer progressTimer;

    private double dragX;
    private double dragY;
    private boolean shutDown;

    public DAPlayerView() {

        equalizer = new Equalizer();
        EqualizerSettings.load(equalizer);
        audioEngine = new AudioEngine(audioDeviceManager, decoderManager, equalizer);
        controller = new PlayerController(audioEngine, playlist);

        PlayerViewBackground background = new PlayerViewBackground(1200, 675);

        background.widthProperty().bind(widthProperty());
        background.heightProperty().bind(heightProperty());

    // PLAYER PANEL
        playerPanel = new PlayerPanel(1200, 675);
        StackPane.setAlignment(playerPanel, Pos.TOP_CENTER);

    // DISPLAY AREA
        StackPane displayArea = new StackPane();
        displayArea.setAlignment(Pos.TOP_CENTER);
        displayArea.setPadding(new Insets(DISPLAY_TOP_MARGIN, DISPLAY_SIDE_MARGIN, 0, DISPLAY_SIDE_MARGIN));

        display = new PlayerDisplay(1158, DISPLAY_HEIGHT, this::openFiles, this::openFolder, audioDeviceManager, equalizer, this::redrawUi, controller::seekToMillis,
                                    this::setActiveDisplay, this::savePlaylist, this::loadPlaylist);
        display.setPlaylist(playlist);
        audioEngine.setVisualizationListener(display.getVisualizationListener());
        displayArea.getChildren().add(display);

        TrackListDisplay trackList = display.getTrackListDisplay();
        trackList.setPlayIndexAction(index -> {
            display.showError(null);
            controller.playIndex(index);
        });
        trackList.setRemoveIndexAction(index -> {
            controller.removeTrack(index);
            display.setPlaylist(playlist);
        });
        trackList.setClearAction(() -> {
            loadGeneration++;
            controller.clearPlaylist();
            trackList.setStatusText(null);
            display.setPlaylist(playlist);
        });
        trackList.setAddLocationAction(this::addLocation);
        trackList.setPlayingSupplier(controller::isPlaying);
        display.getTrackInfoDisplay().setPlayingTrackSupplier(controller::getPlayingTrack);
        display.getTrackInfoDisplay().setSeekableSupplier(controller::isSeekable);
        display.getTrackInfoDisplay().setBrowseChangedAction(trackList::updateCurrentTrack);
        display.getSettingsDisplay().setDeviceChangedAction(controller::restartOnNewDevice);

    // CONTROL AREA
        StackPane controlArea = new StackPane();
        controlArea.setPrefHeight(CONTROL_AREA_HEIGHT);
        controlArea.setMaxHeight(CONTROL_AREA_HEIGHT);
        controlArea.setAlignment(Pos.CENTER);
        controlArea.setPadding(new Insets(CONTROL_TOP_MARGIN, 0, CONTROL_BOTTOM_MARGIN, 0));

        previousButton = new RoundControlButton("PREV", false);
        stopButton = new RoundControlButton("STOPPED", true);
        playPauseButton = new RoundControlButton("PLAY/\nPAUSE", false);
        nextButton = new RoundControlButton("NEXT", false);

        volumeKnob = new VolumeKnob("VOLUME");

        HBox controlRow = new HBox();
        controlRow.setAlignment(Pos.CENTER);
        controlRow.setSpacing(CONTROL_SPACING);
        controlRow.getChildren().addAll(previousButton, nextButton, volumeKnob, stopButton, playPauseButton);

        controlArea.getChildren().add(controlRow);
        previousButton.setTranslateY(+25);
        stopButton.setTranslateY(+25);
        volumeKnob.setTranslateY(-5);
        playPauseButton.setTranslateY(+25);
        nextButton.setTranslateY(+25);

        playPauseButton.setOnMouseClicked(e -> togglePlayPause());
        stopButton.setOnMouseClicked(e -> controller.stop());
        nextButton.setOnMouseClicked(e -> controller.next());
        previousButton.setOnMouseClicked(e -> controller.previous());

        float savedVolume = (float) Math.max(0.0, Math.min(1.0, prefs.getDouble(PREF_VOLUME, 1.0)));
        volumeKnob.setVolume(savedVolume);
        audioEngine.setVolume(savedVolume);
        volumeKnob.setVolumeListener(v -> setVolume(v, false));
        volumeKnob.setMuteListener(m -> audioEngine.setMuted(m));

        controller.setStateListener(this::updateTransportButtons);
        controller.setTrackChangedListener(display::updateCurrentTrack);
        controller.setErrorListener((track, message) -> display.showError(track.getDisplayTitle() + " - " + message));


// MENU BUTTONS

        playlistButton = new MenuButton("PLAYLIST", 150, 40, MenuButton.Shape.CENTER);
        playlistButton.setOnMouseClicked(e -> display.showTrackList());
        StackPane.setAlignment(playlistButton, Pos.TOP_LEFT);
        playlistButton.setTranslateX(25);
        playlistButton.setTranslateY(425);

        trackinfoButton = new MenuButton("TRACKINFO", 150, 40, MenuButton.Shape.CENTER);
        trackinfoButton.setOnMouseClicked(e -> display.showTrackInfo());
        StackPane.setAlignment(trackinfoButton, Pos.TOP_LEFT);
        trackinfoButton.setTranslateX(175);
        trackinfoButton.setTranslateY(425);

        reserveButton = new MenuButton("RESERVE", 150, 40, MenuButton.Shape.RIGHT);
        StackPane.setAlignment(reserveButton, Pos.TOP_LEFT);
        reserveButton.setTranslateX(325);
        reserveButton.setTranslateY(425);

        settingsButton = new MenuButton("SETTINGS", 150, 40, MenuButton.Shape.LEFT);
        settingsButton.setOnMouseClicked(e -> display.showSettings());
        StackPane.setAlignment(settingsButton, Pos.TOP_RIGHT);
        settingsButton.setTranslateX(-325);
        settingsButton.setTranslateY(425);

        visualsButton = new MenuButton("VISUALS", 150, 40, MenuButton.Shape.CENTER);
        visualsButton.setOnMouseClicked(e -> display.showVisuals());
        StackPane.setAlignment(visualsButton, Pos.TOP_RIGHT);
        visualsButton.setTranslateX(-175);
        visualsButton.setTranslateY(425);

        eqButton = new MenuButton("EQUALIZER", 150, 40, MenuButton.Shape.CENTER);
        eqButton.setOnMouseClicked(e -> display.showEqualizer());
        StackPane.setAlignment(eqButton, Pos.TOP_RIGHT);
        eqButton.setTranslateX(-25);
        eqButton.setTranslateY(425);

        offButton = new OffButton("OFF");
        offButton.setOnMouseClicked(e -> shutdown());
        StackPane.setAlignment(offButton, Pos.TOP_LEFT);
        offButton.setTranslateX(20);
        offButton.setTranslateY(585);

        shuffleButton = new PlayModeButton("SHF", false);
        shuffleButton.setOnMouseClicked(e -> toggleShuffle());
        StackPane.setAlignment(shuffleButton, Pos.TOP_RIGHT);
        shuffleButton.setTranslateX(-65);
        shuffleButton.setTranslateY(569);

        repeatButton = new PlayModeButton("RPT", false);
        repeatButton.setOnMouseClicked(e -> toggleRepeat());
        StackPane.setAlignment(repeatButton, Pos.TOP_RIGHT);
        repeatButton.setTranslateX(-65);
        repeatButton.setTranslateY(495);

        controller.setShuffle(prefs.getBoolean(PREF_SHUFFLE, false));
        controller.setRepeat(prefs.getBoolean(PREF_REPEAT, false));
        shuffleButton.setActive(controller.isShuffle());
        repeatButton.setActive(controller.isRepeat());

        moveButton = new MenuButton("MOVE", 70, 30, MenuButton.Shape.BOTH);
        moveButton.setOnMousePressed(e -> {
            dragX = e.getScreenX() - getScene().getWindow().getX();
            dragY = e.getScreenY() - getScene().getWindow().getY();
        });
        moveButton.setOnMouseDragged(e -> {
            getScene().getWindow().setX(e.getScreenX() - dragX);
            getScene().getWindow().setY(e.getScreenY() - dragY);
        });
        StackPane.setAlignment(moveButton, Pos.TOP_CENTER);
        moveButton.setTranslateY(9);

        StackPane.setAlignment(displayArea, Pos.TOP_CENTER);
        StackPane.setAlignment(controlArea, Pos.BOTTOM_CENTER);

        getChildren().addAll(
                background,
                playerPanel,
                displayArea,
                controlArea,

                playlistButton,
                trackinfoButton,
                reserveButton,

                settingsButton,
                visualsButton,
                eqButton,

                offButton,
                shuffleButton,
                repeatButton,
                moveButton
        );

        installDragAndDrop();
        sceneProperty().addListener((obs, oldScene, newScene) -> {if (newScene != null) {installKeyHandlers(newScene);}});

        progressTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                display.updatePlaybackProgress(controller.getCurrentPositionMillis(), controller.getTotalDurationMillis());
            }
        };
        progressTimer.start();

        restoreSession();
    }

    // ============================================================
    // TRANSPORT
    // ============================================================

    private void togglePlayPause() {
        if (playlist.isEmpty()) {return;}
        PlayerController.State before = controller.getState();
        if (before == PlayerController.State.STOPPED) {display.showError(null);}
        controller.togglePlayPause();
        if (before == PlayerController.State.STOPPED && controller.getState() == PlayerController.State.PLAYING
                && !display.isTrackListVisible()) {
            display.showTrackInfo();
        }
    }

    private void updateTransportButtons(PlayerController.State state) {
        switch (state) {
            case PLAYING -> {
                playPauseButton.setText("PLAYING");
                playPauseButton.setActive(true);
                stopButton.setText("STOP");
                stopButton.setActive(false);
            }
            case PAUSED -> {
                playPauseButton.setText("PAUSED");
                playPauseButton.setActive(true);
                stopButton.setText("STOP");
                stopButton.setActive(false);
            }
            case STOPPED -> {
                playPauseButton.setText("PLAY/\nPAUSE");
                playPauseButton.setActive(false);
                stopButton.setText("STOPPED");
                stopButton.setActive(true);
                display.getVisualsDisplay().clear();
            }
        }
        display.updateCurrentTrack();
    }

    private void setVolume(double volume, boolean updateKnob) {
        double v = Math.max(0.0, Math.min(1.0, volume));
        if (updateKnob) {volumeKnob.setVolume(v);}
        audioEngine.setVolume((float) v);
        prefs.putDouble(PREF_VOLUME, v);
    }

    private void toggleMute() {
        boolean muted = !audioEngine.isMuted();
        audioEngine.setMuted(muted);
        volumeKnob.setMuted(muted);
    }

    private void toggleShuffle() {
        controller.setShuffle(!controller.isShuffle());
        shuffleButton.setActive(controller.isShuffle());
        prefs.putBoolean(PREF_SHUFFLE, controller.isShuffle());
    }

    private void toggleRepeat() {
        controller.setRepeat(!controller.isRepeat());
        repeatButton.setActive(controller.isRepeat());
        prefs.putBoolean(PREF_REPEAT, controller.isRepeat());
    }

    // ============================================================
    // TASTATUR
    // ============================================================

    private void installKeyHandlers(Scene scene) {
        scene.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (handleKey(e)) {e.consume();}
        });
    }

    private boolean handleKey(KeyEvent e) {
        KeyCode code = e.getCode();
        boolean ctrl = e.isShortcutDown();
        TrackListDisplay trackList = display.getTrackListDisplay();
        if (ctrl) {
            switch (code) {
                case O -> {if (e.isShiftDown()) {openFolder();} else {openFiles();}}
                case L -> addLocation();
                case S -> savePlaylist();
                case Q -> shutdown();
                default -> {return false;}
            }
            return true;
        }
        switch (code) {
            case SPACE, PLAY, PAUSE -> togglePlayPause();
            case S, STOP -> controller.stop();
            case N, PAGE_DOWN, TRACK_NEXT -> controller.next();
            case B, PAGE_UP, TRACK_PREV -> controller.previous();
            case RIGHT -> controller.seekBy(e.isShiftDown() ? SEEK_STEP_LARGE_MILLIS : SEEK_STEP_MILLIS);
            case LEFT -> controller.seekBy(-(e.isShiftDown() ? SEEK_STEP_LARGE_MILLIS : SEEK_STEP_MILLIS));
            case PLUS, ADD, EQUALS, VOLUME_UP -> setVolume(volumeKnob.getVolume() + VOLUME_STEP, true);
            case MINUS, SUBTRACT, VOLUME_DOWN -> setVolume(volumeKnob.getVolume() - VOLUME_STEP, true);
            case UP -> {
                if (display.isTrackListVisible()) {trackList.moveSelection(-1);}
                else {setVolume(volumeKnob.getVolume() + VOLUME_STEP, true);}
            }
            case DOWN -> {
                if (display.isTrackListVisible()) {trackList.moveSelection(1);}
                else {setVolume(volumeKnob.getVolume() - VOLUME_STEP, true);}
            }
            case ENTER -> {
                if (!display.isTrackListVisible()) {return false;}
                display.showError(null);
                trackList.playSelected();
            }
            case DELETE, BACK_SPACE -> {
                if (!display.isTrackListVisible()) {return false;}
                trackList.removeSelected();
            }
            case M, MUTE -> toggleMute();
            case H -> toggleShuffle();
            case R -> toggleRepeat();
            case DIGIT1, NUMPAD1 -> display.showTrackList();
            case DIGIT2, NUMPAD2 -> display.showTrackInfo();
            case DIGIT3, NUMPAD3 -> display.showSettings();
            case DIGIT4, NUMPAD4 -> display.showVisuals();
            case DIGIT5, NUMPAD5 -> display.showEqualizer();
            default -> {return false;}
        }
        return true;
    }

    // ============================================================
    // DATEIEN / PLAYLIST
    // ============================================================

    private void openFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open audio file");
        List<String> patterns = DecoderManager.SUPPORTED_EXTENSIONS.stream().map(ext -> "*." + ext).toList();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio files", patterns));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
        File initial = lastDirectory();
        if (initial != null) {chooser.setInitialDirectory(initial);}
        List<File> files = chooser.showOpenMultipleDialog(getScene().getWindow());
        if (files == null || files.isEmpty()) {return;}
        rememberDirectory(files.get(0).getParentFile());
        addEntriesAsync(files.stream().map(File::getAbsolutePath).toList(), null);
    }

    private void openFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open audio folder");
        File initial = lastDirectory();
        if (initial != null) {chooser.setInitialDirectory(initial);}
        File folder = chooser.showDialog(getScene().getWindow());
        if (folder == null) {return;}
        rememberDirectory(folder);
        addFilesFromFolderAsync(List.of(folder));
    }

    /** ADD LOCATION: URL (Internetradio/Stream) oder Pfad zu Datei/Ordner. */
    private void addLocation() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Location");
        dialog.setHeaderText("URL (http/https stream) or path to a file or folder");
        dialog.setContentText("Location:");
        if (getScene() != null) {dialog.initOwner(getScene().getWindow());}
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().isBlank()) {return;}
        String location = result.get().strip();
        if (location.length() > 1 && location.startsWith("\"") && location.endsWith("\"")) {
            location = location.substring(1, location.length() - 1);
        }
        if (Track.isUrl(location) && !location.toLowerCase(Locale.ROOT).startsWith("file:")) {
            Track stream = decoderManager.readStream(location);
            playlist.addTrack(stream);
            controller.playlistChanged();
            display.setPlaylist(playlist);
            display.getTrackListDisplay().ensureVisible(playlist.size() - 1);
            return;
        }
        File file = location.toLowerCase(Locale.ROOT).startsWith("file:") ? new File(java.net.URI.create(location)) : new File(location);
        if (file.isDirectory()) {addFilesFromFolderAsync(List.of(file));}
        else if (file.isFile()) {addDroppedFiles(List.of(file));}
        else {display.showError("Location not found: " + location);}
    }

    private void installDragAndDrop() {
        setOnDragOver(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles() || (db.hasUrl() && Track.isUrl(db.getUrl()))) {e.acceptTransferModes(TransferMode.COPY);}
            e.consume();
        });
        setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                addDroppedFiles(db.getFiles());
                success = true;
            } else if (db.hasUrl() && Track.isUrl(db.getUrl())) {
                playlist.addTrack(decoderManager.readStream(db.getUrl()));
                controller.playlistChanged();
                display.setPlaylist(playlist);
                success = true;
            }
            e.setDropCompleted(success);
            e.consume();
        });
    }

    private void addDroppedFiles(List<File> files) {
        List<File> folders = new ArrayList<>();
        List<String> entries = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory()) {folders.add(file);}
            else if (PlaylistManager.isM3u(file) || file.getName().toLowerCase(Locale.ROOT).endsWith(".daplist")) {
                try {entries.addAll(playlistManager.readEntries(file));} catch (IOException ex) {ex.printStackTrace();}
            } else if (DecoderManager.isSupportedFile(file)) {entries.add(file.getAbsolutePath());}
        }
        if (!entries.isEmpty()) {addEntriesAsync(entries, null);}
        if (!folders.isEmpty()) {addFilesFromFolderAsync(folders);}
        if (!display.isTrackListVisible()) {display.showTrackList();}
    }

    /** Durchsucht Ordner rekursiv (sortiert) im Hintergrund und fügt alle unterstützten Dateien hinzu. */
    private void addFilesFromFolderAsync(List<File> folders) {
        int generation = loadGeneration;
        display.getTrackListDisplay().setStatusText("SCANNING ...");
        loader.submit(() -> {
            List<String> entries = new ArrayList<>();
            for (File folder : folders) {
                try (Stream<Path> paths = Files.walk(folder.toPath())) {
                    paths.filter(Files::isRegularFile)
                         .filter(p -> DecoderManager.isSupportedFile(p.toFile()))
                         .sorted(Comparator.comparing((Path p) -> p.getParent().toString().toLowerCase(Locale.ROOT))
                                           .thenComparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                         .forEach(p -> entries.add(p.toAbsolutePath().toString()));
                } catch (Exception ex) {
                    System.err.println("Ordner nicht lesbar: " + folder + " (" + ex.getMessage() + ")");
                }
            }
            Platform.runLater(() -> {
                if (generation != loadGeneration) {return;}
                if (entries.isEmpty()) {
                    display.getTrackListDisplay().setStatusText(null);
                    display.showError("No supported audio files found");
                    return;
                }
                addEntriesAsync(entries, null);
            });
        });
    }

    /**
     * Liest Metadaten im Hintergrund und fügt die Tracks blockweise hinzu,
     * damit die Oberfläche auch bei großen Ordnern bedienbar bleibt.
     */
    private void addEntriesAsync(List<String> entries, Runnable onDone) {
        int generation = loadGeneration;
        int total = entries.size();
        TrackListDisplay trackList = display.getTrackListDisplay();
        trackList.setStatusText("LOADING 0/" + total);
        loader.submit(() -> {
            List<Track> batch = new ArrayList<>();
            int done = 0;
            for (String entry : entries) {
                if (generation != loadGeneration) {return;}
                try {
                    Track track = playlistManager.createTrack(entry, decoderManager);
                    if (track != null) {batch.add(track);}
                } catch (Throwable t) {
                    System.err.println("Track nicht lesbar: " + entry + " (" + t.getMessage() + ")");
                }
                done++;
                if (batch.size() >= LOAD_BATCH_SIZE || done == total) {
                    List<Track> publish = new ArrayList<>(batch);
                    batch.clear();
                    int progress = done;
                    Platform.runLater(() -> {
                        if (generation != loadGeneration) {return;}
                        playlist.addTracks(publish);
                        controller.playlistChanged();
                        trackList.setPlaylist(playlist);
                        trackList.setStatusText(progress < total ? "LOADING " + progress + "/" + total : null);
                        display.setPlaylist(playlist);
                    });
                }
            }
            if (total == 0) {Platform.runLater(() -> trackList.setStatusText(null));}
            if (onDone != null) {Platform.runLater(() -> {if (generation == loadGeneration) {onDone.run();}});}
        });
    }

    private void setActiveDisplay(String display) {
        playlistButton.setActive("PLAYLIST".equals(display));
        trackinfoButton.setActive("TRACKINFO".equals(display));
        reserveButton.setActive(false);

        settingsButton.setActive("SETTINGS".equals(display));
        visualsButton.setActive("VISUALS".equals(display));
        eqButton.setActive("EQUALIZER".equals(display));
    }

    private void redrawUi() {
        playerPanel.draw();
        previousButton.draw();
        stopButton.draw();
        playPauseButton.draw();
        nextButton.draw();
        volumeKnob.draw();
        offButton.draw();
        shuffleButton.draw();
        repeatButton.draw();
        playlistButton.draw();
        trackinfoButton.draw();
        reserveButton.draw();
        settingsButton.draw();
        visualsButton.draw();
        eqButton.draw();
        moveButton.draw();
    }

    private void savePlaylist() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Playlist");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("DAPlayer Playlist", "*.daplist"),
            new FileChooser.ExtensionFilter("M3U Playlist", "*.m3u8", "*.m3u"));
        chooser.setInitialDirectory(playlistManager.getPlaylistDirectory());
        chooser.setInitialFileName(playlist.getName() + ".daplist");
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {return;}
        try {playlistManager.save(playlist, file);}
        catch (IOException e) {
            e.printStackTrace();
            display.showError("Playlist could not be saved: " + e.getMessage());
        }
    }

    private void loadPlaylist() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Playlist");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Playlists", "*.daplist", "*.m3u8", "*.m3u"));
        chooser.setInitialDirectory(playlistManager.getPlaylistDirectory());
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file == null) {return;}
        try {
            List<String> entries = playlistManager.readEntries(file);
            loadGeneration++;
            controller.clearPlaylist();
            display.setPlaylist(playlist);
            addEntriesAsync(entries, null);
        } catch (IOException e) {
            e.printStackTrace();
            display.showError("Playlist could not be loaded: " + e.getMessage());
        }
    }

    // ============================================================
    // SITZUNG
    // ============================================================

    private void restoreSession() {
        File session = playlistManager.getSessionFile();
        if (!session.isFile()) {return;}
        try {
            List<String> entries = playlistManager.readEntries(session);
            int index = prefs.getInt(PREF_SESSION_INDEX, 0);
            addEntriesAsync(entries, () -> {
                playlist.setCurrentIndex(index);
                display.updateCurrentTrack();
            });
        } catch (IOException e) {
            System.err.println("Letzte Sitzung nicht lesbar: " + e.getMessage());
        }
    }

    /** Sichert Playlist, Lautstärke und Modi und beendet die Anwendung. */
    public void shutdown() {
        if (shutDown) {return;}
        shutDown = true;
        try {
            playlistManager.save(playlist, playlistManager.getSessionFile());
            prefs.putInt(PREF_SESSION_INDEX, Math.max(0, playlist.getCurrentIndex()));
            EqualizerSettings.save(equalizer);
            prefs.flush();
        } catch (Exception e) {
            System.err.println("Sitzung konnte nicht gespeichert werden: " + e.getMessage());
        }
        progressTimer.stop();
        loadGeneration++;
        loader.shutdownNow();
        controller.stop();
        Platform.exit();
    }

    private File lastDirectory() {
        String path = prefs.get(PREF_LAST_DIRECTORY, null);
        if (path == null) {return null;}
        File dir = new File(path);
        return dir.isDirectory() ? dir : null;
    }

    private void rememberDirectory(File dir) {
        if (dir != null) {prefs.put(PREF_LAST_DIRECTORY, dir.getAbsolutePath());}
    }
}
