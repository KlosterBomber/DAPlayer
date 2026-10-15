package de.emgeem.daplayer.ui.display;

import java.util.List;
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
    private final List<DisplayMode> displays;
    private final Runnable redrawUiAction;
    private final Consumer<String> activeDisplayAction;

    private DisplayMode currentDisplay;

    public PlayerDisplay(double width, double height, Runnable addFileAction, Runnable addFolderAction, AudioDeviceManager audioDeviceManager, Equalizer equalizer,
                         Runnable redrawUiAction, LongConsumer seekAction, Consumer<String> activeDisplayAction, Runnable savePlaylistAction, Runnable loadPlaylistAction) {
        this.redrawUiAction = redrawUiAction;
        this.activeDisplayAction = activeDisplayAction;
        setPrefSize(width, height);
        setMaxSize(width, height);
        backgroundCanvas = new Canvas();
        getChildren().add(backgroundCanvas);
        displayFont = Font.loadFont(getClass().getResourceAsStream("/fonts/JetBrainsMono-Regular.ttf"),18);
        displayFontSmall = Font.loadFont(getClass().getResourceAsStream("/fonts/JetBrainsMono-Regular.ttf"),14);

        startDisplay = new StartDisplay();
        trackListDisplay = new TrackListDisplay(displayFont, addFileAction, addFolderAction, savePlaylistAction, loadPlaylistAction);
        trackInfoDisplay = new TrackInfoDisplay(displayFont);
        trackInfoDisplay.setSeekAction(seekAction);
        settingsDisplay = new SettingsDisplay(displayFont, displayFontSmall, audioDeviceManager, this::redraw);
        visualsDisplay = new VisualsDisplay(displayFont);
        equalizerDisplay = new EqualizerDisplay(displayFont, equalizer);

        displays = List.of(startDisplay, trackListDisplay, trackInfoDisplay, settingsDisplay, visualsDisplay, equalizerDisplay);
        for (DisplayMode display : displays) {
            display.prefWidthProperty().bind(widthProperty());
            display.prefHeightProperty().bind(heightProperty());
            display.setVisible(display == startDisplay);
            StackPane.setAlignment(display, Pos.TOP_LEFT);
        }
        getChildren().addAll(displays);
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

    private void show(DisplayMode display, String name) {
        for (DisplayMode d : displays) {d.setVisible(d == display);}
        currentDisplay = display;
        activeDisplayAction.accept(name);
        draw();
    }

    public void showTrackList() {show(trackListDisplay, "PLAYLIST");}
    public void showTrackInfo() {show(trackInfoDisplay, "TRACKINFO");}
    public void showSettings() {show(settingsDisplay, "SETTINGS");}
    public void showVisuals() {show(visualsDisplay, "VISUALS");}
    public void showEqualizer() {show(equalizerDisplay, "EQUALIZER");}

    public boolean isTrackListVisible() {return currentDisplay == trackListDisplay;}

    public void setPlaylist(Playlist playlist) {
        trackListDisplay.setPlaylist(playlist);
        trackInfoDisplay.setPlaylist(playlist);
        draw();
    }

    public void updatePlaybackProgress(long position, long duration) {
        trackInfoDisplay.setPlaybackProgress(position, duration);
    }

    public void updateCurrentTrack() {
        trackListDisplay.updateCurrentTrack();
        trackInfoDisplay.draw();
    }

    public void showError(String message) {trackInfoDisplay.setErrorText(message);}

    public TrackListDisplay getTrackListDisplay() {return trackListDisplay;}
    public TrackInfoDisplay getTrackInfoDisplay() {return trackInfoDisplay;}
    public SettingsDisplay getSettingsDisplay() {return settingsDisplay;}
    public VisualsDisplay getVisualsDisplay() {return visualsDisplay;}
    public EqualizerDisplay getEqualizerDisplay() {return equalizerDisplay;}

    public VisualizationListener getVisualizationListener() {return visualsDisplay;}
}
