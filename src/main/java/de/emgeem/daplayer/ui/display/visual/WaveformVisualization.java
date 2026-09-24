package de.emgeem.daplayer.ui.display.visual;

import de.emgeem.daplayer.audio.VisualizationData;
import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.GraphicsContext;

public class WaveformVisualization {

    private final GraphicsContext gc;

    private double width;
    private double height;

    private float[] samples;

    public WaveformVisualization(GraphicsContext gc) {this.gc = gc;}

    public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public void update(VisualizationData data) {samples = data.getSamples();}

    public void draw() {
        if (samples == null || samples.length == 0) {return;}
        double centerY = height / 2.0;
        double amplitude = height * 0.45;
        if (UiColors.isRgbVisualization()) {gc.setStroke(UiColors.accentRGB());} else {gc.setStroke(UiColors.getAccent());}
        gc.setLineWidth(1.0);
        gc.beginPath();
        for (int x = 0; x < width; x++) {
            int index = (int) ((double) x / width * samples.length);
            if (index >= samples.length) {index = samples.length - 1;}
            double sample = samples[index];
            double y = centerY - sample * amplitude;
            if (x == 0) {gc.moveTo(x, y);} 
            else {gc.lineTo(x, y);}
        }
        gc.stroke();
    }
}