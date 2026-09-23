package de.emgeem.daplayer.ui.display.visual;

import de.emgeem.daplayer.audio.VisualizationData;
import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

public class VuVisualization {

    private static final int[] SCALE_DB = {3, 0, -1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -12, -14, -16, -18, -21, -24, -27, -30, -33, -36, -39, -42, -45, -48, -51};

    private static final int SEGMENTS = 81;
    private static final int SEGMENTS_PER_STEP = 3;
    private static final Color COLOR_OFF = Color.rgb(38, 38, 38);
    private static final Color COLOR_OFF_HIGHLIGHT = Color.rgb(60, 60, 60, 0.12);
    private static final Color COLOR_HIGHLIGHT = Color.rgb(255, 255, 255, 0.18);

    private final GraphicsContext gc;

    private final DropShadow greenGlow = new DropShadow();
    private final DropShadow yellowGlow = new DropShadow();
    private final DropShadow redGlow = new DropShadow();

    private double width;
    private double height;
    private double leftPeak;
    private double rightPeak;
    private double leftHold;
    private double rightHold;

    public VuVisualization(GraphicsContext gc) {
        this.gc = gc;

        greenGlow.setBlurType(BlurType.GAUSSIAN);
        greenGlow.setRadius(8);
        greenGlow.setSpread(0.5);
        greenGlow.setColor(Color.web("#1cbd00"));

        yellowGlow.setBlurType(BlurType.GAUSSIAN);
        yellowGlow.setRadius(8);
        yellowGlow.setSpread(0.5);
        yellowGlow.setColor(Color.web("#c79f00"));

        redGlow.setBlurType(BlurType.GAUSSIAN);
        redGlow.setRadius(8);
        redGlow.setSpread(0.5);
        redGlow.setColor(Color.web("#ff2600"));
    }

    public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public void update(VisualizationData data) {
        leftPeak = data.getLeftPeak();
        rightPeak = data.getRightPeak();
        if (leftPeak > leftHold) {leftHold = leftPeak;} 
        else {leftHold *= 0.997;}
        if (rightPeak > rightHold) {rightHold = rightPeak;} 
        else {rightHold *= 0.997;}
    }

    public void draw() {
        if (width <= 0 || height <= 0) {return;}
        double meterWidth = width * 0.82;
        double meterX = (width - meterWidth) / 2.0;
        double segmentWidth = meterWidth / SEGMENTS;
        double segmentHeight = 18;
        double leftY = height * 0.35;
        double rightY = height * 0.65;
        drawMeter(meterX, leftY, segmentWidth, segmentHeight, leftPeak, leftHold);
        drawMeter(meterX, rightY, segmentWidth, segmentHeight, rightPeak, rightHold);
        drawScale(meterX, meterWidth, leftY, rightY);
    }

    private void drawMeter(double x, double y, double segmentWidth, double segmentHeight, double level, double hold) {
        int activeSegments = dbToSegment(level);
        int holdSegment = dbToSegment(hold);
        for (int i = 0; i < SEGMENTS; i++) {
            double segmentX = x + i * segmentWidth;
            boolean active = i < activeSegments;
            drawSegment(segmentX, y, segmentWidth - 2, segmentHeight, i, active);
            if (i == holdSegment) {
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(2);
                gc.strokeLine(segmentX, y - 3, segmentX, y + segmentHeight + 3);
            }
        }
    }

    private void drawSegment(double x, double y, double width, double height, int segment, boolean active) {
        if (active) {
            gc.setFill(UiColors.accentLight());
            gc.fillRoundRect(x, y, width, height, 2, 2);
            gc.setEffect(getGlowForSegment(segment));
            gc.fillRoundRect(x, y, width, height, 2, 2);
            gc.setEffect(null);
            gc.setFill(COLOR_HIGHLIGHT);
            gc.fillRect(x + 1, y + 1, width - 2, 2);
        } else {
            gc.setFill(COLOR_OFF);
            gc.fillRoundRect(x, y, width, height, 2, 2);
            gc.setFill(COLOR_OFF_HIGHLIGHT);
            gc.fillRect(x + 1, y + 1, width - 2, 2);
        }
    }

    private static final int[] SCALE_LABELS = {-62, -48, -36, -26, -18, -12, -9, -6, -3, 0, 3};

    private void drawScale(double x, double meterWidth, double leftY, double rightY) {
        double scaleY = (leftY + rightY) / 2.0;
        gc.setFont(javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/JetBrainsMono-Regular.ttf"), 18));
        gc.setFill(UiColors.getAccent());
        gc.setEffect(UiColors.displayGlow());
        for (int i = 0; i < SCALE_LABELS.length; i++) {
            int db = SCALE_LABELS[i];
            double fraction = (double) i / (SCALE_LABELS.length - 1);
            double scaleX = x + fraction * meterWidth;
            String text = Integer.toString(db);
            double textWidth = text.length() * 6.5;
            gc.fillText(text, scaleX - textWidth / 2.0, scaleY + 15);
        }
        gc.fillText("db", x + meterWidth + 15, scaleY + 4);
        gc.setEffect(null);
    }

    private DropShadow getGlowForSegment(int segment) {
        int scaleIndex = (SEGMENTS - 1 - segment) / SEGMENTS_PER_STEP;
        scaleIndex = Math.max(0, Math.min(scaleIndex, SCALE_DB.length - 1));
        int db = SCALE_DB[scaleIndex];
        if (db >= -1) {return redGlow;}
        if (db >= -12) {return yellowGlow;}
        return greenGlow;
    }

    private int dbToSegment(double level) {
        if (level <= 0.00001) {return 0;}
        double db = 20.0 * Math.log10(level);
        if (db >= SCALE_DB[0]) {return SEGMENTS;}
        if (db <= SCALE_DB[SCALE_DB.length - 1]) {return 0;}
        for (int i = 0; i < SCALE_DB.length - 1; i++) {
            double upper = SCALE_DB[i];
            double lower = SCALE_DB[i + 1];
            if (db <= upper && db >= lower) {
                double fraction = (upper - db) / (upper - lower);
                int segment = SEGMENTS - (i * SEGMENTS_PER_STEP) - (int) Math.round(fraction * SEGMENTS_PER_STEP);
                return Math.max(0, Math.min(SEGMENTS, segment));
            }
        }
        return 0;
    }
}