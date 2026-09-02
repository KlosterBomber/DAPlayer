package de.emgeem.daplayer.ui.display;

import de.emgeem.daplayer.ui.painter.UiColors;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class AccentColorSettings extends StackPane {

    private final Font displayFont;
    private final Canvas canvas;
    private final Runnable redrawAction;

    private int red;
    private int green;
    private int blue;

    private int activeChannel = -1;

    public AccentColorSettings(Font displayFont, Runnable redrawAction) {
        this.displayFont = displayFont;
        this.redrawAction = redrawAction;
        Color accent = UiColors.getAccent();
        red = (int) Math.round(accent.getRed() * 255);        
        green = (int) Math.round(accent.getGreen() * 255);    
        blue = (int) Math.round(accent.getBlue() * 255);        
        canvas = new Canvas();
        getChildren().add(canvas);
        canvas.setMouseTransparent(true);
        widthProperty().addListener((obs, oldValue, newValue) -> draw());
        heightProperty().addListener((obs, oldValue, newValue) -> draw());
        setOnMousePressed(e -> activeChannel = getChannelAt(e.getX(), e.getY()));
        setOnMouseDragged(e -> {
            if (activeChannel != -1) {
                updateChannel(e.getX());
                draw();
            }
        });
        setOnMouseReleased(e -> activeChannel = -1);
        setOnMouseClicked(e -> {
            System.out.println("ACCENT CLICK: " + e.getX() + " / " + e.getY());
            if (e.getX() >= 800 && e.getX() <= 910 && e.getY() >= 210 && e.getY() <= 240) {
                UiColors.setAccent(Color.rgb(red, green, blue));
                System.out.println("APPLIED: " + red + " / " + green + " / " + blue);
                redrawAction.run();
            }
        });
    }

    public void draw() {
        double w = getWidth();
        double h = getHeight();
        canvas.setWidth(w);
        canvas.setHeight(h);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        gc.setFill(UiColors.accentLight());
        gc.setEffect(UiColors.displayGlow());
        gc.setFont(displayFont);
        gc.fillText("ACCENT COLORS", 30, 45);
        drawSlider(gc, 30, 80, 700, "RED", red);
        drawSlider(gc, 30, 135, 700, "GREEN", green);
        drawSlider(gc, 30, 190, 700, "BLUE", blue);
        drawApplyButton(gc);
        gc.setEffect(null);
        drawPreview(gc);
    }

    private void drawSlider(GraphicsContext gc, double x, double y, double width, String label, int value) {
        double labelWidth = 90;
        double sliderX = x + labelWidth;
        double sliderWidth = width - labelWidth - 70;
        gc.setFill(UiColors.accentLight());
        gc.setFont(displayFont);
        gc.fillText(label, x, y + 5);
        // Grundstrecke
        gc.setStroke(Color.rgb(70, 70, 70));
        gc.setLineWidth(6);
        gc.strokeLine(sliderX, y, sliderX + sliderWidth, y);
        double thumbX = sliderX + (value / 255.0) * sliderWidth;
        // Aktive Strecke
        gc.setStroke(UiColors.accentLight());
        gc.setLineWidth(2);
        gc.strokeLine(sliderX, y, thumbX, y);
        // Thumb
        gc.setFill(UiColors.accentLight());
        gc.fillOval(thumbX - 6, y - 6, 12, 12);
        // Wert
        gc.fillText(String.valueOf(value), sliderX + sliderWidth + 20, y + 5);
    }

    private void drawPreview(GraphicsContext gc) {
        double x = 800;
        double y = 80;
        double size = 110;
        gc.setFill(Color.rgb(red, green, blue));
        gc.fillRoundRect(x, y, size, size, 8, 8);
        gc.setStroke(UiColors.accentLight());
        gc.setEffect(UiColors.displayGlow());
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, size, size, 8, 8);
        gc.setEffect(null);
    }

    private int getChannelAt(double mouseX, double mouseY) {
        if (mouseY >= 65 && mouseY <= 95) {return 0;}
        if (mouseY >= 120 && mouseY <= 150) {return 1;}
        if (mouseY >= 175 && mouseY <= 205) {return 2;}
        return -1;
    }

    private void updateChannel(double mouseX) {
        double sliderX = 120;
        double sliderWidth = 540;
        double position = (mouseX - sliderX) / sliderWidth;
        position = Math.max(0, Math.min(1, position));
        int value = (int) Math.round(position * 255);
        switch (activeChannel) {
            case 0 -> red = value;
            case 1 -> green = value;
            case 2 -> blue = value;
        }
    }

    private void drawApplyButton(GraphicsContext gc) {
        double x = 800;
        double y = 210;
        double width = 110;
        double height = 30;
        gc.setFill(Color.rgb(25, 25, 25));
        gc.fillRoundRect(x, y, width, height, 6, 6);
        gc.setStroke(UiColors.accentLight());
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, width, height, 6, 6);
        gc.setFill(UiColors.accentLight());
        gc.setFont(displayFont);
        double textWidth = gc.getFont().getSize() * 10 * 0.6;
        gc.fillText("ÜBERNEHMEN", x + (width - textWidth) / 2, y + 21);
    }
}