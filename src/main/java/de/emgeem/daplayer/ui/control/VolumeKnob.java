package de.emgeem.daplayer.ui.control;

import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

public class VolumeKnob extends Canvas {

    private static final double OUTER = 160;
    private static final double RING = 146;
    private static final double INNER = 122;

    private double knobAngle = 0;

    public VolumeKnob() {
        super(OUTER + 8, OUTER + 8);
        draw();
    }


    private void draw() {

        GraphicsContext gc = getGraphicsContext2D();

        double cx = getWidth() / 2.0;
        double cy = getHeight() / 2.0;

        gc.clearRect(0, 0, getWidth(), getHeight());

        // ========================================================
        // AUSSENSCHATTEN
        // ========================================================
        gc.setFill(Color.rgb(10, 10, 10, 0.45));
        gc.fillOval(cx - OUTER / 2 + 4, cy - OUTER / 2 + 4, OUTER, OUTER);

        // ========================================================
        // AUSSENRING
        // ========================================================
        RadialGradient outerGradient = new RadialGradient(0, 0, cx, cy, OUTER / 2, false, CycleMethod.NO_CYCLE, new Stop[] {
            new Stop(0.0, Color.rgb(78, 78, 78)),
            new Stop(0.65, Color.rgb(46, 46, 46)),
            new Stop(1.0, Color.rgb(18, 18, 18))
        });
        gc.setFill(outerGradient);
        gc.fillOval(cx - OUTER / 2, cy - OUTER / 2, OUTER, OUTER);

        // ========================================================
        // RÄNDELUNG
        // ========================================================
        gc.setLineWidth(1.2);
        double r1 = OUTER / 2 - 3;
        double r2 = OUTER / 2 - 2;
        for (int i = 0; i < 72; i++) {
            double angle = Math.toRadians(i * 7.5 + knobAngle);
            double x1 = cx + Math.cos(angle) * r1;
            double y1 = cy + Math.sin(angle) * r1;
            double x2 = cx + Math.cos(angle) * r2;
            double y2 = cy + Math.sin(angle) * r2;
            gc.setStroke(Color.rgb(18, 18, 18));
            gc.strokeLine(x1, y1, x2, y2);
            double angle2 = angle + Math.toRadians(0.6);
            double hx1 = cx + Math.cos(angle2) * r1;
            double hy1 = cy + Math.sin(angle2) * r1;
            double hx2 = cx + Math.cos(angle2) * r2;
            double hy2 = cy + Math.sin(angle2) * r2;
            gc.setStroke(Color.rgb(180, 180, 180, 0.18));
            gc.strokeLine(hx1, hy1, hx2, hy2);
        }

        // ========================================================
        // ALURING
        // ========================================================
        LinearGradient ringGradient = new LinearGradient(0, cy - RING / 2, 0, cy + RING / 2, false, CycleMethod.NO_CYCLE, new Stop[] {
            new Stop(0.0, Color.rgb(120, 120, 120)),
            new Stop(0.5, Color.rgb(68, 68, 68)),
            new Stop(1.0, Color.rgb(32, 32, 32))
        });
        gc.setFill(ringGradient);
        gc.fillOval(cx - RING / 2, cy - RING / 2, RING, RING);

        // ========================================================
        // INNENKAPPE
        // ========================================================
        RadialGradient capGradient = new RadialGradient(0, 0, cx, cy, INNER / 2, false, CycleMethod.NO_CYCLE, new Stop[] {
            new Stop(0.0, Color.rgb(52, 52, 52)),
            new Stop(0.35, Color.rgb(36, 36, 36)),
            new Stop(1.0, Color.rgb(14, 14, 14))
        });
        gc.setFill(capGradient);
        gc.fillOval(cx - INNER / 2, cy - INNER / 2, INNER, INNER);

        // ========================================================
        // LICHTKANTE
        // ========================================================
        gc.setStroke(Color.rgb(255, 255, 255, 0.18));
        gc.setLineWidth(1.2);
        gc.strokeOval(cx - INNER / 2 + 1, cy - INNER / 2 + 1, INNER - 2, INNER - 2);

        // ========================================================
        // SCHATTENKANTE
        // ========================================================
        RadialGradient innerShadow = new RadialGradient(0, 0, cx, cy, INNER / 2, false, CycleMethod.NO_CYCLE, new Stop[] {
            new Stop(0.82, Color.TRANSPARENT),
            new Stop(0.94, Color.rgb(68, 68, 68, 0.18)),
            new Stop(1.00, Color.rgb(40, 40, 40, 0.55))
        });
        gc.setFill(innerShadow);
        gc.fillOval(cx - INNER / 2, cy - INNER / 2, INNER, INNER);

        // ========================================================
        // MARKER
        // ========================================================
        double angle = Math.toRadians(knobAngle);
        double r = (RING + INNER) / 4.0;
        double px = cx + Math.cos(angle) * r;
        double py = cy + Math.sin(angle) * r;
        Color markerColor = UiColors.getAccent();
        gc.setFill(Color.rgb(18, 18, 18));
        gc.fillOval(px - 4, py - 4, 8, 8);

        // Marker-Glow
        RadialGradient glow = new RadialGradient(0, 0, px, py, 12, false, CycleMethod.NO_CYCLE, new Stop[] {
            new Stop(0.0, markerColor.deriveColor(0, 1.0, 1.0, 0.28)),
            new Stop(1.0, Color.TRANSPARENT)
        });

        gc.setFill(glow);
        gc.fillOval(px - 12, py - 12, 24, 24);

        // Marker
        gc.setFill(markerColor);
        gc.fillOval(px - 2.5, py - 2.5, 5, 5);

        // kleiner Lichtpunkt
        gc.setFill(Color.rgb(255, 255, 255, 0.65));
        gc.fillOval(px - 1.2, py - 1.2, 1.5, 1.5);
    }
}