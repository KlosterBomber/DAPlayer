package de.emgeem.daplayer.ui.control;

import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Effect;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class VolumeKnob extends Canvas {

    public interface VolumeListener {void onVolumeChanged(double volume);}
    public interface MuteListener {void onMuteChanged(boolean muted);}

    private VolumeListener volumeListener;
    private MuteListener muteListener;

    public void setVolumeListener(VolumeListener listener) {this.volumeListener = listener;}
    public void setMuteListener(MuteListener listener) {this.muteListener = listener;}

    private double volume = 1.0;
    private boolean muted = false;

    private static final double OUTER = 220;
    private static final double RING = 178;

    private static final double LED_OUTER = 194;
    private static final double LED_INNER = 187;

    private static final double ALU_OUTER = 180;
    private static final double ALU_INNER = 160;

    private static final double INNER = 160;

    private static final Font BUTTON_FONT = Font.loadFont(RoundControlButton.class.getResourceAsStream("/fonts/SairaStencil_Condensed-Black.ttf"), 22);

    private final String text;

    private double knobAngle = 420;

    public VolumeKnob(String text) {
        this.text = text;
        super.setWidth(OUTER + 12);
        super.setHeight(OUTER + 12);
        hoverProperty().addListener((obs, oldValue, newValue) -> draw());
        setOnScroll(this::mouseScrolled);
        setOnMouseClicked(this::mouseClicked);
        setVolume(volume);
        draw();
    }

    public void setVolume(double volume) {
        this.volume = Math.max(0.0, Math.min(1.0, volume));
        knobAngle = 120 + this.volume * 300;
        draw();
    }

    public double  getVolume() {return volume;}

    private void mouseClicked(MouseEvent e) {
        double cx = getWidth() / 2.0;
        double cy = getHeight() / 2.0;
        double dx = e.getX() - cx;
        double dy = e.getY() - cy;

        if (Math.sqrt(dx*dx + dy*dy) <= OUTER * 0.45) {
            muted = !muted;
            if (muteListener != null) {muteListener.onMuteChanged(muted);}
            draw();
        }
    }
    public void setMuted(boolean muted) {
        this.muted = muted;
        draw();
    }

    private void mouseScrolled(ScrollEvent e) {
        double delta = e.getDeltaY();
        if (delta > 0) {volume += 0.05;} else if (delta < 0) {volume -= 0.05;}
        volume = Math.max(0.0, Math.min(1.0, volume));
        knobAngle = 120 + volume * 300;
        if (volumeListener != null) {volumeListener.onVolumeChanged(volume);}
        draw();
        e.consume();
    }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double cx = getWidth() / 2.0;
        double cy = getHeight() / 2.0;
        gc.clearRect(0, 0, getWidth(), getHeight());
        Color ledColor = muted ? Color.web("#ff2500") : UiColors.getAccent();
// obere Lichtkante
        gc.setStroke(Color.rgb(110, 110, 110, 0.20));
        gc.setLineWidth(1.2);
        gc.strokeOval(cx - OUTER / 2 + 1, cy - OUTER / 2 + 1, OUTER - 2, OUTER - 2);
// untere Schattenkante
        gc.setStroke(Color.rgb(0, 0, 0, 0.80));
        gc.setLineWidth(2);
        gc.strokeOval(cx - OUTER / 2 + 3, cy - OUTER / 2 + 3, OUTER - 6, OUTER - 6);
// 3. LED-RING
        RadialGradient ledSurface = new RadialGradient(0, 0, cx, cy, LED_OUTER / 2, false, CycleMethod.NO_CYCLE, new Stop[] {
                new Stop(0.00, ledColor.deriveColor(0, 1.0, 1.25, 1.0)),
                new Stop(0.55, ledColor.deriveColor(0, 1.0, 1.15, 1.0)),
                new Stop(0.88, ledColor),
                new Stop(1.00, ledColor.deriveColor(0, 1.0, 0.82, 1.0))
        });
        gc.setFill(ledSurface);
        gc.fillOval(cx - LED_OUTER / 2, cy - LED_OUTER / 2, LED_OUTER, LED_OUTER);
// 4. INNERE AUSSPARUNG DES LED-RINGS
        gc.setFill(Color.rgb(14, 14, 14));
        gc.fillOval(cx - LED_INNER / 2, cy - LED_INNER / 2, LED_INNER, LED_INNER);
// 5. ALUMINIUMRING
        LinearGradient aluGradient = new LinearGradient(0, cy - ALU_OUTER / 2, 0, cy + ALU_OUTER / 2, false, CycleMethod.NO_CYCLE, new Stop[] {
                new Stop(0.00, Color.rgb(105, 105, 105)),
                new Stop(0.18, Color.rgb(85, 85, 85)),
                new Stop(0.50, Color.rgb(66, 66, 66)),
                new Stop(0.82, Color.rgb(48, 48, 48)),
                new Stop(1.00, Color.rgb(28, 28, 28))
        });
        gc.setFill(aluGradient);
        gc.fillOval(cx - ALU_OUTER / 2, cy - ALU_OUTER / 2, ALU_OUTER, ALU_OUTER);
// 6. INNERE AUSSPARUNG DES ALURINGS
        gc.setFill(Color.rgb(13, 13, 13));
        gc.fillOval(cx - ALU_INNER / 2, cy - ALU_INNER / 2, ALU_INNER, ALU_INNER);
// 7. MITTLERE KAPPE
        RadialGradient capGradient = new RadialGradient(0, 0, cx, cy, INNER / 2, false, CycleMethod.NO_CYCLE, new Stop[] {
                new Stop(0.00, Color.rgb(58, 58, 58)),
                new Stop(0.35, Color.rgb(40, 40, 40)),
                new Stop(0.72, Color.rgb(25, 25, 25)),
                new Stop(1.00, Color.rgb(12, 12, 12))
        });
        gc.setFill(capGradient);
        gc.fillOval(cx - INNER / 2, cy - INNER / 2, INNER, INNER);
// 8. LICHTKANTE DER KAPPE
        gc.setStroke(Color.rgb(255, 255, 255, 0.16));
        gc.setLineWidth(1.2);
        gc.strokeOval(cx - INNER / 2 + 1, cy - INNER / 2 + 1, INNER - 2, INNER - 2);
// 9. INNERE SCHATTENKANTE
        RadialGradient innerShadow = new RadialGradient(0, 0, cx, cy, INNER / 2, false, CycleMethod.NO_CYCLE, new Stop[] {
                new Stop(0.78,Color.TRANSPARENT),
                new Stop(0.91, Color.rgb(20, 20, 20, 0.18)),
                new Stop(1.00, Color.rgb(5, 5, 5, 0.60))
        });
        gc.setFill(innerShadow);
        gc.fillOval(cx - INNER / 2, cy - INNER / 2, INNER, INNER);
// MARKER
        double angle = Math.toRadians(knobAngle);
        double r = (RING + INNER) / 4.0;
        double px = cx + Math.cos(angle) * r;
        double py = cy + Math.sin(angle) * r;
        Color markerColor = muted ? Color.web("#ff2600") : UiColors.getAccent();
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
// BESCHRIFTUNG
        Color textColor = muted ? Color.web("#ff2600") : UiColors.getAccent();
        Color textColorHover = muted ? Color.web("#ffb5b5") : UiColors.accentLight();
        Effect textGlow = muted ? UiColors.offGlow() : UiColors.displayGlow();
        String knobText = muted ? "MUTED" : text;
        gc.setFont(BUTTON_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        if (isHover()) {
            gc.setEffect(textGlow);
            gc.setFill(textColorHover);
        } else {
            gc.setEffect(null);
            gc.setFill(textColor);
        }
        if (text.contains("\n")) {
            String[] lines = text.split("\n");
            double lineHeight = BUTTON_FONT.getSize() * 1.05;
            double baselineOffset = BUTTON_FONT.getSize() * 0.35;
            gc.fillText(lines[0], cx, cy - lineHeight / 2 + baselineOffset);
            gc.fillText(lines[1], cx, cy + lineHeight / 2 + baselineOffset);
        } else {gc.fillText(knobText, cx, cy + BUTTON_FONT.getSize() * 0.35);}
        gc.setEffect(null);
    }
}