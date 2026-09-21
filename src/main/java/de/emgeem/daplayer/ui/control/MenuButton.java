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

    public enum Shape {LEFT, CENTER, RIGHT, BOTH}

    private static final double SLOPE = 10;
    private static final double INSET = 3;

    private static final Font BUTTON_FONT = Font.loadFont(MenuButton.class.getResourceAsStream("/fonts/SairaStencil_Condensed-Black.ttf"), 18);

    private final String text;
    private final Shape shape;

    private boolean active;

    public MenuButton(String text, double width, double height, Shape shape) {
        super(width, height);
        this.text = text;
        this.shape = shape;
        hoverProperty().addListener((obs, oldValue, newValue) -> draw());
        draw();
    }

    public void setActive(boolean active) {this.active = active; draw();}

    public void draw() {

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
        } else if (shape == Shape.BOTH) {
            x = new double[] {0, w, w - SLOPE, SLOPE};
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
        } else if (shape == Shape.BOTH) {
            x = new double[] {INSET, w - INSET, w - SLOPE - INSET, SLOPE + INSET};
        }else {
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
        gc.strokeLine(
            shape == Shape.LEFT  || shape == Shape.BOTH ? SLOPE + INSET : INSET, INSET, 
            shape == Shape.RIGHT  || shape == Shape.BOTH ? w - SLOPE - INSET : w - INSET, INSET
        );

        // UNTERE SCHATTENKANTE
        gc.setStroke(Color.rgb(0, 0, 0, 0.9));
        gc.setLineWidth(2);
        gc.strokeLine(
            shape == Shape.RIGHT  || shape == Shape.BOTH ? INSET : SLOPE + INSET, h - INSET, 
            shape == Shape.LEFT  || shape == Shape.BOTH ? w - INSET : w - SLOPE - INSET, h - INSET
        );

        // INNERE VERTIEFUNG
        gc.setStroke(Color.rgb(0, 0, 0, 0.45));
        gc.setLineWidth(1);
        gc.strokeLine(
            shape == Shape.LEFT  || shape == Shape.BOTH ? SLOPE + 4 : 4, h - 5, 
            shape == Shape.RIGHT  || shape == Shape.BOTH ? w - SLOPE - 4 : w - 4, h - 5
        );

        // BESCHRIFTUNG
        gc.setFont(BUTTON_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        if (isHover() || active) {
            gc.setEffect(UiColors.displayGlow());
            gc.setFill(UiColors.accentLight());
        } else {
            gc.setEffect(null);
            gc.setFill(UiColors.getAccent());
        }
        gc.fillText(text, w / 2, h / 2 - 3);
        gc.setEffect(null);

        // AKTIVITÄTSANZEIGE
        if (active) {
            gc.setEffect(UiColors.displayGlow());
            gc.setStroke(UiColors.accentLight());
        } else {
            gc.setEffect(null);   
            gc.setStroke(Color.rgb(35, 35, 35));
        }
        gc.setLineWidth(4);
        gc.strokeLine(w / 2 - 20, h / 2 + 10, w / 2 + 20, h / 2 + 10);
        gc.setEffect(null);
    }



}