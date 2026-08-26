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
        MenuButton offButton = new MenuButton("OFF", 150, MenuButton.Shape.LEFT);
        MenuButton loadButton = new MenuButton("LOAD", 150, MenuButton.Shape.CENTER);
        MenuButton settingsButton = new MenuButton("SETTINGS", 150, MenuButton.Shape.RIGHT);
        MenuButton menuButton = new MenuButton("MENU", 150, MenuButton.Shape.LEFT);
        MenuButton visualsButton = new MenuButton("VISUALS", 150, MenuButton.Shape.CENTER);
        MenuButton reserveButton = new MenuButton("RESERVE", 150, MenuButton.Shape.RIGHT);
        StackPane.setAlignment(offButton, Pos.TOP_LEFT);
        StackPane.setAlignment(loadButton, Pos.TOP_LEFT);
        StackPane.setAlignment(settingsButton, Pos.TOP_LEFT);
        StackPane.setAlignment(menuButton, Pos.TOP_RIGHT);
        StackPane.setAlignment(visualsButton, Pos.TOP_RIGHT);
        StackPane.setAlignment(reserveButton, Pos.TOP_RIGHT);
        offButton.setTranslateX(55);
        offButton.setTranslateY(425);
        loadButton.setTranslateX(205);
        loadButton.setTranslateY(425);
        settingsButton.setTranslateX(355);
        settingsButton.setTranslateY(425);
        menuButton.setTranslateX(-355);
        menuButton.setTranslateY(425);
        visualsButton.setTranslateX(-205);
        visualsButton.setTranslateY(425);
        reserveButton.setTranslateX(-55);
        reserveButton.setTranslateY(425);

        StackPane.setAlignment(displayArea, Pos.TOP_CENTER);
        StackPane.setAlignment(controlArea, Pos.BOTTOM_CENTER);

        getChildren().addAll(
                background,
                playerPanel,
                displayArea,
                controlArea,

                offButton,
                loadButton,
                settingsButton,

                menuButton,
                visualsButton,
                reserveButton
        );
    }
}