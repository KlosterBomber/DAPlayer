package de.emgeem.daplayer.ui.display;

import java.util.function.Consumer;
import java.util.function.LongConsumer;

import de.emgeem.daplayer.audio.AudioDeviceManager;
import de.emgeem.daplayer.audio.Equalizer;
import de.emgeem.daplayer.audio.VisualizationListener;
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
    private final Font displayFontSmall;
    private final Canvas backgroundCanvas;
    private final StartDisplay startDisplay;
    private final TrackListDisplay trackListDisplay;
    private final TrackInfoDisplay trackInfoDisplay;
    private final SettingsDisplay settingsDisplay;
    private final VisualsDisplay visualsDisplay;
    private final EqualizerDisplay equalizerDisplay;
    private final Runnable redrawUiAction;
    private final Consumer<String> activeDisplayAction;

    private DisplayMode currentDisplay;

    public PlayerDisplay(double width, double height, Runnable addFileAction, Runnable addFolderAction, AudioDeviceManager audioDeviceManager, Equalizer equalizer,
                         Runnable redrawUiAction, LongConsumer seekAction, Consumer<String> activeDisplayAction) {
        this.redrawUiAction = redrawUiAction;
        this.activeDisplayAction = activeDisplayAction;
        setPrefSize(width, height);
        setMaxSize(width, height);
        backgroundCanvas = new Canvas();
        getChildren().add(backgroundCanvas);
        displayFont = Font.loadFont(getClass().getResourceAsStream("/fonts/JetBrainsMono-Regular.ttf"),18);
        displayFontSmall = Font.loadFont(getClass().getResourceAsStream("/fonts/JetBrainsMono-Regular.ttf"),14);
        
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

        settingsDisplay = new SettingsDisplay(displayFont, displayFontSmall, audioDeviceManager, this::redraw);
        settingsDisplay.prefWidthProperty().bind(widthProperty());
        settingsDisplay.prefHeightProperty().bind(heightProperty());

        visualsDisplay = new VisualsDisplay(displayFont);
        visualsDisplay.prefWidthProperty().bind(widthProperty());
        visualsDisplay.prefHeightProperty().bind(heightProperty());        

        equalizerDisplay = new EqualizerDisplay(displayFont, equalizer);
        equalizerDisplay.prefWidthProperty().bind(widthProperty());
        equalizerDisplay.prefHeightProperty().bind(heightProperty());


        getChildren().addAll(startDisplay, trackListDisplay, trackInfoDisplay, settingsDisplay,visualsDisplay, equalizerDisplay);
        trackListDisplay.setVisible(false);
        trackInfoDisplay.setVisible(false);
        settingsDisplay.setVisible(false);
        visualsDisplay.setVisible(false);
        equalizerDisplay.setVisible(false);

        StackPane.setAlignment(startDisplay, Pos.TOP_LEFT);
        StackPane.setAlignment(trackListDisplay, Pos.TOP_LEFT);
        StackPane.setAlignment(trackInfoDisplay, Pos.TOP_LEFT);
        StackPane.setAlignment(settingsDisplay, Pos.TOP_LEFT);
        StackPane.setAlignment(visualsDisplay, Pos.TOP_LEFT);
        StackPane.setAlignment(equalizerDisplay, Pos.TOP_LEFT);
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
        visualsDisplay.setVisible(false);
        equalizerDisplay.setVisible(false);
        trackListDisplay.setVisible(true);
        currentDisplay = trackListDisplay;
        activeDisplayAction.accept("PLAYLIST");
        draw();
    }
    public void showTrackInfo() {
        startDisplay.setVisible(false);
        trackListDisplay.setVisible(false);
        settingsDisplay.setVisible(false);
        visualsDisplay.setVisible(false);
        equalizerDisplay.setVisible(false);
        trackInfoDisplay.setVisible(true);
        currentDisplay = trackInfoDisplay;
        activeDisplayAction.accept("TRACKINFO");
        draw();
    }
    public void showSettings() {
        startDisplay.setVisible(false);
        trackListDisplay.setVisible(false);
        trackInfoDisplay.setVisible(false);
        visualsDisplay.setVisible(false);
        equalizerDisplay.setVisible(false);
        settingsDisplay.setVisible(true);
        currentDisplay = settingsDisplay;
        activeDisplayAction.accept("SETTINGS");
        draw();
    }
    public void showVisuals() {
        startDisplay.setVisible(false);
        trackListDisplay.setVisible(false);
        trackInfoDisplay.setVisible(false);
        settingsDisplay.setVisible(false);
        equalizerDisplay.setVisible(false);
        visualsDisplay.setVisible(true);
        currentDisplay = visualsDisplay;
        activeDisplayAction.accept("VISUALS");
        draw();
    }
    public void showEqualizer() {
        startDisplay.setVisible(false);
        trackListDisplay.setVisible(false);
        trackInfoDisplay.setVisible(false);
        settingsDisplay.setVisible(false);
        visualsDisplay.setVisible(false);
        equalizerDisplay.setVisible(true);
        currentDisplay = equalizerDisplay;
        activeDisplayAction.accept("EQUALIZER");
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

    public void updateCurrentTrack() {trackListDisplay.updateCurrentTrack();}

    public VisualizationListener getVisualizationListener() {return visualsDisplay;}
}