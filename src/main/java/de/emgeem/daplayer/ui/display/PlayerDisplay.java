package de.emgeem.daplayer.ui.display;

import java.util.function.LongConsumer;

import de.emgeem.daplayer.audio.AudioDeviceManager;
import de.emgeem.daplayer.model.Playlist;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class PlayerDisplay extends StackPane {

    private final Font displayFont;
    private final Canvas backgroundCanvas;
    private final StartDisplay startDisplay;
    private final TrackListDisplay trackListDisplay;
    private final TrackInfoDisplay trackInfoDisplay;
    private final SettingsDisplay settingsDisplay;
    private final Runnable redrawUiAction;

    private DisplayMode currentDisplay;

    public PlayerDisplay(double width, double height, Runnable addFileAction, Runnable addFolderAction, AudioDeviceManager audioDeviceManager, Runnable redrawUiAction, LongConsumer seekAction) {
        this.redrawUiAction = redrawUiAction;
        setPrefSize(width, height);
        setMaxSize(width, height);
        backgroundCanvas = new Canvas();
        getChildren().add(backgroundCanvas);
        displayFont = Font.loadFont(getClass().getResourceAsStream("/fonts/JetBrainsMono-Regular.ttf"),18);
        
        startDisplay = new StartDisplay();
        startDisplay.prefWidthProperty().bind(widthProperty());
        startDisplay.prefHeightProperty().bind(heightProperty());

        trackListDisplay = new TrackListDisplay(displayFont, addFileAction, addFolderAction);
        trackListDisplay.prefWidthProperty().bind(widthProperty());
        trackListDisplay.prefHeightProperty().bind(heightProperty());

        trackInfoDisplay = new TrackInfoDisplay(displayFont);
        trackInfoDisplay.setSeekAction(seekAction);
        trackInfoDisplay.prefWidthProperty().bind(widthProperty());
        trackInfoDisplay.prefHeightProperty().bind(heightProperty());

        settingsDisplay = new SettingsDisplay(displayFont, audioDeviceManager, this::redraw);
        settingsDisplay.prefWidthProperty().bind(widthProperty());
        settingsDisplay.prefHeightProperty().bind(heightProperty());

        getChildren().addAll(startDisplay, trackListDisplay, trackInfoDisplay, settingsDisplay);
        trackListDisplay.setVisible(false);
        trackInfoDisplay.setVisible(false);
        settingsDisplay.setVisible(false);

        StackPane.setAlignment(startDisplay, Pos.TOP_LEFT);
        StackPane.setAlignment(trackListDisplay, Pos.TOP_LEFT);
        StackPane.setAlignment(trackInfoDisplay, Pos.TOP_LEFT);
        StackPane.setAlignment(settingsDisplay, Pos.TOP_LEFT);
        currentDisplay = startDisplay;

        widthProperty().addListener((obs, oldValue, newValue) -> draw());
        heightProperty().addListener((obs, oldValue, newValue) -> draw());
        Platform.runLater(this::draw);
    }

    private void draw() {
        double w = getWidth();
        double h = getHeight();
        backgroundCanvas.setWidth(w);
        backgroundCanvas.setHeight(h);
        GraphicsContext gc = backgroundCanvas.getGraphicsContext2D();
        
        gc.clearRect(0, 0, w, h);
        drawDisplay(gc, 0, 0, w, h);
        currentDisplay.draw();
    }
    public void redraw() {
        draw();
        redrawUiAction.run();
    }

    private void drawDisplay(GraphicsContext gc, double x, double y, double w, double h) {
// Außenschacht
        gc.setFill(Color.rgb(22, 22, 22));
        gc.fillRoundRect(x, y, w, h, 16, 16);
// oberes Highlight
        gc.setStroke(Color.rgb(90, 90, 90, 0.25));
        gc.setLineWidth(1);
        gc.strokeRoundRect(x + 1, y + 1, w - 2, h - 2, 16, 16);
// unterer Schatten
        gc.setStroke(Color.rgb(0, 0, 0, 0.70));
        gc.strokeRoundRect(x + 3, y + 3, w - 6, h - 6, 12, 12);
// eigentliche Displayfläche
        gc.setFill(Color.rgb(10, 10, 10));
        gc.fillRoundRect(x + 10, y + 10, w - 20, h - 20, 8, 8);
// Lichtkante links
        gc.setStroke(Color.rgb(90, 90, 90, 0.12));
        gc.strokeLine(x + 11, y + 14, x + 11, y + h - 14);
// Schatten rechts
        gc.setStroke(Color.rgb(0, 0, 0, 0.60));
        gc.strokeLine(x + w - 11, y + 14, x + w - 11, y + h - 14);
    }


    public void showTrackList() {
        startDisplay.setVisible(false);
        trackInfoDisplay.setVisible(false);
        settingsDisplay.setVisible(false);
        trackListDisplay.setVisible(true);
        currentDisplay = trackListDisplay;
        draw();
    }
    public void showTrackInfo() {
        startDisplay.setVisible(false);
        trackListDisplay.setVisible(false);
        settingsDisplay.setVisible(false);
        trackInfoDisplay.setVisible(true);
        currentDisplay = trackInfoDisplay;
        draw();
    }
    public void showSettings() {
        startDisplay.setVisible(false);
        trackListDisplay.setVisible(false);
        trackInfoDisplay.setVisible(false);
        settingsDisplay.setVisible(true);
        currentDisplay = settingsDisplay;
        draw();
    }
    

    public void setPlaylist(Playlist playlist) {
        trackListDisplay.setPlaylist(playlist);
        trackInfoDisplay.setPlaylist(playlist);
        draw();
    }

    public void updatePlaybackProgress(long position, long duration) {
        trackInfoDisplay.setPlaybackProgress(position, duration);
    }
}