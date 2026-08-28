package de.emgeem.daplayer.ui.display;

import de.emgeem.daplayer.model.Playlist;
import de.emgeem.daplayer.model.Track;
import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class PlayerDisplay extends Canvas {

    private final Font displayFont;
    private Playlist playlist;

    public PlayerDisplay(double width, double height) {

        super(width, height);

        displayFont = Font.loadFont(getClass().getResourceAsStream("/fonts/BitcountPropSingle-Regular.ttf"),18);

        widthProperty().addListener((obs, oldValue, newValue) -> draw());
        heightProperty().addListener((obs, oldValue, newValue) -> draw());

        draw();
    }


    private void draw() {

        GraphicsContext gc = getGraphicsContext2D();

        double w = getWidth();
        double h = getHeight();

        gc.clearRect(0, 0, w, h);

        drawDisplay(gc, 0, 0, w, h);
        drawTrackList(gc, w, h);
    }


    private void drawDisplay(GraphicsContext gc, double x, double y, double w, double h) {

        // Außenschacht
        gc.setFill(Color.rgb(22, 22, 22));
        gc.fillRoundRect(x, y, w, h, 16, 16);

        // oberes Highlight
        gc.setStroke(Color.rgb(90, 90, 90, 0.25));
        gc.setLineWidth(1);
        gc.strokeRoundRect(x + 1, y + 1, w - 2, h - 2, 16, 16);

        // unterer Schatten
        gc.setStroke(Color.rgb(0, 0, 0, 0.70));
        gc.strokeRoundRect(x + 3, y + 3, w - 6, h - 6, 12, 12);

        // eigentliche Displayfläche
        gc.setFill(Color.rgb(10, 10, 10));
        gc.fillRoundRect(x + 10, y + 10, w - 20, h - 20, 8, 8);

        // Lichtkante links
        gc.setStroke(Color.rgb(90, 90, 90, 0.12));
        gc.strokeLine(x + 11, y + 14, x + 11, y + h - 14);

        // Schatten rechts
        gc.setStroke(Color.rgb(0, 0, 0, 0.60));
        gc.strokeLine(x + w - 11, y + 14, x + w - 11, y + h - 14);
    }

    private void drawTestText(GraphicsContext gc, double w, double h) {
        String text = "DAPlayer";
        gc.setFont(displayFont);
        gc.setFill(UiColors.accentLight());
        gc.setEffect(UiColors.displayGlow());
        double textWidth = displayFont.getSize() * text.length() * 0.6;
        double x = (w - textWidth) / 2.0;
        double y = h / 2.0;
        gc.fillText(text, x, y);
        gc.setEffect(null);
    }

    private void drawTrackList(GraphicsContext gc, double w, double h) {
        if (playlist == null) {return;}
        gc.setFont(displayFont);
        gc.setFill(UiColors.accentLight());
        gc.setEffect(UiColors.displayGlow());
        double y = 50;
        for (Track track : playlist.getTracks()) {
            String info = String.format(
                "%s | %.1f kHz | %d bit | %s | %s",
                track.getTitle(),
                track.getSampleRate() / 1000.0,
                track.getBitDepth(),
                track.getChannels() == 1 ? "Mono" : "Stereo",
                formatDuration(track.getDuration())
            );
            gc.fillText(info, 30, y);
            y += 20;
        }
        gc.setEffect(null);
    }

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
        draw();
    }

    private String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}