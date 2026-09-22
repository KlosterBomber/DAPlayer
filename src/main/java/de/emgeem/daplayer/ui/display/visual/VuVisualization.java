package de.emgeem.daplayer.ui.display.visual;

import de.emgeem.daplayer.audio.VisualizationData;
import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.GraphicsContext;

public class VuVisualization {

    private final GraphicsContext gc;

    private double width;
    private double height;

    private float[] samples;

    private double level;
    private double peak;

    private static final int SEGMENTS = 48;

    public VuVisualization(GraphicsContext gc) {
        this.gc = gc;
    }

    public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public void update(VisualizationData data) {
        samples = data.getSamples();
    }

    public void draw() {

        if (samples == null || samples.length == 0) {
            return;
        }

        double sum = 0.0;

        for (float sample : samples) {
            sum += sample * sample;
        }

        double rms = Math.sqrt(sum / samples.length);

        double targetLevel = Math.min(1.0, rms * 2.0);

        level += (targetLevel - level) * 0.25;

        if (targetLevel > peak) {
            peak = targetLevel;
        } else {
            peak -= 0.01;

            if (peak < 0) {
                peak = 0;
            }
        }

        double segmentWidth = width / SEGMENTS;
        double segmentHeight = 12;
        double gap = 4;

        double totalHeight = segmentHeight * 3 + gap * 2;
        double startY = (height - totalHeight) / 2.0;

        for (int row = 0; row < 3; row++) {

            double y = startY + row * (segmentHeight + gap);

            for (int i = 0; i < SEGMENTS; i++) {

                double position =
                        (double) i / (SEGMENTS - 1);

                boolean active =
                        position <= level;

                gc.setFill(
                        active
                                ? UiColors.accentLight()
                                : UiColors.accentDark()
                );

                gc.fillRoundRect(
                        i * segmentWidth,
                        y,
                        segmentWidth - 3,
                        segmentHeight,
                        3,
                        3
                );
            }
        }

        // Peak-Marker
        double peakX = peak * width;

        gc.setStroke(UiColors.accentLight());
        gc.setLineWidth(2);

        gc.strokeLine(
                peakX,
                startY - 8,
                peakX,
                startY + totalHeight + 8
        );
    }
}