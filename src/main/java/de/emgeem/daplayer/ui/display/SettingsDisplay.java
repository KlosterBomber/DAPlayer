package de.emgeem.daplayer.ui.display;

import de.emgeem.daplayer.audio.AudioDeviceManager;
import de.emgeem.daplayer.model.Playlist;
import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;

public class SettingsDisplay extends DisplayMode {

    private final Font displayFont;
    private final Font displayFontSmall;
    private final Canvas canvas;
    private final AudioDeviceManager audioDeviceManager;
    private final AudioDeviceSettings audioDeviceSettings;
    private final AccentColorSettings accentColorSettings;

    public SettingsDisplay(Font displayFont, Font displayFontSmall, AudioDeviceManager audioDeviceManager, Runnable redrawAction) {
        this.displayFont = displayFont;
        this.displayFontSmall = displayFontSmall;
        this.audioDeviceManager = audioDeviceManager;
        canvas = new Canvas();
        widthProperty().addListener((obs, oldValue, newValue) -> draw());
        heightProperty().addListener((obs, oldValue, newValue) -> draw());
        audioDeviceSettings = new AudioDeviceSettings(displayFont, audioDeviceManager);        
        audioDeviceSettings.prefWidthProperty().bind(widthProperty());
        audioDeviceSettings.prefHeightProperty().bind(heightProperty());
        accentColorSettings = new AccentColorSettings(displayFont, displayFontSmall, redrawAction);
        getChildren().addAll(canvas, audioDeviceSettings, accentColorSettings);
        audioDeviceSettings.setVisible(false);
        accentColorSettings.setVisible(false);
        setOnMouseClicked(e ->{
            double x = e.getX();
            double y = e.getY();
            if (y >= 350 && y <= 380) {
                if (x >= 30 && x <= 180) {
                    audioDeviceSettings.setVisible(true);
                    accentColorSettings.setVisible(false);
                    draw();
                }
                if (x >= 190 && x <= 340) {
                    audioDeviceSettings.setVisible(false);
                    accentColorSettings.setVisible(true);
                    draw();
                }
            }
        });
    }

    public void draw() {
        double w = getWidth();
        double h = getHeight();
        canvas.setWidth(w);
        canvas.setHeight(h);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        // Trennlinie
        gc.setStroke(UiColors.accentLight());
        gc.setEffect(UiColors.displayGlow());
        gc.setLineWidth(1);
        gc.strokeLine(20, 345, w - 20, 345);
        // Untermenübuttons
        drawButton(gc, 30, 350, 150, 30, "AUDIO DEVICE");
        drawButton(gc, 190, 350, 150, 30, "ACCENT COLORS");
        drawButton(gc, 350, 350, 150, 30, "KEY CONTROLS");
        gc.setEffect(null);
        if (audioDeviceSettings.isVisible()) {audioDeviceSettings.draw();}
        if (accentColorSettings.isVisible()) {accentColorSettings.draw();}
    }

    private void drawButton(GraphicsContext gc, double x, double y, double width, double height, String text) {
        gc.setFill(UiColors.accentLight().deriveColor(0, 1, 1, 0.08));
        gc.fillRoundRect(x, y, width, height, 6, 6);
        gc.setStroke(UiColors.accentLight());
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, width, height, 6, 6);
        gc.setFill(UiColors.accentLight());
        gc.setFont(displayFont);
        double textWidth = gc.getFont().getSize() * text.length() * 0.6;
        gc.fillText(text, x + (width - textWidth) / 2, y + 21);
    }

    @Override
    public void setPlaylist(Playlist playlist) {}
}