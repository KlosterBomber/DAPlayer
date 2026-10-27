package de.emgeem.daplayer.ui.display;

import java.util.function.IntConsumer;

import de.emgeem.daplayer.model.Playlist;
import de.emgeem.daplayer.model.Track;
import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class TrackListDisplay extends DisplayMode {

    // Geometrie der Liste
    private static final double LIST_TOP = 60;
    private static final double LIST_BOTTOM = 340;
    private static final double ROW_HEIGHT = 20;
    private static final double FIRST_ROW_TOP = 63;
    private static final int VISIBLE_ROWS = 14;

    // Scrollbar
    private static final double SCROLL_X = 1130;
    private static final double SCROLL_Y = 65;
    private static final double SCROLL_HEIGHT = 275;
    private static final double THUMB_HEIGHT = 40;

    // Buttons: x, Breite, Beschriftung
    private static final double BUTTON_Y = 350;
    private static final double BUTTON_HEIGHT = 30;
    private static final double[][] BUTTONS = {
        {30, 140}, {180, 140}, {330, 140}, {480, 140}, {630, 140}, {818, 150}, {978, 150}
    };
    private static final String[] BUTTON_TEXT = {
        "ADD FILE", "ADD FOLDER", "ADD LOCATION", "CLEAR", "REMOVE", "SAVE PLAYLIST", "LOAD PLAYLIST"
    };
    private static final int BTN_ADD_FILE = 0, BTN_ADD_FOLDER = 1, BTN_ADD_LOCATION = 2, BTN_CLEAR = 3,
                             BTN_REMOVE = 4, BTN_SAVE = 5, BTN_LOAD = 6;

    private final Canvas canvas;
    private final Font displayFont;
    private final Runnable addFileAction;
    private final Runnable addFolderAction;
    private final Runnable savePlaylistAction;
    private final Runnable loadPlaylistAction;

    private Runnable addLocationAction;
    private Runnable clearAction;
    private IntConsumer playIndexAction;
    private IntConsumer removeIndexAction;
    private java.util.function.BooleanSupplier playingSupplier = () -> false;

    private double scrollOffset = 0;
    private Playlist playlist;
    private int pressedButton = -1;
    private int selectedIndex = -1;
    private boolean draggingScrollBar = false;
    private double dragStartY;
    private double dragStartOffset;
    private String statusText;

    public TrackListDisplay(Font displayFont, Runnable addFileAction, Runnable addFolderAction, Runnable savePlaylistAction, Runnable loadPlaylistAction) {
        this.displayFont = displayFont;
        this.addFileAction = addFileAction;
        this.addFolderAction = addFolderAction;
        this.savePlaylistAction = savePlaylistAction;
        this.loadPlaylistAction = loadPlaylistAction;
        canvas = new Canvas();
        getChildren().add(canvas);

        canvas.setOnMousePressed(e -> {
            double x = e.getX();
            double y = e.getY();
            if (playlist != null && playlist.size() > VISIBLE_ROWS && x >= SCROLL_X - 8 && x <= SCROLL_X + 8 && y >= SCROLL_Y && y <= SCROLL_Y + SCROLL_HEIGHT) {
                double thumbY = SCROLL_Y + scrollPosition() * (SCROLL_HEIGHT - THUMB_HEIGHT);
                if (y < thumbY || y > thumbY + THUMB_HEIGHT) {
                    // Klick neben den Thumb: seitenweise blättern
                    scrollBy(y < thumbY ? -VISIBLE_ROWS * ROW_HEIGHT : VISIBLE_ROWS * ROW_HEIGHT);
                }
                draggingScrollBar = true;
                dragStartY = y;
                dragStartOffset = scrollOffset;
                return;
            }
            pressedButton = buttonAt(x, y);
            if (pressedButton >= 0) {
                draw();
                return;
            }
            int index = rowAt(x, y);
            if (index >= 0 && e.getButton() == MouseButton.PRIMARY) {
                selectedIndex = index;
                // im Stopp-Zustand bestimmt die Auswahl, was PLAY startet
                if (!playingSupplier.getAsBoolean()) {playlist.setCurrentIndex(index);}
                draw();
            }
        });

        canvas.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY || e.getClickCount() != 2) {return;}
            int index = rowAt(e.getX(), e.getY());
            if (index >= 0 && playIndexAction != null) {playIndexAction.accept(index);}
        });

        canvas.setOnMouseDragged(e -> {
            if (!draggingScrollBar || playlist == null) {return;}
            double travel = SCROLL_HEIGHT - THUMB_HEIGHT;
            double deltaRatio = (e.getY() - dragStartY) / travel;
            setScrollOffset(dragStartOffset + deltaRatio * maxScroll());
            draw();
        });

        canvas.setOnMouseReleased(e -> {
            draggingScrollBar = false;
            int button = pressedButton;
            pressedButton = -1;
            boolean inside = button >= 0 && buttonAt(e.getX(), e.getY()) == button;
            draw();
            if (!inside) {return;}
            switch (button) {
                case BTN_ADD_FILE -> run(addFileAction);
                case BTN_ADD_FOLDER -> run(addFolderAction);
                case BTN_ADD_LOCATION -> run(addLocationAction);
                case BTN_CLEAR -> {
                    run(clearAction);
                    selectedIndex = -1;
                    scrollOffset = 0;
                    draw();
                }
                case BTN_REMOVE -> removeSelected();
                case BTN_SAVE -> run(savePlaylistAction);
                case BTN_LOAD -> run(loadPlaylistAction);
                default -> { }
            }
        });

        canvas.setOnScroll(e -> {
            if (playlist == null || playlist.size() <= VISIBLE_ROWS) {return;}
            double rows = e.getDeltaY() > 0 ? -3 : (e.getDeltaY() < 0 ? 3 : 0);
            scrollBy(rows * ROW_HEIGHT);
        });
    }

    private static void run(Runnable action) {if (action != null) {action.run();}}

    // ============================================================
    // ZEICHNEN
    // ============================================================

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
        String info = statusText != null ? statusText : playlist.size() + (playlist.size() == 1 ? " TRACK" : " TRACKS") + totalDurationText();
        gc.fillText(info, 140, 50);
        gc.setStroke(UiColors.accentLight());
        gc.setLineWidth(1);
        gc.strokeLine(20, 55, 1138, 55);
        gc.strokeLine(20, 345, 1138, 345);
        for (int i = 0; i < BUTTONS.length; i++) {
            drawButton(gc, BUTTONS[i][0], BUTTON_Y, BUTTONS[i][1], BUTTON_HEIGHT, BUTTON_TEXT[i], i,
                       i == BTN_REMOVE && (selectedIndex < 0 || selectedIndex >= playlist.size()));
        }
        gc.save();
        gc.beginPath();
        gc.rect(20, LIST_TOP, w - 40, LIST_BOTTOM - LIST_TOP);
        gc.closePath();
        gc.clip();
        gc.setEffect(null);
        if (playlist.isEmpty()) {
            gc.setFill(UiColors.accentLight().deriveColor(0, 1, 1, 0.45));
            gc.fillText("PLAYLIST IS EMPTY  -  ADD FILES, A FOLDER OR DROP FILES HERE", 30, 120);
        }
        int first = Math.max(0, (int) (scrollOffset / ROW_HEIGHT));
        int last = Math.min(playlist.size() - 1, first + VISIBLE_ROWS + 1);
        for (int i = first; i <= last; i++) {
            Track track = playlist.getTracks().get(i);
            double rowTop = FIRST_ROW_TOP + i * ROW_HEIGHT - scrollOffset;
            double baseline = rowTop + 17;
            boolean current = i == playlist.getCurrentIndex();
            if (current) {
                gc.setFill(UiColors.accentLight().deriveColor(0, 1, 1, 0.15));
                gc.fillRect(25, rowTop, 1095, ROW_HEIGHT);
            }
            if (i == selectedIndex) {
                gc.setStroke(UiColors.accentLight().deriveColor(0, 1, 1, 0.6));
                gc.setLineWidth(1);
                gc.strokeRect(25.5, rowTop + 0.5, 1094, ROW_HEIGHT - 1);
            }
            gc.setFill(current ? UiColors.accentLight() : UiColors.accentLight().deriveColor(0, 1, 0.85, 1));
            String prefix = current && playingSupplier.getAsBoolean() ? "> " : "";
            gc.fillText(limitText(prefix + track.getDisplayTitle(), 50), 30, baseline);
            gc.fillText(limitText(track.getArtist(), 39), 600, baseline);
            String duration = track.isStream() && track.getDuration() <= 0 ? " LIVE" : formatDuration(track.getDuration());
            gc.fillText(duration, 1060, baseline);
        }
        if (playlist.size() > VISIBLE_ROWS) {drawScrollBar(gc);}
        gc.restore();
        gc.setEffect(null);
    }

    private String totalDurationText() {
        long total = 0;
        for (Track t : playlist.getTracks()) {total += Math.max(0, t.getDuration());}
        if (total <= 0) {return "";}
        long seconds = total / 1000;
        return String.format(" / %d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);
    }

    private void drawButton(GraphicsContext gc, double x, double y, double width, double height, String text, int buttonIndex, boolean disabled) {
        Color accent = disabled ? UiColors.accentLight().deriveColor(0, 1, 1, 0.35) : UiColors.accentLight();
        gc.setLineWidth(1);
        gc.setStroke(accent);
        if (pressedButton == buttonIndex && !disabled) {gc.setFill(accent);}
        else {gc.setFill(accent.deriveColor(0, 1, 1, 0.08));}
        gc.fillRoundRect(x, y, width, height, 6, 6);
        gc.strokeRoundRect(x, y, width, height, 6, 6);
        gc.setFill(pressedButton == buttonIndex && !disabled ? Color.BLACK : accent);
        double textWidth = displayFont.getSize() * text.length() * 0.6;
        double textX = x + (width - textWidth) / 2.0;
        double textY = y + (height + displayFont.getSize()) / 2.0 - 2;
        gc.fillText(text, textX, textY);
    }

    private void drawScrollBar(GraphicsContext gc) {
        gc.setStroke(UiColors.accentLight());
        gc.setLineWidth(1);
        gc.setEffect(UiColors.displayGlow());
        gc.strokeLine(SCROLL_X, SCROLL_Y, SCROLL_X, SCROLL_Y + SCROLL_HEIGHT);
        double thumbY = SCROLL_Y + scrollPosition() * (SCROLL_HEIGHT - THUMB_HEIGHT);
        gc.setFill(UiColors.accentLight());
        gc.fillRoundRect(SCROLL_X - 4, thumbY, 8, THUMB_HEIGHT, 8, 8);
        gc.setEffect(null);
    }

    // ============================================================
    // HIT-TESTS / SCROLLEN
    // ============================================================

    private int buttonAt(double x, double y) {
        if (y < BUTTON_Y || y > BUTTON_Y + BUTTON_HEIGHT) {return -1;}
        for (int i = 0; i < BUTTONS.length; i++) {
            if (x >= BUTTONS[i][0] && x <= BUTTONS[i][0] + BUTTONS[i][1]) {return i;}
        }
        return -1;
    }

    private int rowAt(double x, double y) {
        if (playlist == null || y < LIST_TOP || y >= LIST_BOTTOM || x < 20 || x > SCROLL_X - 10) {return -1;}
        int index = (int) Math.floor((y - FIRST_ROW_TOP + scrollOffset) / ROW_HEIGHT);
        return index >= 0 && index < playlist.size() ? index : -1;
    }

    private double maxScroll() {
        return playlist == null ? 0 : Math.max(0, playlist.size() - VISIBLE_ROWS) * ROW_HEIGHT;
    }

    private double scrollPosition() {
        double max = maxScroll();
        return max > 0 ? scrollOffset / max : 0;
    }

    private void scrollBy(double delta) {
        setScrollOffset(scrollOffset + delta);
        draw();
    }

    public void setScrollOffset(double offset) {
        scrollOffset = Math.max(0, Math.min(maxScroll(), offset));
    }

    /** Scrollt so, dass der Track an index sichtbar ist. */
    public void ensureVisible(int index) {
        if (index < 0) {return;}
        double rowTop = index * ROW_HEIGHT;
        if (rowTop < scrollOffset) {setScrollOffset(rowTop);}
        else if (rowTop + ROW_HEIGHT > scrollOffset + VISIBLE_ROWS * ROW_HEIGHT) {setScrollOffset(rowTop - (VISIBLE_ROWS - 1) * ROW_HEIGHT);}
    }

    // ============================================================
    // ÖFFENTLICHE API
    // ============================================================

    @Override
    public void setPlaylist(Playlist playlist) {
        boolean samePlaylist = this.playlist == playlist;
        this.playlist = playlist;
        if (!samePlaylist) {scrollOffset = 0;}
        setScrollOffset(scrollOffset);
        if (selectedIndex >= playlist.size()) {selectedIndex = -1;}
        draw();
    }

    public void updateCurrentTrack() {
        if (playlist != null) {
            if (selectedIndex >= playlist.size()) {selectedIndex = -1;}
            setScrollOffset(scrollOffset);
            ensureVisible(playlist.getCurrentIndex());
        }
        draw();
    }

    public int getSelectedIndex() {return selectedIndex;}

    /** Auswahl per Tastatur verschieben. */
    public void moveSelection(int delta) {
        if (playlist == null || playlist.isEmpty()) {return;}
        int start = selectedIndex >= 0 ? selectedIndex : Math.max(0, playlist.getCurrentIndex());
        selectedIndex = Math.max(0, Math.min(playlist.size() - 1, start + (selectedIndex >= 0 ? delta : 0)));
        if (!playingSupplier.getAsBoolean()) {playlist.setCurrentIndex(selectedIndex);}
        ensureVisible(selectedIndex);
        draw();
    }

    public void playSelected() {
        if (selectedIndex >= 0 && playIndexAction != null) {playIndexAction.accept(selectedIndex);}
    }

    public void removeSelected() {
        if (playlist == null || selectedIndex < 0 || selectedIndex >= playlist.size() || removeIndexAction == null) {return;}
        removeIndexAction.accept(selectedIndex);
        if (selectedIndex >= playlist.size()) {selectedIndex = playlist.size() - 1;}
        setScrollOffset(scrollOffset);
        draw();
    }

    /** Statuszeile oben (z. B. "LOADING 12/240"); null = Trackanzahl. */
    public void setStatusText(String statusText) {
        this.statusText = statusText;
        draw();
    }

    public void setAddLocationAction(Runnable action) {this.addLocationAction = action;}
    public void setClearAction(Runnable action) {this.clearAction = action;}
    public void setPlayIndexAction(IntConsumer action) {this.playIndexAction = action;}
    public void setRemoveIndexAction(IntConsumer action) {this.removeIndexAction = action;}
    public void setPlayingSupplier(java.util.function.BooleanSupplier supplier) {this.playingSupplier = supplier;}

    static String limitText(String text, int maxCharacters) {
        if (text == null) {return "";}
        if (text.length() <= maxCharacters) {return text;}
        return text.substring(0, maxCharacters - 3) + "...";
    }

    static String formatDuration(long millis) {
        long totalSeconds = Math.max(0, millis) / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds / 60) % 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {return String.format("%d:%02d:%02d", hours, minutes, seconds);}
        return String.format("%02d:%02d", minutes, seconds);
    }
}
