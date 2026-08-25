package de.emgeem.daplayer.ui.background;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

public class PlayerViewBackground extends Canvas {

    public PlayerViewBackground(double width, double height) {
        super(width, height);
        widthProperty().addListener((obs, oldValue, newValue) -> draw());
        heightProperty().addListener((obs, oldValue, newValue) -> draw());
        draw();
    }

    private void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();
        gc.clearRect(0, 0, w, h);
        drawBackground(gc, w, h);
        drawPanel(gc, w, h);
    }


    private void drawBackground(GraphicsContext gc, double w, double h) {
        gc.setFill(Color.TRANSPARENT);
        gc.fillRect(0, 0, w, h);
    }

    private void drawPanel(GraphicsContext gc, double w, double h) {
        double x = 0;
        double y = 0;
        double pw = w;
        double ph = h;
        LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop[]{
            new Stop(0.0, Color.rgb(52, 52, 52)),
            new Stop(0.05, Color.rgb(38, 38, 38)),
            new Stop(0.50, Color.rgb(26, 26, 26)),
            new Stop(0.95, Color.rgb(19, 19, 19)),
            new Stop(1.0, Color.rgb(10, 10, 10))
        });
        gc.setFill(gradient);
        gc.fillRoundRect(x, y, pw, ph, 12, 12);

        // äußerer Rahmen
        gc.setStroke(Color.rgb(120, 120, 120, 0.35));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x + 2, y + 2, pw - 4, ph - 4, 10, 10);

        // innerer Schattenrahmen
        gc.setStroke(Color.rgb(0, 0, 0, 0.55));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x + 6, y + 6, pw - 12, ph - 12, 8, 8);

        // oberer Lichtreflex
        LinearGradient highlight = new LinearGradient(0, y, 0, y + 70, false, CycleMethod.NO_CYCLE, new Stop[]{
            new Stop(0, Color.rgb(255, 255, 255, 0.10)),
            new Stop(1, Color.TRANSPARENT)
        });

        gc.setFill(highlight);
        gc.fillRoundRect(x + 4, y + 4, pw - 8, 60, 10, 10);

        // gebürstetes Metall
        drawBrushedMetal(gc, x, y, pw, ph);

        // abschließende Rahmen
        gc.setStroke(Color.rgb(90, 90, 90));
        gc.setLineWidth(1);
        gc.strokeRoundRect(x + 1, y + 1, pw - 2, ph - 2, 12, 12);
        gc.setStroke(Color.rgb(12, 12, 12));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x + 4, y + 4, pw - 8, ph - 8, 10, 10);
    }

    private void drawBrushedMetal(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setLineWidth(1);
        for (int i = 0; i < h; i++) {
            double alpha = 0.015 + ((i * 37) % 100) / 100.0 * 0.03;
            gc.setStroke(Color.color(1, 1, 1, alpha));
            gc.strokeLine(x + 12, y + i, x + w - 12, y + i);
        }
    }
}