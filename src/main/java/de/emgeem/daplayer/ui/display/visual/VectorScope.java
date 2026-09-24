package de.emgeem.daplayer.ui.display.visual;

import de.emgeem.daplayer.audio.VisualizationData;
import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class VectorScope {

    private final GraphicsContext gc;

    private double width;
    private double height;

    private float[] leftSamples;
    private float[] rightSamples;

    public VectorScope(GraphicsContext gc) {this.gc = gc;}

    public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public void update(VisualizationData data) {
        leftSamples = data.getLeftSamples();
        rightSamples = data.getRightSamples();
    }

    public void draw() {
        if (width <= 0 || height <= 0) {return;}
        if (leftSamples == null || rightSamples == null) {return;}
        int sampleCount = Math.min(leftSamples.length, rightSamples.length);
        if (sampleCount == 0) {return;}
        double centerX = width / 2.0;
        double centerY = height / 2.0;
        double scale = Math.min(width, height) * 0.45;
        drawBackground(centerX, centerY, scale);
        drawTrace(centerX, centerY, scale, sampleCount);
    }

    private void drawBackground(double centerX, double centerY, double scale) {
        gc.setStroke(UiColors.getAccent());
        gc.setLineWidth(1.0);
        gc.setGlobalAlpha(0.18);
        // Horizontal
        gc.strokeLine(centerX - scale, centerY, centerX + scale, centerY);
        // Vertikal
        gc.strokeLine(centerX, centerY - scale, centerX, centerY + scale);
        // Diagonale
        gc.strokeLine(centerX - scale, centerY - scale, centerX + scale, centerY + scale);
        gc.strokeLine(centerX - scale, centerY + scale, centerX + scale, centerY - scale);
        // Begrenzung
        gc.strokeRect(centerX - scale, centerY - scale, scale * 2, scale * 2);
        gc.setGlobalAlpha(1.0);
    }

    private void drawTrace(double centerX, double centerY, double scale, int sampleCount) {
        gc.setGlobalAlpha(0.08);
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, width, height);
        gc.setFill(UiColors.getAccent());
        gc.setGlobalAlpha(0.35);
        int step = Math.max(1, sampleCount / 600);
        for (int i = 0; i < sampleCount; i += step) {
            double left = leftSamples[i];
            double right = rightSamples[i];
            double mid = (left + right) * 0.5;
            double side = (left - right) * 0.5;
            double x = centerX + mid * scale;
            double y = centerY - side * scale;
            gc.fillOval(x - 1, y - 1, 2, 2);
        }
        gc.setGlobalAlpha(1.0);
    }
}