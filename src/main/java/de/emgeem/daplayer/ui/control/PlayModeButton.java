package de.emgeem.daplayer.ui.control;

import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class PlayModeButton extends Canvas {

    // ============================================================
    // GEOMETRIE
    // ============================================================
    private static final double RES_FAC = 0.55;

    private static final double OUTER = 110 * RES_FAC;

    // LED-RING
    private static final double LED_OUTER = 95 * RES_FAC;
    private static final double LED_INNER = 91 * RES_FAC;

    // KAPPE
    private static final double INNER = 80 * RES_FAC;

    private static final Font BUTTON_FONT = Font.loadFont(RoundControlButton.class.getResourceAsStream("/fonts/SairaStencil_Condensed-Black.ttf"), 16);
    private String text;
    private boolean active;

    public PlayModeButton(String text, boolean active) {
        this.text = text;
        this.active = active;
        super.setWidth(OUTER + 12);
        super.setHeight(OUTER + 12);
        hoverProperty().addListener((obs, oldValue, newValue) -> draw());
        draw();
    }

    public void setText(String text) {
        this.text = text;
        draw();
    }
    public void setActive(boolean active) {
        this.active = active;
        draw();
    }

    public void draw() {

        GraphicsContext gc = getGraphicsContext2D();

        double cx = getWidth() / 2.0;
        double cy = getHeight() / 2.0;

        gc.clearRect(0, 0, getWidth(), getHeight());

        // ========================================================
        // 1. ÄUSSERE LICHTKANTE
        // ========================================================

        gc.setStroke(Color.rgb(110, 110, 110, 0.20));
        gc.setLineWidth(1.2);
        gc.strokeOval(cx - OUTER / 2 + 1, cy - OUTER / 2 + 1, OUTER - 2, OUTER - 2);

        // ========================================================
        // 2. ÄUSSERE SCHATTENKANTE
        // ========================================================

        gc.setStroke(Color.rgb(0, 0, 0, 0.80));
        gc.setLineWidth(2);
        gc.strokeOval(cx - OUTER / 2 + 3, cy - OUTER / 2 + 3, OUTER - 6, OUTER - 6);

        // ========================================================
        // 3. LED-RING
        // ========================================================

        if (!active) {gc.setFill(Color.rgb(35, 35, 35));}
        else {
            gc.setFill(UiColors.accentLight());
            gc.setEffect(UiColors.displayGlow());
        }
        gc.fillOval(cx - LED_OUTER / 2, cy - LED_OUTER / 2, LED_OUTER, LED_OUTER);
        gc.setEffect(null);

        // ========================================================
        // 4. INNERE AUSSPARUNG DES LED-RINGS
        // ========================================================

        gc.setFill(Color.rgb(14, 14, 14));
        gc.fillOval(cx - LED_INNER / 2, cy - LED_INNER / 2, LED_INNER, LED_INNER);

        // ========================================================
        // 5. MITTLERE KAPPE
        // ========================================================

        RadialGradient capGradient = new RadialGradient(0, 0, cx, cy, INNER / 2, false, CycleMethod.NO_CYCLE, new Stop[] {
            new Stop(0.00, Color.rgb(58, 58, 58)),
            new Stop(0.35, Color.rgb(40, 40, 40)),
            new Stop(0.72, Color.rgb(25, 25, 25)),
            new Stop(1.00, Color.rgb(12, 12, 12))
        });
        gc.setFill(capGradient);
        gc.fillOval(cx - INNER / 2, cy - INNER / 2, INNER, INNER);

        // ========================================================
        // 6. LICHTKANTE DER KAPPE
        // ========================================================

        gc.setStroke(Color.rgb(255, 255, 255, 0.16));
        gc.setLineWidth(1.2);
        gc.strokeOval(cx - INNER / 2 + 1, cy - INNER / 2 + 1, INNER - 2, INNER - 2);

        // ========================================================
        // 7. INNERE SCHATTENKANTE
        // ========================================================

        RadialGradient innerShadow = new RadialGradient(0, 0, cx, cy, INNER / 2, false, CycleMethod.NO_CYCLE, new Stop[] {
            new Stop(0.78, Color.TRANSPARENT),
            new Stop(0.91, Color.rgb(20, 20, 20, 0.18)),
            new Stop(1.00, Color.rgb(5, 5, 5, 0.60))
        });
        gc.setFill(innerShadow);
        gc.fillOval(cx - INNER / 2, cy - INNER / 2, INNER, INNER);

        // BESCHRIFTUNG
        gc.setFont(BUTTON_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        if (isHover() || active) {
            gc.setEffect(UiColors.displayGlow());
            gc.setFill(UiColors.accentLight());
        } else {
            gc.setEffect(null);
            gc.setFill(UiColors.getAccent());
        }
        if (text.contains("\n")) {
            String[] lines = text.split("\n");
            double lineHeight = BUTTON_FONT.getSize() * 1.05;
            double baselineOffset = BUTTON_FONT.getSize() * 0.35;
            gc.fillText(lines[0], cx, cy - lineHeight / 2 + baselineOffset);
            gc.fillText(lines[1], cx, cy + lineHeight / 2 + baselineOffset);
        } else {gc.fillText(text, cx, cy + BUTTON_FONT.getSize() * 0.35);}
        gc.setEffect(null);
    }
}