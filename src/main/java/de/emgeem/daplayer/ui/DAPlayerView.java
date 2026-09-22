package de.emgeem.daplayer.ui;

import java.io.File;
import java.util.List;

import de.emgeem.daplayer.audio.AudioDeviceManager;
import de.emgeem.daplayer.audio.AudioEngine;
import de.emgeem.daplayer.audio.Equalizer;
import de.emgeem.daplayer.audio.EqualizerSettings;
import de.emgeem.daplayer.audio.PlayerController;
import de.emgeem.daplayer.decoder.DecoderManager;
import de.emgeem.daplayer.model.Playlist;
import de.emgeem.daplayer.model.Track;
import de.emgeem.daplayer.ui.background.PlayerPanel;
import de.emgeem.daplayer.ui.background.PlayerViewBackground;
import de.emgeem.daplayer.ui.control.MenuButton;
import de.emgeem.daplayer.ui.control.OffButton;
import de.emgeem.daplayer.ui.control.RoundControlButton;
import de.emgeem.daplayer.ui.control.VolumeKnob;
import de.emgeem.daplayer.ui.display.PlayerDisplay;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

    private final Playlist playlist = new Playlist("Current");
    private final DecoderManager decoderManager = new DecoderManager();
    private final PlayerDisplay display;
    private final AudioDeviceManager audioDeviceManager = new AudioDeviceManager();

    private final PlayerPanel playerPanel;
    private final RoundControlButton previousButton;
    private final RoundControlButton stopButton;
    private final RoundControlButton playPauseButton;
    private final RoundControlButton nextButton;
    private final VolumeKnob volumeKnob;
    private final OffButton offButton;
    private final MenuButton playlistButton;
    private final MenuButton trackinfoButton;
    private final MenuButton settingsButton;
    private final MenuButton reserveButton;
    private final MenuButton visualsButton;
    private final MenuButton eqButton;
    private final MenuButton moveButton;
    private final AudioEngine audioEngine;
    private final PlayerController controller;
    private final Equalizer equalizer;

    private double dragX;
    private double dragY;

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
                                    this::setActiveDisplay);
        display.setPlaylist(playlist);
        displayArea.getChildren().add(display);

    // CONTROL AREA
        StackPane controlArea = new StackPane();
        controlArea.setPrefHeight(CONTROL_AREA_HEIGHT);
        controlArea.setMaxHeight(CONTROL_AREA_HEIGHT);
        controlArea.setAlignment(Pos.CENTER);
        controlArea.setPadding(new Insets(CONTROL_TOP_MARGIN, 0, CONTROL_BOTTOM_MARGIN, 0));

        previousButton = new RoundControlButton("PREV");
        stopButton = new RoundControlButton("STOP");
        playPauseButton = new RoundControlButton("PLAY/\nPAUSE");
        nextButton = new RoundControlButton("NEXT");    

        volumeKnob = new VolumeKnob("VOLUME");

        HBox controlRow = new HBox();
        controlRow.setAlignment(Pos.CENTER);
        controlRow.setSpacing(CONTROL_SPACING);
        controlRow.getChildren().addAll(previousButton, stopButton, volumeKnob, playPauseButton, nextButton);

        controlArea.getChildren().add(controlRow);
        previousButton.setTranslateY(+25);
        stopButton.setTranslateY(+25);
        volumeKnob.setTranslateY(-5);
        playPauseButton.setTranslateY(+25);
        nextButton.setTranslateY(+25);

playPauseButton.setOnMouseClicked(e -> {
    if (!controller.isPlaying()) {
        if (!playlist.getTracks().isEmpty()) {
            controller.play();
            playPauseButton.setText("PLAYING");
            display.showTrackInfo();
        }
    } else if (!controller.isPaused()) {
        controller.pause();
        playPauseButton.setText("PAUSED");
    } else {
        controller.resume();
        playPauseButton.setText("PLAYING");
    }
});
stopButton.setOnMouseClicked(e -> {
    controller.stop();
    playPauseButton.setText("PLAY/\nPAUSE");
});
nextButton.setOnMouseClicked(e -> controller.next());
previousButton.setOnMouseClicked(e -> controller.previous());
volumeKnob.setVolumeListener(v -> audioEngine.setVolume((float) v));
volumeKnob.setMuteListener(m -> audioEngine.setMuted(m));


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

        settingsButton = new MenuButton("SETTINGS", 150, 40, MenuButton.Shape.RIGHT);
        settingsButton.setOnMouseClicked(e -> display.showSettings());
        StackPane.setAlignment(settingsButton, Pos.TOP_LEFT);
        settingsButton.setTranslateX(325);
        settingsButton.setTranslateY(425);

        reserveButton = new MenuButton("RESERVE", 150, 40, MenuButton.Shape.LEFT);
        StackPane.setAlignment(reserveButton, Pos.TOP_RIGHT);
        reserveButton.setTranslateX(-325);
        reserveButton.setTranslateY(425);

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
        offButton.setOnMouseClicked(e -> {getScene().getWindow().hide();});
        StackPane.setAlignment(offButton, Pos.TOP_LEFT);
        offButton.setTranslateX(20);
        offButton.setTranslateY(585);

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
                settingsButton,

                reserveButton,
                visualsButton,
                eqButton,

                offButton,
                moveButton
        );
        
        AnimationTimer progressTimer = new AnimationTimer() {
            @Override 
            public void handle(long now) {
                display.updatePlaybackProgress(controller.getCurrentPositionMillis(), controller.getTotalDurationMillis());
            }
        };
        progressTimer.start();
    }

    private void openFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open audio file");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Audio files", "*.wav", "*.mp3", "*.wma", "*.flac", "*.ogg", "*.aiff")
        );
        List<File> files = chooser.showOpenMultipleDialog(getScene().getWindow());
        if (files != null) {
            for (File file : files) {
                Track track = decoderManager.readTrack(file);
                playlist.addTrack(track);
        }
            display.setPlaylist(playlist);
        }
    }

    private void openFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open audio folder");
        File folder = chooser.showDialog(getScene().getWindow());
        if (folder == null) {return;}
        File[] files = folder.listFiles();
        if (files == null) {return;}
        for (File file : files) {
            if (!file.isFile()) {continue;}
            String name = file.getName().toLowerCase();
            if (!name.endsWith(".wav")
                    && !name.endsWith(".mp3")
                    && !name.endsWith(".wma")
                    && !name.endsWith(".flac")
                    && !name.endsWith(".ogg")
                    && !name.endsWith(".aiff")) {
                continue;
            }
            Track track = decoderManager.readTrack(file);
            playlist.addTrack(track);
        }
        display.setPlaylist(playlist);
    }

    private void setActiveDisplay(String display) {
        playlistButton.setActive("PLAYLIST".equals(display));
        trackinfoButton.setActive("TRACKINFO".equals(display));
        settingsButton.setActive("SETTINGS".equals(display));
        reserveButton.setActive(false);
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
        playlistButton.draw();
        trackinfoButton.draw();
        settingsButton.draw();
        reserveButton.draw();
        visualsButton.draw();
        eqButton.draw();
        moveButton.draw();
    }
}