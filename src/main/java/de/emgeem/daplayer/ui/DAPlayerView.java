package de.emgeem.daplayer.ui;

import java.io.File;
import java.util.List;

import de.emgeem.daplayer.audio.AudioDeviceManager;
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

    private double dragX;
    private double dragY;

    public DAPlayerView() {

        PlayerViewBackground background = new PlayerViewBackground(1200, 675);

        background.widthProperty().bind(widthProperty());
        background.heightProperty().bind(heightProperty());

    // PLAYER PANEL
        PlayerPanel playerPanel = new PlayerPanel(1200, 675);
        StackPane.setAlignment(playerPanel, Pos.TOP_CENTER);

    // DISPLAY AREA
        StackPane displayArea = new StackPane();
        displayArea.setAlignment(Pos.TOP_CENTER);
        displayArea.setPadding(new Insets(DISPLAY_TOP_MARGIN, DISPLAY_SIDE_MARGIN, 0, DISPLAY_SIDE_MARGIN));

        display = new PlayerDisplay(1158, DISPLAY_HEIGHT, this::openFiles, this::openFolder, audioDeviceManager);
        display.setPlaylist(playlist);
        displayArea.getChildren().add(display);

    // CONTROL AREA
        StackPane controlArea = new StackPane();
        controlArea.setPrefHeight(CONTROL_AREA_HEIGHT);
        controlArea.setMaxHeight(CONTROL_AREA_HEIGHT);
        controlArea.setAlignment(Pos.CENTER);
        controlArea.setPadding(new Insets(CONTROL_TOP_MARGIN, 0, CONTROL_BOTTOM_MARGIN, 0));

        RoundControlButton previousButton = new RoundControlButton("PREV");
        RoundControlButton stopButton = new RoundControlButton("STOP");
        RoundControlButton playPauseButton = new RoundControlButton("PLAY/\nPAUSE");
        RoundControlButton nextButton = new RoundControlButton("NEXT");    

        VolumeKnob volumeKnob = new VolumeKnob("VOLUME");

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

// MENU BUTTONS
    
        MenuButton playlistButton = new MenuButton("PLAYLIST", 150, 40, MenuButton.Shape.CENTER);
        playlistButton.setOnMouseClicked(e -> display.showTrackList());
        StackPane.setAlignment(playlistButton, Pos.TOP_LEFT);
        playlistButton.setTranslateX(25);
        playlistButton.setTranslateY(425);

        MenuButton trackinfoButton = new MenuButton("TRACKINFO", 150, 40, MenuButton.Shape.CENTER);
        trackinfoButton.setOnMouseClicked(e -> display.showTrackInfo());
        StackPane.setAlignment(trackinfoButton, Pos.TOP_LEFT);
        trackinfoButton.setTranslateX(175);
        trackinfoButton.setTranslateY(425);

        MenuButton settingsButton = new MenuButton("SETTINGS", 150, 40, MenuButton.Shape.RIGHT);
        settingsButton.setOnMouseClicked(e -> display.showSettings());
        StackPane.setAlignment(settingsButton, Pos.TOP_LEFT);
        settingsButton.setTranslateX(325);
        settingsButton.setTranslateY(425);

        MenuButton reserveButton = new MenuButton("RESERVE", 150, 40, MenuButton.Shape.LEFT);
        StackPane.setAlignment(reserveButton, Pos.TOP_RIGHT);
        reserveButton.setTranslateX(-325);
        reserveButton.setTranslateY(425);

        MenuButton visualsButton = new MenuButton("VISUALS", 150, 40, MenuButton.Shape.CENTER);
        StackPane.setAlignment(visualsButton, Pos.TOP_RIGHT);
        visualsButton.setTranslateX(-175);
        visualsButton.setTranslateY(425);

        MenuButton eqButton = new MenuButton("EQUALIZER", 150, 40, MenuButton.Shape.CENTER);
        StackPane.setAlignment(eqButton, Pos.TOP_RIGHT);
        eqButton.setTranslateX(-25);
        eqButton.setTranslateY(425);

        OffButton offButton = new OffButton("OFF"); 
        offButton.setOnMouseClicked(e -> {getScene().getWindow().hide();});
        StackPane.setAlignment(offButton, Pos.TOP_LEFT);
        offButton.setTranslateX(20);
        offButton.setTranslateY(585);

        MenuButton moveButton = new MenuButton("MOVE", 70, 30, MenuButton.Shape.BOTH);
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
    }

    private void openFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open audio file");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Audio files", "*.wav", "*.mp3", "*.wma", "*.flac", "*.ogg", "*.aiff")
        );
        List<File> files = chooser.showOpenMultipleDialog(getScene().getWindow());
        if (files != null) {
            for (File file : files) {Track track = decoderManager.readTrack(file);
            playlist.addTrack(track);
                            
            System.out.println(
                track.getFileName()
                + " | Titel: " + track.getTitle()
                + " | Artist: " + track.getArtist()
                + " | Album: " + track.getAlbum()
            );}
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
            System.out.println(
                track.getFileName()
                + " | Titel: " + track.getTitle()
                + " | Artist: " + track.getArtist()
                + " | Album: " + track.getAlbum()
            );
        }
        display.setPlaylist(playlist);
    }   
}