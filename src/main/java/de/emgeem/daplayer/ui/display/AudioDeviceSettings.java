package de.emgeem.daplayer.ui.display;

import java.util.List;

import javax.sound.sampled.Mixer;

import de.emgeem.daplayer.audio.AudioDeviceManager;
import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class AudioDeviceSettings extends StackPane {

    private final Font displayFont;
    private final AudioDeviceManager audioDeviceManager;
    private final Canvas canvas;
    private boolean deviceListOpen;

    public AudioDeviceSettings(Font displayFont, AudioDeviceManager audioDeviceManager) {
        this.displayFont = displayFont;
        this.audioDeviceManager = audioDeviceManager;
        canvas = new Canvas();
        getChildren().add(canvas);
        widthProperty().addListener((obs, oldValue, newValue) -> draw());
        heightProperty().addListener((obs, oldValue, newValue) -> draw());
        setOnMouseClicked(e -> handleMouseClick(e.getX(), e.getY()));
    }

    public void draw() {
        double w = getWidth();
        double h = getHeight();
        canvas.setWidth(w);
        canvas.setHeight(h);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        gc.setFill(UiColors.accentLight());
        gc.setEffect(UiColors.displayGlow());
        gc.setFont(displayFont);
        gc.fillText("AUDIO DEVICE", 30, 45);
        gc.setEffect(null);
        drawDeviceField(gc);
        if (deviceListOpen) {drawDeviceList(gc);}
    }

    private void drawDeviceField(GraphicsContext gc) {
        double x = 30;
        double y = 65;
        double width = 700;
        double height = 30;
        gc.setFill(Color.rgb(25, 25, 25));
        gc.fillRoundRect(x, y, width, height, 6, 6);
        gc.setStroke(UiColors.accentLight());
        gc.setEffect(UiColors.displayGlow());
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, width, height, 6, 6);
        gc.setEffect(null);
        if (audioDeviceManager.getSelectedDevice() != null) {
            gc.setFill(UiColors.accentLight());
            gc.setFont(displayFont);
            gc.fillText(audioDeviceManager.getSelectedDevice().getName(), x + 10, y + 21);
        }
        // Pfeil
        gc.setFill(UiColors.accentLight());
        double arrowX = x + width - 20;
        double arrowY = y + 11;
        gc.fillPolygon(new double[] {arrowX - 5, arrowX + 5, arrowX}, new double[] {arrowY - 3, arrowY - 3, arrowY + 4}, 3);
    }

    private void handleMouseClick(double mouseX, double mouseY) {
        double fieldX = 30;
        double fieldY = 65;
        double fieldWidth = 700;
        double fieldHeight = 30;
        if (!deviceListOpen && mouseX >= fieldX && mouseX <= fieldX + fieldWidth && mouseY >= fieldY && mouseY <= fieldY + fieldHeight) {
            deviceListOpen = true;
            draw();
            return;
        }
        if (!deviceListOpen) {return;}
        List<Mixer.Info> devices = audioDeviceManager.getDevices();
        double listY = fieldY + fieldHeight;
        double itemHeight = 30;
        for (int i = 0; i < devices.size(); i++) {
            double itemY = listY + i * itemHeight;
            if (mouseX >= fieldX && mouseX <= fieldX + fieldWidth && mouseY >= itemY && mouseY <= itemY + itemHeight) {
                audioDeviceManager.setSelectedDevice(devices.get(i));
                audioDeviceManager.save();
                deviceListOpen = false;
                draw();
                return;
            }
        }
        deviceListOpen = false;
        draw();
    }

    private void drawDeviceList(GraphicsContext gc) {
        List<Mixer.Info> devices = audioDeviceManager.getDevices();
        double x = 30;
        double y = 95;
        double width = 700;
        double itemHeight = 30;
        for (int i = 0; i < devices.size(); i++) {
            Mixer.Info device = devices.get(i);
            double itemY = y + i * itemHeight;
            if (device.equals(audioDeviceManager.getSelectedDevice())) {gc.setFill(UiColors.accentLight().deriveColor(0, 1, 1, 0.15));} 
            else {gc.setFill(Color.rgb(20, 20, 20));}
            gc.fillRect(x, itemY, width, itemHeight);
            gc.setStroke(Color.rgb(70, 70, 70, 0.5));
            gc.strokeRect(x, itemY, width, itemHeight);
            gc.setFill(UiColors.accentLight());
            gc.setFont(displayFont);
            gc.fillText(device.getName(), x + 10, itemY + 21);
        }
    }
}