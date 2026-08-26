package de.emgeem.daplayer.ui;

import de.emgeem.daplayer.ui.background.PlayerViewBackground;
import de.emgeem.daplayer.ui.control.RoundControlButton;
import de.emgeem.daplayer.ui.control.VolumeKnob;
import de.emgeem.daplayer.ui.display.PlayerDisplay;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class DAPlayerView extends StackPane {

    private static final double DISPLAY_TOP_MARGIN = 35;
    private static final double DISPLAY_SIDE_MARGIN = 50;
    private static final double DISPLAY_HEIGHT = 400;
    private static final double CONTROL_TOP_MARGIN = 15;
    private static final double CONTROL_BOTTOM_MARGIN = 15;
    private static final double CONTROL_AREA_HEIGHT = 240;
    private static final double CONTROL_SPACING = 35;


    public DAPlayerView() {

        PlayerViewBackground background = new PlayerViewBackground(1200, 675);

        background.widthProperty().bind(widthProperty());
        background.heightProperty().bind(heightProperty());

    // DISPLAY AREA
        StackPane displayArea = new StackPane();
        displayArea.setAlignment(Pos.TOP_CENTER);
        displayArea.setPadding(new Insets(DISPLAY_TOP_MARGIN, DISPLAY_SIDE_MARGIN, 0, DISPLAY_SIDE_MARGIN));

        PlayerDisplay display = new PlayerDisplay(1000, DISPLAY_HEIGHT);
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
//        volumeKnob.setTranslateY(-10);

        StackPane.setAlignment(displayArea, Pos.TOP_CENTER);
        StackPane.setAlignment(controlArea, Pos.BOTTOM_CENTER);

        getChildren().addAll(
                background,
                displayArea,
                controlArea
        );
    }
}