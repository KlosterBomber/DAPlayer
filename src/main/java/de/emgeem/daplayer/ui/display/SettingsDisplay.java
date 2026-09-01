package de.emgeem.daplayer.ui.display;

import de.emgeem.daplayer.audio.AudioDeviceManager;
import de.emgeem.daplayer.model.Playlist;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class SettingsDisplay extends DisplayMode {

    private final Font displayFont;
    private final Canvas canvas;
    private final AudioDeviceManager audioDeviceManager;
    private final AudioDeviceSettings audioDeviceSettings;

    public SettingsDisplay(Font displayFont, AudioDeviceManager audioDeviceManager) {
        this.displayFont = displayFont;
        this.audioDeviceManager = audioDeviceManager;
        canvas = new Canvas();
        widthProperty().addListener((obs, oldValue, newValue) -> draw());
        heightProperty().addListener((obs, oldValue, newValue) -> draw());
        audioDeviceSettings = new AudioDeviceSettings(displayFont, audioDeviceManager);        
        audioDeviceSettings.prefWidthProperty().bind(widthProperty());
        audioDeviceSettings.prefHeightProperty().bind(heightProperty());
        getChildren().addAll(canvas, audioDeviceSettings);
    }

    public void draw() {
        double w = getWidth();
        double h = getHeight();
        canvas.setWidth(w);
        canvas.setHeight(h);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        // Trennlinie
        gc.setStroke(Color.rgb(90, 90, 90, 0.35));
        gc.setLineWidth(1);
        gc.strokeLine(20, 345, w - 20, 345);
        // Untermenübuttons
        drawButton(gc, 30, 350, 150, 30, "AUDIO DEVICE");
        drawButton(gc, 190, 350, 150, 30, "ACCENT COLORS");
        drawButton(gc, 350, 350, 150, 30, "KEY CONTROLS");
        audioDeviceSettings.draw();
    }

    private void drawButton(GraphicsContext gc, double x, double y, double width, double height, String text) {
        gc.setFill(Color.rgb(25, 25, 25));
        gc.fillRoundRect(x, y, width, height, 6, 6);
        gc.setStroke(Color.rgb(90, 90, 90, 0.35));
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, width, height, 6, 6);
        gc.setFill(Color.rgb(180, 180, 180));
        gc.setFont(displayFont);
        double textWidth = gc.getFont().getSize() * text.length() * 0.6;
        gc.fillText(text, x + (width - textWidth) / 2, y + 21);
    }

    @Override
    public void setPlaylist(Playlist playlist) {}
}