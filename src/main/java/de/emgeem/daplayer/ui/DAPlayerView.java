package de.emgeem.daplayer.ui;

import de.emgeem.daplayer.ui.background.PlayerPanel;
import de.emgeem.daplayer.ui.background.PlayerViewBackground;
import de.emgeem.daplayer.ui.control.MenuButton;
import de.emgeem.daplayer.ui.control.RoundControlButton;
import de.emgeem.daplayer.ui.control.VolumeKnob;
import de.emgeem.daplayer.ui.display.PlayerDisplay;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class DAPlayerView extends StackPane {

    private static final double DISPLAY_TOP_MARGIN = 15;
    private static final double DISPLAY_SIDE_MARGIN = 21;
    private static final double DISPLAY_HEIGHT = 400;
    private static final double CONTROL_TOP_MARGIN = 15;
    private static final double CONTROL_BOTTOM_MARGIN = 15;
    private static final double CONTROL_AREA_HEIGHT = 240;
    private static final double CONTROL_SPACING = 50;

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

        PlayerDisplay display = new PlayerDisplay(1158, DISPLAY_HEIGHT);
        display.widthProperty().bind(widthProperty().subtract(DISPLAY_SIDE_MARGIN * 2));
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
        MenuButton openButton = new MenuButton("OPEN", 150, MenuButton.Shape.LEFT);
        StackPane.setAlignment(openButton, Pos.TOP_LEFT);
        openButton.setTranslateX(55);
        openButton.setTranslateY(425);

        MenuButton playlistButton = new MenuButton("PLAYLIST", 150, MenuButton.Shape.CENTER);
        StackPane.setAlignment(playlistButton, Pos.TOP_LEFT);
        playlistButton.setTranslateX(205);
        playlistButton.setTranslateY(425);

        MenuButton settingsButton = new MenuButton("SETTINGS", 150, MenuButton.Shape.RIGHT);
        StackPane.setAlignment(settingsButton, Pos.TOP_LEFT);
        settingsButton.setTranslateX(355);
        settingsButton.setTranslateY(425);

        MenuButton displayButton = new MenuButton("DISPLAY", 150, MenuButton.Shape.LEFT);
        StackPane.setAlignment(displayButton, Pos.TOP_RIGHT);
        displayButton.setTranslateX(-355);
        displayButton.setTranslateY(425);

        MenuButton visualsButton = new MenuButton("VISUALS", 150, MenuButton.Shape.CENTER);
        StackPane.setAlignment(visualsButton, Pos.TOP_RIGHT);
        visualsButton.setTranslateX(-205);
        visualsButton.setTranslateY(425);

        MenuButton eqButton = new MenuButton("EQUALIZER", 150, MenuButton.Shape.RIGHT);
        StackPane.setAlignment(eqButton, Pos.TOP_RIGHT);
        eqButton.setTranslateX(-55);
        eqButton.setTranslateY(425);

        MenuButton offButton = new MenuButton("OFF", 50, MenuButton.Shape.CENTER);
        offButton.setOnMouseClicked(e -> {getScene().getWindow().hide();});
        StackPane.setAlignment(offButton, Pos.TOP_LEFT);
        offButton.setTranslateX(30);
        offButton.setTranslateY(605);

        MenuButton moveButton = new MenuButton("MOVE", 50, MenuButton.Shape.CENTER);
        moveButton.setOnMousePressed(e -> {
            dragX = e.getScreenX() - getScene().getWindow().getX();
            dragY = e.getScreenY() - getScene().getWindow().getY();
        });
        moveButton.setOnMouseDragged(e -> {
            getScene().getWindow().setX(e.getScreenX() - dragX);
            getScene().getWindow().setY(e.getScreenY() - dragY);
        });
        StackPane.setAlignment(moveButton, Pos.TOP_RIGHT);
        moveButton.setTranslateX(-30);
        moveButton.setTranslateY(605);

        StackPane.setAlignment(displayArea, Pos.TOP_CENTER);
        StackPane.setAlignment(controlArea, Pos.BOTTOM_CENTER);

        getChildren().addAll(
                background,
                playerPanel,
                displayArea,
                controlArea,

                openButton,
                playlistButton,
                settingsButton,

                displayButton,
                visualsButton,
                eqButton,

                offButton,
                moveButton
        );

        
        
    }
}