package de.emgeem.daplayer.ui.display;

import de.emgeem.daplayer.audio.Equalizer;
import de.emgeem.daplayer.audio.EqualizerSettings;
import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class EqualizerDisplay extends DisplayMode {

    private static final double MIN_DB = -12.0;
    private static final double MAX_DB = 12.0;

    private final Canvas canvas;
    private final Font displayFont;
    private final Equalizer equalizer;

    private int activeBand = -1;
    private boolean resetPressed;

    public EqualizerDisplay(Font displayFont, Equalizer equalizer) {
        this.displayFont = displayFont;
        this.equalizer = equalizer;

        canvas = new Canvas();
        getChildren().add(canvas);

        canvas.setOnMousePressed(e -> {
            if (isOnResetButton(e.getX(), e.getY())) {
                resetPressed = true;
                draw();
                return;
            }
            if (e.getClickCount() == 2) {
                // Doppelklick setzt das Band auf 0 dB zurück
                int band = bandAt(e.getX());
                if (band >= 0) {
                    equalizer.setBandGain(band, 0);
                    draw();
                    return;
                }
            }
            updateFromMouse(e.getX(), e.getY());
        });
        canvas.setOnMouseDragged(e -> {if (!resetPressed) {updateFromMouse(e.getX(), e.getY());}});
        canvas.setOnMouseReleased(e -> {
            if (resetPressed) {
                resetPressed = false;
                if (isOnResetButton(e.getX(), e.getY())) {equalizer.reset();}
                draw();
            }
            activeBand = -1;
            EqualizerSettings.save(equalizer);
            draw();
        });

        widthProperty().addListener((obs, oldValue, newValue) -> draw());
        heightProperty().addListener((obs, oldValue, newValue) -> draw());
    }

    @Override
    public void draw() {
        double w = getWidth();
        double h = getHeight();
        canvas.setWidth(w);
        canvas.setHeight(h);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        int bandCount = equalizer.getBandCount();
        if (bandCount == 0) {return;}
        double left = 90;
        double right = 90;
        double top = 70;
        double bottom = 80;
        double availableWidth = w - left - right;
        double spacing = availableWidth / bandCount;
        double centerY = top + (h - top - bottom) / 2.0;
        double sliderHeight = h - top - bottom;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(displayFont);
        for (int i = 0; i < bandCount; i++) {
            double x = left + spacing * i + spacing / 2.0;
            double gain = equalizer.getBandGain(i);
// HINTERGRUND
            gc.setStroke(Color.rgb(55, 55, 55));
            gc.setLineWidth(5);
            gc.strokeLine(x, centerY - sliderHeight / 2, x, centerY + sliderHeight / 2);
// 0-DB-LINIE
            gc.setStroke(UiColors.accentLight());
            gc.setLineWidth(1);
            gc.strokeLine(x - 12, centerY, x + 12, centerY);
// REGLERPOSITION
            double normalized = (gain - MIN_DB) / (MAX_DB - MIN_DB);
            double knobY = centerY + sliderHeight / 2 - normalized * sliderHeight;
// REGLER
            gc.setFill(UiColors.accentLight());
            gc.setEffect(UiColors.displayGlow());
            gc.fillRoundRect(x - 9, knobY - 5, 18, 10, 4, 4);
            gc.setEffect(null);
// FREQUENZ
            gc.setFill(UiColors.accentLight());
            gc.setEffect(UiColors.displayGlow());
            String frequency = formatFrequency(equalizer.getFrequency(i)) + "Hz";
            gc.fillText(frequency, x, h - 42);
// GAIN
            String gainText = gain > 0 ? "+" + Math.round(gain) + " dB" : Math.round(gain) + " dB";
            gc.fillText(gainText, x, top - 20);
            gc.setEffect(null);
        }
        gc.setTextAlign(TextAlignment.LEFT);
        double bx = w - RESET_WIDTH - 30;
        gc.setFill(resetPressed ? UiColors.accentLight() : UiColors.accentLight().deriveColor(0, 1, 1, 0.08));
        gc.fillRoundRect(bx, RESET_Y, RESET_WIDTH, RESET_HEIGHT, 6, 6);
        gc.setStroke(UiColors.accentLight());
        gc.setLineWidth(1);
        gc.strokeRoundRect(bx, RESET_Y, RESET_WIDTH, RESET_HEIGHT, 6, 6);
        gc.setFill(resetPressed ? Color.BLACK : UiColors.accentLight());
        gc.fillText("RESET", bx + (RESET_WIDTH - displayFont.getSize() * 5 * 0.6) / 2, RESET_Y + 19);
    }

    private static final double RESET_WIDTH = 90;
    private static final double RESET_HEIGHT = 26;
    private static final double RESET_Y = 6;

    private boolean isOnResetButton(double x, double y) {
        double bx = getWidth() - RESET_WIDTH - 30;
        return x >= bx && x <= bx + RESET_WIDTH && y >= RESET_Y && y <= RESET_Y + RESET_HEIGHT;
    }

    private int bandAt(double mouseX) {
        int bandCount = equalizer.getBandCount();
        double spacing = (getWidth() - 180) / bandCount;
        int band = (int) Math.floor((mouseX - 90) / spacing);
        return band >= 0 && band < bandCount ? band : -1;
    }

    private void updateFromMouse(double mouseX, double mouseY) {
        double w = getWidth();
        double h = getHeight();
        int bandCount = equalizer.getBandCount();
        double left = 90;
        double right = 90;
        double top = 70;
        double bottom = 80;
        double availableWidth = w - left - right;
        double spacing = availableWidth / bandCount;
        double centerY = top + (h - top - bottom) / 2.0;
        double sliderHeight = h - top - bottom;
        int band = activeBand >= 0 ? activeBand : (int) Math.floor((mouseX - left) / spacing);
        if (band < 0 || band >= bandCount) {return;}
        double minY = centerY - sliderHeight / 2.0;
        double maxY = centerY + sliderHeight / 2.0;
        double y = Math.max(minY, Math.min(maxY, mouseY));
        double normalized = (maxY - y) / sliderHeight;
        double gain = MIN_DB + normalized * (MAX_DB - MIN_DB);
        gain = Math.round(gain);
        equalizer.setBandGain(band, gain);
        activeBand = band;
        draw();
    }

    private String formatFrequency(double frequency) {
        if (frequency >= 1000) {
            double value = frequency / 1000.0;
            if (value == Math.floor(value)) {return String.format("%.0fK", value);}
            return String.format("%.1fK", value);
        }
        return String.format("%.0f", frequency);
    }

    @Override
    public void setPlaylist(
        de.emgeem.daplayer.model.Playlist playlist) {
        // EQ benötigt keine Playlist.
    }
}