package de.emgeem.daplayer.ui.display;

import java.util.List;

import de.emgeem.daplayer.ui.KeyBindings;
import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;

/** Übersicht der Tastaturbefehle (SETTINGS / KEY CONTROLS). */
public class KeyControlsSettings extends StackPane {

    private final Font displayFont;
    private final Font displayFontSmall;
    private final Canvas canvas;

    public KeyControlsSettings(Font displayFont, Font displayFontSmall) {
        this.displayFont = displayFont;
        this.displayFontSmall = displayFontSmall;
        canvas = new Canvas();
        canvas.setMouseTransparent(true);
        getChildren().add(canvas);
        setMouseTransparent(true);
        widthProperty().addListener((obs, oldValue, newValue) -> draw());
        heightProperty().addListener((obs, oldValue, newValue) -> draw());
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
        gc.fillText("KEY CONTROLS", 30, 45);
        gc.setEffect(null);
        gc.setFont(displayFontSmall);
        List<KeyBindings.Binding> bindings = KeyBindings.ALL;
        int perColumn = (bindings.size() + 1) / 2;
        double[] columnX = {30, 590};
        double keyWidth = 230;
        double lineHeight = 33;
        for (int i = 0; i < bindings.size(); i++) {
            int column = i / perColumn;
            double x = columnX[column];
            double y = 85 + (i % perColumn) * lineHeight;
            KeyBindings.Binding b = bindings.get(i);
            gc.setFill(UiColors.accentLight());
            gc.fillText(b.keys(), x, y);
            gc.setFill(UiColors.accentLight().deriveColor(0, 1, 0.8, 1));
            gc.fillText(b.action(), x + keyWidth, y);
        }
    }
}
