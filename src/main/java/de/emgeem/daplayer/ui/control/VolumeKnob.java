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

    // ============================================================
    // GEOMETRIE
    // ============================================================

    private static final double OUTER = 200;

    // LED-RING
    private static final double LED_OUTER = 184;
    private static final double LED_INNER = 160;

    // ALU-RING
    private static final double ALU_OUTER = 160;
    private static final double ALU_INNER = 148;

    // KAPPE
    private static final double INNER = 140;

    private double knobAngle = 0;

    public VolumeKnob() {
        super(OUTER + 12, OUTER + 12);
        draw();
    }

    private void draw() {

        GraphicsContext gc = getGraphicsContext2D();

        double cx = getWidth() / 2.0;
        double cy = getHeight() / 2.0;

        gc.clearRect(0, 0, getWidth(), getHeight());

        Color ledColor = UiColors.getAccent();

        // ========================================================
        // 1. ÄUSSERE VERTIEFUNG
        // ========================================================

        RadialGradient recessGradient = new RadialGradient(
                0,
                0,
                cx,
                cy,
                OUTER / 2,
                false,
                CycleMethod.NO_CYCLE,
                new Stop[] {
                        new Stop(0.00, Color.rgb(18, 18, 18)),
                        new Stop(0.78, Color.rgb(11, 11, 11)),
                        new Stop(1.00, Color.rgb(3, 3, 3))
                }
        );

        gc.setFill(recessGradient);
        gc.fillOval(
                cx - OUTER / 2,
                cy - OUTER / 2,
                OUTER,
                OUTER
        );

        // obere Lichtkante
        gc.setStroke(Color.rgb(110, 110, 110, 0.20));
        gc.setLineWidth(1.2);
        gc.strokeOval(
                cx - OUTER / 2 + 1,
                cy - OUTER / 2 + 1,
                OUTER - 2,
                OUTER - 2
        );

        // untere Schattenkante
        gc.setStroke(Color.rgb(0, 0, 0, 0.80));
        gc.setLineWidth(2);
        gc.strokeOval(
                cx - OUTER / 2 + 3,
                cy - OUTER / 2 + 3,
                OUTER - 6,
                OUTER - 6
        );

        // ========================================================
        // 2. LED-GLOW
        // ========================================================

        // breiter, weicher Außen-Glow
        RadialGradient outerGlow = new RadialGradient(
                0,
                0,
                cx,
                cy,
                LED_OUTER / 2 + 18,
                false,
                CycleMethod.NO_CYCLE,
                new Stop[] {
                        new Stop(
                                0.72,
                                Color.TRANSPARENT
                        ),
                        new Stop(
                                0.82,
                                ledColor.deriveColor(
                                        0, 1.0, 1.0, 0.08
                                )
                        ),
                        new Stop(
                                0.90,
                                ledColor.deriveColor(
                                        0, 1.0, 1.0, 0.20
                                )
                        ),
                        new Stop(
                                0.96,
                                ledColor.deriveColor(
                                        0, 1.0, 1.0, 0.32
                                )
                        ),
                        new Stop(
                                1.00,
                                Color.TRANSPARENT
                        )
                }
        );

        gc.setFill(outerGlow);
        gc.fillOval(
                cx - LED_OUTER / 2 - 18,
                cy - LED_OUTER / 2 - 18,
                LED_OUTER + 36,
                LED_OUTER + 36
        );

        // enger, stärkerer Glow direkt am LED-Ring
        RadialGradient innerGlow = new RadialGradient(
                0,
                0,
                cx,
                cy,
                LED_OUTER / 2 + 6,
                false,
                CycleMethod.NO_CYCLE,
                new Stop[] {
                        new Stop(
                                0.78,
                                Color.TRANSPARENT
                        ),
                        new Stop(
                                0.88,
                                ledColor.deriveColor(
                                        0, 1.0, 1.05, 0.18
                                )
                        ),
                        new Stop(
                                0.96,
                                ledColor.deriveColor(
                                        0, 1.0, 1.10, 0.48
                                )
                        ),
                        new Stop(
                                1.00,
                                Color.TRANSPARENT
                        )
                }
        );

        gc.setFill(innerGlow);
        gc.fillOval(
                cx - LED_OUTER / 2 - 6,
                cy - LED_OUTER / 2 - 6,
                LED_OUTER + 12,
                LED_OUTER + 12
        );

        // ========================================================
        // 3. LED-RING
        // ========================================================

        RadialGradient ledSurface = new RadialGradient(
                0,
                0,
                cx,
                cy,
                LED_OUTER / 2,
                false,
                CycleMethod.NO_CYCLE,
                new Stop[] {
                        new Stop(
                                0.00,
                                ledColor.deriveColor(
                                        0, 1.0, 1.25, 1.0
                                )
                        ),
                        new Stop(
                                0.55,
                                ledColor.deriveColor(
                                        0, 1.0, 1.15, 1.0
                                )
                        ),
                        new Stop(
                                0.88,
                                ledColor
                        ),
                        new Stop(
                                1.00,
                                ledColor.deriveColor(
                                        0, 1.0, 0.82, 1.0
                                )
                        )
                }
        );

        gc.setFill(ledSurface);
        gc.fillOval(
                cx - LED_OUTER / 2,
                cy - LED_OUTER / 2,
                LED_OUTER,
                LED_OUTER
        );

        // ========================================================
        // 4. INNERE AUSSPARUNG DES LED-RINGS
        // ========================================================

        gc.setFill(Color.rgb(14, 14, 14));
        gc.fillOval(
                cx - LED_INNER / 2,
                cy - LED_INNER / 2,
                LED_INNER,
                LED_INNER
        );

        // ========================================================
        // 5. ALUMINIUMRING
        // ========================================================

        LinearGradient aluGradient = new LinearGradient(
                0,
                cy - ALU_OUTER / 2,
                0,
                cy + ALU_OUTER / 2,
                false,
                CycleMethod.NO_CYCLE,
                new Stop[] {
                        new Stop(
                                0.00,
                                Color.rgb(155, 155, 155)
                        ),
                        new Stop(
                                0.18,
                                Color.rgb(115, 115, 115)
                        ),
                        new Stop(
                                0.50,
                                Color.rgb(76, 76, 76)
                        ),
                        new Stop(
                                0.82,
                                Color.rgb(48, 48, 48)
                        ),
                        new Stop(
                                1.00,
                                Color.rgb(28, 28, 28)
                        )
                }
        );

        gc.setFill(aluGradient);
        gc.fillOval(
                cx - ALU_OUTER / 2,
                cy - ALU_OUTER / 2,
                ALU_OUTER,
                ALU_OUTER
        );

        // ========================================================
        // 6. INNERE AUSSPARUNG DES ALURINGS
        // ========================================================

        gc.setFill(Color.rgb(13, 13, 13));
        gc.fillOval(
                cx - ALU_INNER / 2,
                cy - ALU_INNER / 2,
                ALU_INNER,
                ALU_INNER
        );

        // ========================================================
        // 7. MITTLERE KAPPE
        // ========================================================

        RadialGradient capGradient = new RadialGradient(
                0,
                0,
                cx,
                cy,
                INNER / 2,
                false,
                CycleMethod.NO_CYCLE,
                new Stop[] {
                        new Stop(
                                0.00,
                                Color.rgb(58, 58, 58)
                        ),
                        new Stop(
                                0.35,
                                Color.rgb(40, 40, 40)
                        ),
                        new Stop(
                                0.72,
                                Color.rgb(25, 25, 25)
                        ),
                        new Stop(
                                1.00,
                                Color.rgb(12, 12, 12)
                        )
                }
        );

        gc.setFill(capGradient);
        gc.fillOval(
                cx - INNER / 2,
                cy - INNER / 2,
                INNER,
                INNER
        );

        // ========================================================
        // 8. LICHTKANTE DER KAPPE
        // ========================================================

        gc.setStroke(Color.rgb(255, 255, 255, 0.16));
        gc.setLineWidth(1.2);
        gc.strokeOval(
                cx - INNER / 2 + 1,
                cy - INNER / 2 + 1,
                INNER - 2,
                INNER - 2
        );

        // ========================================================
        // 9. INNERE SCHATTENKANTE
        // ========================================================

        RadialGradient innerShadow = new RadialGradient(
                0,
                0,
                cx,
                cy,
                INNER / 2,
                false,
                CycleMethod.NO_CYCLE,
                new Stop[] {
                        new Stop(
                                0.78,
                                Color.TRANSPARENT
                        ),
                        new Stop(
                                0.91,
                                Color.rgb(
                                        20, 20, 20, 0.18
                                )
                        ),
                        new Stop(
                                1.00,
                                Color.rgb(
                                        5, 5, 5, 0.60
                                )
                        )
                }
        );

        gc.setFill(innerShadow);
        gc.fillOval(
                cx - INNER / 2,
                cy - INNER / 2,
                INNER,
                INNER
        );
    }
}