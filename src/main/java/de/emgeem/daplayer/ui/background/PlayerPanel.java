package de.emgeem.daplayer.ui.background;

import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

public class PlayerPanel extends Canvas {

    private static final double LED_WIDTH = 5;

    public PlayerPanel(double width, double height) {
        super(width, height);
        draw();
    }

    public void draw() {

        GraphicsContext gc = getGraphicsContext2D();

        double width = getWidth();
        double height = getHeight();

        gc.clearRect(0, 0, width, height);

        Color accent = UiColors.getAccent();

        // ========================================================
        // AUSSERE VERTIEFUNG
        // ========================================================
        gc.setStroke(Color.rgb(5, 5, 5, 0.75));
        gc.setLineWidth(6);
        gc.strokeRoundRect(3, 3, width - 6, height - 6, 24, 24);

        // ========================================================
        // ALUMINIUM-AUSSENRAHMEN
        // ========================================================
        LinearGradient aluGradient = new LinearGradient(0, 0, 0, height, false, CycleMethod.NO_CYCLE, new Stop[] {
            new Stop(0.0, Color.rgb(105, 105, 105)),
            new Stop(0.12, Color.rgb(62, 62, 62)),
            new Stop(0.50, Color.rgb(32, 32, 32)),
            new Stop(0.88, Color.rgb(62, 62, 62)),
            new Stop(1.0, Color.rgb(105, 105, 105))
        });
        gc.setStroke(aluGradient);
        gc.setLineWidth(6);
        gc.strokeRoundRect(7, 7, width - 14, height - 14, 20, 20);

        // ========================================================
        // INNERE VERTIEFUNG
        // ========================================================
        gc.setStroke(Color.rgb(0, 0, 0, 0.85));
        gc.setLineWidth(5);
        gc.strokeRoundRect(11, 11, width - 22, height - 22, 15, 15);

        // ========================================================
        // LED-NUT
        // ========================================================
        gc.setStroke(Color.rgb(0, 0, 0, 0.8));
        gc.setLineWidth(9);
        gc.strokeRoundRect(16, 16, width - 32, height - 32, 13, 13);

        // ========================================================
        // LED
        // ========================================================
        gc.setStroke(accent);
        gc.setLineWidth(LED_WIDTH);
        gc.strokeRoundRect(18, 18, width - 36, height - 36, 11, 11);

        // ========================================================
        // LED-LICHTKANTE
        // ========================================================
        gc.setStroke(accent.deriveColor(0, 1.0, 1.35, 0.55));
        gc.setLineWidth(1);
        gc.strokeRoundRect(20, 20, width - 40, height - 40, 9, 9);

        // ========================================================
        // UNTERE VERTIEFUNG DES CONTROL-BEREICHS
        // ========================================================
        // gc.setStroke(Color.rgb(0, 0, 0, 0.8));
        // gc.setLineWidth(3);
        // gc.strokeLine(20, height * 0.62, width - 20, height * 0.62);
    }
}