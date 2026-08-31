package de.emgeem.daplayer.ui.display;

import de.emgeem.daplayer.model.Playlist;
import de.emgeem.daplayer.model.Track;
import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;

public class TrackInfoDisplay extends DisplayMode {

    private final Canvas canvas;
    private final Font displayFont;

    private Playlist playlist;


    public TrackInfoDisplay(Font displayFont) {
        this.displayFont = displayFont;
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
        if (playlist == null || playlist.getTracks().isEmpty()) {return;}
        Track track = playlist.getCurrentTrack();
        if (track == null) {return;}
        gc.setFont(displayFont);
        gc.setFill(UiColors.accentLight());
        gc.setEffect(UiColors.displayGlow());
        double xLabel = 50;
        double xValue = 250;
        double y = 60;
        double lineHeight = 28;
        gc.fillText("TITLE", xLabel, y);
        gc.fillText(valueOrEmpty(track.getTitle()), xValue, y);
        y += lineHeight;
        gc.fillText("ARTIST", xLabel, y);
        gc.fillText(valueOrEmpty(track.getArtist()), xValue, y);
        y += lineHeight;
        gc.fillText("ALBUM", xLabel, y);
        gc.fillText(valueOrEmpty(track.getAlbum()), xValue, y);
        y += lineHeight;
        gc.fillText("ALBUM ARTIST", xLabel, y);
        gc.fillText(valueOrEmpty(track.getAlbumArtist()), xValue, y);
        y += lineHeight;
        gc.fillText("GENRE", xLabel, y);
        gc.fillText(valueOrEmpty(track.getGenre()), xValue, y);
        y += lineHeight;
        gc.fillText("YEAR", xLabel, y);
        gc.fillText(track.getYear() > 0 ? String.valueOf(track.getYear()) : "", xValue, y);
        y += lineHeight;
        gc.fillText("TRACK", xLabel, y);
        gc.fillText(track.getTrackNumber() > 0 ? String.valueOf(track.getTrackNumber()) : "", xValue, y);
        y += lineHeight;
        gc.fillText("DISC", xLabel, y);
        gc.fillText(track.getDiscNumber() > 0 ? String.valueOf(track.getDiscNumber()) : "", xValue, y);
        y += lineHeight;
        gc.fillText("FORMAT", xLabel, y);
        gc.fillText(track.getFormat(), xValue, y);
        y += lineHeight;
        gc.fillText("DURATION", xLabel, y);
        gc.fillText(formatDuration(track.getDuration()), xValue, y);
        gc.setEffect(null);
    }

    @Override
    public void setPlaylist(Playlist playlist) {this.playlist = playlist; draw();}

    private String valueOrEmpty(String value) {return value == null ? "" : value;}

    private String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
