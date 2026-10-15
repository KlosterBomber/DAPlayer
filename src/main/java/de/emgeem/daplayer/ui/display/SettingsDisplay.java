package de.emgeem.daplayer.ui.display;

import de.emgeem.daplayer.audio.AudioDeviceManager;
import de.emgeem.daplayer.model.Playlist;
import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class SettingsDisplay extends DisplayMode {

    private static final String[] BUTTON_TEXT = {"AUDIO DEVICE", "ACCENT COLORS", "KEY CONTROLS"};
    private static final double[] BUTTON_X = {30, 190, 350};
    private static final double BUTTON_Y = 350;
    private static final double BUTTON_WIDTH = 150;
    private static final double BUTTON_HEIGHT = 30;

    private final Font displayFont;
    private final Canvas canvas;
    private final AudioDeviceSettings audioDeviceSettings;
    private final AccentColorSettings accentColorSettings;
    private final KeyControlsSettings keyControlsSettings;

    public SettingsDisplay(Font displayFont, Font displayFontSmall, AudioDeviceManager audioDeviceManager, Runnable redrawAction) {
        this.displayFont = displayFont;
        canvas = new Canvas();
        widthProperty().addListener((obs, oldValue, newValue) -> draw());
        heightProperty().addListener((obs, oldValue, newValue) -> draw());
        audioDeviceSettings = new AudioDeviceSettings(displayFont, audioDeviceManager);
        audioDeviceSettings.prefWidthProperty().bind(widthProperty());
        audioDeviceSettings.prefHeightProperty().bind(heightProperty());
        accentColorSettings = new AccentColorSettings(displayFont, displayFontSmall, redrawAction);
        keyControlsSettings = new KeyControlsSettings(displayFont, displayFontSmall);
        getChildren().addAll(canvas, audioDeviceSettings, accentColorSettings, keyControlsSettings);
        showPage(0);
        setOnMouseClicked(e -> {
            double x = e.getX();
            double y = e.getY();
            if (y < BUTTON_Y || y > BUTTON_Y + BUTTON_HEIGHT) {return;}
            for (int i = 0; i < BUTTON_X.length; i++) {
                if (x >= BUTTON_X[i] && x <= BUTTON_X[i] + BUTTON_WIDTH) {
                    showPage(i);
                    draw();
                    return;
                }
            }
        });
    }

    /** Wird nach einem Wechsel des Audiogeräts aufgerufen. */
    public void setDeviceChangedAction(Runnable action) {audioDeviceSettings.setDeviceChangedAction(action);}

    private void showPage(int page) {
        audioDeviceSettings.setVisible(page == 0);
        accentColorSettings.setVisible(page == 1);
        keyControlsSettings.setVisible(page == 2);
    }

    private int activePage() {
        if (audioDeviceSettings.isVisible()) {return 0;}
        if (accentColorSettings.isVisible()) {return 1;}
        if (keyControlsSettings.isVisible()) {return 2;}
        return -1;
    }

    @Override
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
        int active = activePage();
        for (int i = 0; i < BUTTON_TEXT.length; i++) {
            drawButton(gc, BUTTON_X[i], BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_TEXT[i], i == active);
        }
        gc.setEffect(null);
        if (audioDeviceSettings.isVisible()) {audioDeviceSettings.draw();}
        if (accentColorSettings.isVisible()) {accentColorSettings.draw();}
        if (keyControlsSettings.isVisible()) {keyControlsSettings.draw();}
    }

    static void drawSubmenuButton(GraphicsContext gc, Font font, double x, double y, double width, double height, String text, boolean active) {
        gc.setFill(active ? UiColors.accentLight() : UiColors.accentLight().deriveColor(0, 1, 1, 0.08));
        gc.fillRoundRect(x, y, width, height, 6, 6);
        gc.setStroke(UiColors.accentLight());
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, width, height, 6, 6);
        gc.setFill(active ? Color.BLACK : UiColors.accentLight());
        gc.setFont(font);
        double textWidth = font.getSize() * text.length() * 0.6;
        gc.fillText(text, x + (width - textWidth) / 2, y + 21);
    }

    private void drawButton(GraphicsContext gc, double x, double y, double width, double height, String text, boolean active) {
        drawSubmenuButton(gc, displayFont, x, y, width, height, text, active);
    }

    @Override
    public void setPlaylist(Playlist playlist) {}
}
