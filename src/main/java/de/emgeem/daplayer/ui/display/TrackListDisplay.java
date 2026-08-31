package de.emgeem.daplayer.ui.display;

import de.emgeem.daplayer.model.Playlist;
import de.emgeem.daplayer.model.Track;
import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class TrackListDisplay extends DisplayMode {
    
    private final Canvas canvas;
    private final Font displayFont;
    private final Runnable addFileAction;
    private final Runnable addFolderAction;

    private double scrollOffset = 0;
    private double scrollPosition = 0;
    private Playlist playlist;
    private int pressedButton = -1;
    private boolean draggingScrollBar = false;
    private double dragStartY;
    private double DragStartPosition;

    public TrackListDisplay(Font displayFont, Runnable addFileAction, Runnable addFolderAction) {
        this.displayFont = displayFont;
        this.addFileAction = addFileAction;
        this.addFolderAction = addFolderAction;
        canvas = new Canvas();
        getChildren().add(canvas);
        
        canvas.setOnMousePressed(e -> {
            double x = e.getX();
            double y = e.getY();
            if (playlist != null && playlist.getTracks().size() > 14 && x >= 1122 && x <= 1138 && y >= 65 && y <=340) {
                double thumbHeight = 40;
                double thumbY = 65 + scrollPosition * (275 - thumbHeight);
                if (y >= thumbY && y <= thumbY + thumbHeight) {
                    draggingScrollBar = true;
                    dragStartY = y;
                    DragStartPosition = scrollPosition;
                    return;
                }
            }
            if (y >= 355 && y <= 395) {
                if (x >= 40 && x < 140) {pressedButton = 0;} 
                else if (x >= 180 && x < 280) {pressedButton = 1;} 
                else if (x >= 320 && x < 420) {pressedButton = 2;} 
                else if (x >= 460 && x < 560) {pressedButton = 3;}
                draw();
            }
            if (playlist != null && y >= 60 && y < 340) {
                double trackY = y + scrollOffset - 60;
                int index = (int) (trackY / 20);
                if (index >= 0 && index < playlist.getTracks().size()) {
                    playlist.setCurrentIndex(index);
                    draw();
                }
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (!draggingScrollBar) {return;}
            double deltaY = e.getY() - dragStartY;
            double travel = 275 - 40;
            scrollPosition = DragStartPosition + deltaY / travel;
            scrollPosition = Math.max(0, Math.min(1, scrollPosition));
            double maxScroll = Math.max(0, playlist.getTracks().size() - 14) * 20;
            scrollOffset = scrollPosition * maxScroll;
            draw();
        });

        canvas.setOnMouseReleased(e -> {
            draggingScrollBar = false;
            int button = pressedButton;
            pressedButton = -1;
            draw();
            if (button == 0 && addFileAction != null) {addFileAction.run();}
            if (button == 1 && addFolderAction != null) {addFolderAction.run();}
            if (button == 3 && playlist != null) {
                playlist.clear();
                scrollOffset = 0;
                scrollPosition = 0;
                draw();
            }
        });

        canvas.setOnScroll(e -> {
            if (playlist == null || playlist.getTracks().size() <= 14) {return;}
            double delta = e.getDeltaY();
            scrollPosition -= delta / 1000.0;
            scrollPosition = Math.max(0, Math.min(1, scrollPosition));
            double maxScroll = Math.max(0, playlist.getTracks().size() - 14) * 20;
            scrollOffset = scrollPosition * maxScroll;
            draw();
        });
    }

    @Override
    public void draw() {
        double w = getWidth();
        double h = getHeight();
        canvas.setWidth(w);
        canvas.setHeight(h);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        if (playlist == null) {return;}
        gc.setFont(displayFont);
        gc.setFill(UiColors.accentLight());
        gc.setEffect(UiColors.displayGlow());
        gc.fillText("TITLE", 30, 50);
        gc.fillText("ARTIST", 600, 50);
        gc.fillText("DURATION", 1033, 50);
        gc.setStroke(UiColors.accentLight());
        gc.setLineWidth(1);
        gc.strokeLine(20, 55, 1138, 55);
        gc.strokeLine(20, 345, 1138, 345);
        drawButton(gc, 30, 350, 140, 30, "ADD FILE", 0);
        drawButton(gc, 180, 350, 140, 30, "ADD FOLDER", 1);
        drawButton(gc, 330, 350, 140, 30, "ADD LOCATION", 2);
        drawButton(gc, 480, 350, 140, 30, "CLEAR", 3);
        gc.save();
        gc.beginPath();
        gc.rect(20, 60, w - 40, 280);
        gc.closePath();
        gc.clip();
        double y = 80 - scrollOffset;
        for (int i = 0; i < playlist.getTracks().size(); i++) {
            Track track = playlist.getTracks().get(i);
            String title = limitText(track.getTitle(), 45);
            String artist = limitText(track.getArtist(), 34);
            String duration = formatDuration(track.getDuration());
            int trackIndex = playlist.getTracks().indexOf(track);
            if (i == playlist.getCurrentIndex()) {
                gc.setFill(UiColors.accentLight().deriveColor(0, 1, 1, 0.15));
                gc.fillRoundRect(25, y-17, 1099, 20, 4, 4);
                gc.setFill(UiColors.accentLight());
            }
            gc.fillText(title, 30, y);
            gc.fillText(artist, 600, y);
            gc.fillText(duration, 1065, y);
            y += 20;
        }
        if (playlist.getTracks().size() > 14) {drawScrollBar(gc);}
        gc.restore();
        gc.setEffect(null);
    }

    @Override
    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
        scrollOffset = 0;
        scrollPosition = 0;
        draw();
    }

    public void setScrollOffset(double offset) {
        scrollOffset = Math.max(0, offset);
    }

    private void drawButton(GraphicsContext gc, double x, double y, double width, double height, String text, int buttonIndex) {
        gc.setLineWidth(1);
        gc.setStroke(UiColors.accentLight());
        if (pressedButton == buttonIndex) {gc.setFill(UiColors.accentLight());}
        else {gc.setFill(UiColors.accentLight().deriveColor(0, 1, 1, 0.08));}
        gc.fillRoundRect(x, y, width, height, 6, 6);
        gc.strokeRoundRect(x, y, width, height, 6, 6);
        gc.setFill(pressedButton == buttonIndex ? Color.BLACK : UiColors.accentLight());
        double textWidth = displayFont.getSize() * text.length() * 0.6;
        double textX = x + (width - textWidth) / 2.0;
        double textY = y + (height + displayFont.getSize()) / 2.0 - 2;
        gc.fillText(text, textX, textY);
    }

    private void drawScrollBar(GraphicsContext gc) {
        double x = 1130;
        double y = 65;
        double height = 275;
        gc.setStroke(UiColors.accentLight());
        gc.setLineWidth(1);
        gc.setEffect(UiColors.displayGlow());
        gc.strokeLine(x, y, x, y + height);
        double thumbHeight = 40;
        double thumbY = y + scrollPosition * (height - thumbHeight);
        gc.setFill(UiColors.accentLight());
        gc.fillRoundRect(x - 4, thumbY, 8, thumbHeight, 8, 8);
        gc.setEffect(null);
    }    

    private String limitText(String text, int maxCharacters) {
        if (text == null) {return "";}
        if (text.length() <= maxCharacters) {return text;}
        return text.substring(0, maxCharacters - 3) + "...";
    }

    private String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
