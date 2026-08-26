package de.emgeem.daplayer.ui.control;

import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class MenuButton extends Canvas {

    public enum Shape {
        LEFT, CENTER, RIGHT
    }

    private static final double HEIGHT = 40;
    private static final double SLOPE = 10;
    private static final double INSET = 3;

    private static final Font BUTTON_FONT = Font.loadFont(MenuButton.class.getResourceAsStream("/fonts/SairaStencil_Condensed-Black.ttf"), 18);

    private final String text;
    private final Shape shape;

    public MenuButton(String text, double width, Shape shape) {
        super(width, HEIGHT);
        this.text = text;
        this.shape = shape;
        draw();
    }

    private void draw() {

        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        gc.clearRect(0, 0, w, h);

        // AUSSENNUT
        double[] x;
        if (shape == Shape.LEFT) {
            x = new double[] {0, w, w, SLOPE};
        } else if (shape == Shape.RIGHT) {
            x = new double[] {0, w, w - SLOPE, 0};
        } else {
            x = new double[] {0, w, w, 0};
        }
        double[] y = {0, 0, h, h};
        gc.setFill(Color.rgb(4, 4, 4, 0.95));
        gc.fillPolygon(x, y, 4);

        // TASTERKÖRPER
        if (shape == Shape.LEFT) {
            x = new double[] {INSET, w - INSET, w - INSET, SLOPE + INSET};
        } else if (shape == Shape.RIGHT) {
            x = new double[] {INSET, w - INSET, w - SLOPE - INSET, INSET};
        } else {
            x = new double[] {INSET, w - INSET, w - INSET, INSET};
        }
        y = new double[] {INSET, INSET, h - INSET, h - INSET};

        LinearGradient surface = new LinearGradient(0, 0, 0, h, false, CycleMethod.NO_CYCLE, new Stop[] {
            new Stop(0.00, Color.rgb(58, 58, 58)),
            new Stop(0.20, Color.rgb(43, 43, 43)),
            new Stop(0.55, Color.rgb(29, 29, 29)),
            new Stop(1.00, Color.rgb(15, 15, 15))
        });
        gc.setFill(surface);
        gc.fillPolygon(x, y, 4);

        // OBERE LICHTKANTE
        gc.setStroke(Color.rgb(150, 150, 150, 0.25));
        gc.setLineWidth(1);
        gc.strokeLine(shape == Shape.LEFT ? SLOPE + INSET : INSET, INSET, shape == Shape.RIGHT ? w - SLOPE - INSET : w - INSET, INSET);

        // UNTERE SCHATTENKANTE
        gc.setStroke(Color.rgb(0, 0, 0, 0.9));
        gc.setLineWidth(2);
        gc.strokeLine(shape == Shape.RIGHT ? INSET : SLOPE + INSET, h - INSET, shape == Shape.LEFT ? w - INSET : w - SLOPE - INSET, h - INSET);

        // INNERE VERTIEFUNG
        gc.setStroke(Color.rgb(0, 0, 0, 0.45));
        gc.setLineWidth(1);
        gc.strokeLine(shape == Shape.LEFT ? SLOPE + 4 : 4, h - 5, shape == Shape.RIGHT ? w - SLOPE - 4 : w - 4, h - 5);

        // BESCHRIFTUNG
        gc.setFont(BUTTON_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFill(UiColors.accentLight());
        gc.fillText(text, w / 2, h / 2);
    }
}