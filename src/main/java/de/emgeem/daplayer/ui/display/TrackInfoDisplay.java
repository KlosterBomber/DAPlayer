package de.emgeem.daplayer.ui.display;

import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;
import java.util.function.Supplier;

import de.emgeem.daplayer.model.Playlist;
import de.emgeem.daplayer.model.Track;
import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class TrackInfoDisplay extends DisplayMode {

    private static final double BAR_X = 170;
    private static final double LINE_HEIGHT = 28;
    private static final double FIRST_LINE_Y = 60;
    private static final double BAR_BASELINE = FIRST_LINE_Y + 10 * LINE_HEIGHT + 25;
    /** Fortschritt wird höchstens in diesem Raster neu gezeichnet. */
    private static final long REDRAW_STEP_MILLIS = 100;

    private final Canvas canvas;
    private final Font displayFont;
    private final ArtworkManager artworkManager;

    private Playlist playlist;
    private Supplier<Track> playingTrackSupplier = () -> null;
    private BooleanSupplier seekableSupplier = () -> true;
    private LongConsumer seekAction;
    private Runnable browseChangedAction;

    private long currentPosition;
    private long totalDuration;
    private String errorText;

    public TrackInfoDisplay(Font displayFont) {
        this.displayFont = displayFont;
        canvas = new Canvas();
        getChildren().add(canvas);
        canvas.setOnScroll(e -> {
            // Blättern nur im Stopp-Zustand, damit Anzeige und Wiedergabe nicht auseinanderlaufen
            if (playlist == null || playlist.isEmpty() || playingTrackSupplier.get() != null) {return;}
            if (e.getDeltaY() < 0) {playlist.nextTrack();}
            else if (e.getDeltaY() > 0) {playlist.previousTrack();}
            if (browseChangedAction != null) {browseChangedAction.run();}
            draw();
        });
        canvas.setOnMousePressed(e -> seekFromMouse(e.getX(), e.getY()));
        canvas.setOnMouseDragged(e -> seekFromMouse(e.getX(), e.getY()));
        artworkManager = new ArtworkManager();
    }

    public void setSeekAction(LongConsumer seekAction) {this.seekAction = seekAction;}
    public void setPlayingTrackSupplier(Supplier<Track> supplier) {this.playingTrackSupplier = supplier;}
    public void setSeekableSupplier(BooleanSupplier supplier) {this.seekableSupplier = supplier;}
    public void setBrowseChangedAction(Runnable action) {this.browseChangedAction = action;}

    public void setPlaybackProgress(long position, long duration) {
        long quantized = position / REDRAW_STEP_MILLIS;
        if (quantized == currentPosition / REDRAW_STEP_MILLIS && this.totalDuration == duration) {return;}
        this.currentPosition = position;
        this.totalDuration = duration;
        if (isVisible()) {draw();}
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
        draw();
    }

    private Track displayedTrack() {
        Track playing = playingTrackSupplier.get();
        if (playing != null) {return playing;}
        return playlist != null ? playlist.getCurrentTrack() : null;
    }

    @Override
    public void draw() {
        double w = getWidth();
        double h = getHeight();
        canvas.setWidth(w);
        canvas.setHeight(h);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        Track track = displayedTrack();
        gc.setFont(displayFont);
        gc.setFill(UiColors.accentLight());
        gc.setEffect(UiColors.displayGlow());
        if (track == null) {
            gc.fillText("NO TRACK SELECTED", 50, FIRST_LINE_Y);
            drawError(gc, h);
            gc.setEffect(null);
            return;
        }
        Image artwork = artworkManager.getArtwork(track);
        double xLabel = 50;
        double xValue = 250;
        double y = FIRST_LINE_Y;
        String[][] rows = {
            {"TITLE", track.getDisplayTitle()},
            {"ARTIST", track.getArtist()},
            {"ALBUM", track.getAlbum()},
            {"ALBUM ARTIST", track.getAlbumArtist()},
            {"GENRE", track.getGenre()},
            {"YEAR", track.getYear() > 0 ? String.valueOf(track.getYear()) : ""},
            {"TRACK", track.getTrackNumber() > 0 ? String.valueOf(track.getTrackNumber()) : ""},
            {"DISC", track.getDiscNumber() > 0 ? String.valueOf(track.getDiscNumber()) : ""},
            {"FORMAT", track.getFormatDescription()},
            {"DURATION", track.isStream() && track.getDuration() <= 0 ? "LIVE STREAM" : TrackListDisplay.formatDuration(track.getDuration())},
        };
        for (String[] row : rows) {
            gc.fillText(row[0], xLabel, y);
            gc.fillText(limitText(row[1], 50), xValue, y);
            y += LINE_HEIGHT;
        }

        double barY = BAR_BASELINE;
        double barWidth = w - 340;
        double barHeight = 13;
        boolean live = totalDuration <= 0 && track.isStream();
        double progress = totalDuration > 0 ? (double) currentPosition / totalDuration : 0.0;
        progress = Math.max(0.0, Math.min(1.0, progress));
        gc.fillText(TrackListDisplay.formatDuration(currentPosition), 100 - (currentPosition >= 3_600_000 ? 30 : 0), barY);
        gc.fillText(live ? "LIVE" : TrackListDisplay.formatDuration(totalDuration), w - 150, barY);
        gc.setFill(Color.rgb(35, 35, 35));
        gc.fillRoundRect(BAR_X, barY - 13, barWidth, barHeight, 8, 8);
        gc.setFill(UiColors.accentLight());
        if (live) {
            gc.setGlobalAlpha(0.5);
            gc.fillRoundRect(BAR_X, barY - 13, barWidth, barHeight, 8, 8);
            gc.setGlobalAlpha(1.0);
        } else {
            gc.fillRoundRect(BAR_X, barY - 13, barWidth * progress, barHeight, 8, 8);
        }
        drawError(gc, h);

        if (artwork != null && artwork.getWidth() > 0) {
            double boxX = 850;
            double boxY = 50;
            double boxSize = 250;
            double imageWidth = artwork.getWidth();
            double imageHeight = artwork.getHeight();
            double scale = Math.min(boxSize / imageWidth, boxSize / imageHeight);
            double drawWidth = imageWidth * scale;
            double drawHeight = imageHeight * scale;
            double drawX = boxX + (boxSize - drawWidth) / 2;
            double drawY = boxY + (boxSize - drawHeight) / 2;
            gc.setEffect(null);
            gc.drawImage(artwork, drawX, drawY, drawWidth, drawHeight);
        }
        gc.setEffect(null);
    }

    private void drawError(GraphicsContext gc, double h) {
        if (errorText == null || errorText.isBlank()) {return;}
        gc.setEffect(UiColors.offGlow());
        gc.setFill(Color.web("#ff8a7a"));
        gc.fillText(limitText("ERROR: " + errorText, 95), 50, h - 18);
        gc.setEffect(UiColors.displayGlow());
        gc.setFill(UiColors.accentLight());
    }

    @Override
    public void setPlaylist(Playlist playlist) {this.playlist = playlist; draw();}

    private static String limitText(String value, int max) {
        if (value == null) {return "";}
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }

    private void seekFromMouse(double mouseX, double mouseY) {
        if (seekAction == null || totalDuration <= 0 || !seekableSupplier.getAsBoolean()) {return;}
        double barWidth = getWidth() - 340;
        double barY = BAR_BASELINE - 13;
        if (mouseX < BAR_X - 5 || mouseX > BAR_X + barWidth + 5 || mouseY < barY - 8 || mouseY > barY + 21) {return;}
        double progress = (mouseX - BAR_X) / barWidth;
        progress = Math.max(0.0, Math.min(1.0, progress));
        long position = (long) (totalDuration * progress);
        seekAction.accept(position);
        currentPosition = position;
        draw();
    }

}
