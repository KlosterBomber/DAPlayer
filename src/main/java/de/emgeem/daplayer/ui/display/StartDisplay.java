package de.emgeem.daplayer.ui.display;

import de.emgeem.daplayer.model.Playlist;
import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;

public class StartDisplay extends DisplayMode {

    private final Canvas canvas;
    private final Font titleFont;
    private final Font subtitleFont;

    public StartDisplay() {
        titleFont = Font.loadFont(getClass().getResourceAsStream("/fonts/Exo2-Black.ttf"),88);
        subtitleFont = Font.loadFont(getClass().getResourceAsStream("/fonts/Exo2-Black.ttf"), 56);

        canvas = new Canvas();
        getChildren().add(canvas);
    }

    @Override
    public void draw() {

        double w = getWidth();
        double h = getHeight();

        canvas.setWidth(w);
        canvas.setHeight(h);

        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.clearRect(0, 0, w, h);

        gc.setFill(UiColors.accentLight());
        gc.setEffect(UiColors.displayGlow());

        gc.setFont(titleFont);
        String title = "DAPlayer";
        double textWidth = titleFont.getSize() * title.length() * 0.6;
        double x = (w - textWidth) / 2.0 + 6;
        double y = h / 2.0;
        gc.fillText(title, x, y);
    
        gc.setFont(subtitleFont);
        String subtitle = "by emgeem";       
        double subtextWidth = subtitleFont.getSize() * subtitle.length() * 0.6;
        double xs = (w - subtextWidth) / 2.0 - 5;
        double ys = h / 2.0 + 70;
        gc.fillText(subtitle, xs, ys);

        gc.setStroke(UiColors.accentLight());
        gc.setLineWidth(2);
        gc.strokeLine(368, 127, 790, 127);
        gc.strokeLine(368, 291, 790, 291);

        gc.setLineWidth(10);
        gc.strokeLine(378, 119, 378, 122);
        gc.strokeLine(398, 115, 398, 122);
        gc.strokeLine(418, 110, 418, 122);
        gc.strokeLine(438, 105, 438, 122);
        gc.strokeLine(458, 98, 458, 122);
        gc.strokeLine(478, 95, 478, 122);
        gc.strokeLine(498, 103, 498, 122);
        gc.strokeLine(518, 107, 518, 122);
        gc.strokeLine(538, 101, 538, 122);
        gc.strokeLine(558, 96, 558, 122);
        gc.strokeLine(578, 91, 578, 122);
        gc.strokeLine(598, 88, 598, 122);
        gc.strokeLine(618, 85, 618, 122);
        gc.strokeLine(638, 81, 638, 122);
        gc.strokeLine(658, 84, 658, 122);
        gc.strokeLine(678, 79, 678, 122);
        gc.strokeLine(698, 76, 698, 122);
        gc.strokeLine(718, 80, 718, 122);
        gc.strokeLine(738, 85, 738, 122);
        gc.strokeLine(758, 91, 758, 122);
        gc.strokeLine(778, 98, 778, 122);

        gc.setEffect(null);
    }

    @Override
    public void setPlaylist(Playlist playlist) {
        // Der Startbildschirm benötigt keine Playlist.
    }
}