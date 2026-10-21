package de.emgeem.daplayer.ui.display;

import java.util.List;
import java.util.function.Consumer;

import de.emgeem.daplayer.library.LibraryScanner;
import de.emgeem.daplayer.library.MediaLibrary;
import de.emgeem.daplayer.model.LibraryEntry;
import de.emgeem.daplayer.model.Playlist;
import de.emgeem.daplayer.model.Track;
import de.emgeem.daplayer.ui.painter.UiColors;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class LibraryDisplay extends DisplayMode {

    // GEOMETRIE ++++++++++==========++++++++++
    private static final int COL_ARTIST = 0, COL_ALBUM = 1, COL_TITLE = 2;
    private static final double[] COL_LEFT = {20, 330, 680};
    private static final double[] COL_RIGHT = {320, 670, 1138};
    private static final String[] COL_HEADER = {"ARTISTS", "ALBUMS", "TITLES"};

    private static final double HEADER_Y = 50;
    private static final double LIST_TOP = 60;
    private static final double LIST_BOTTOM = 340;
    private static final double FIRST_ROW_TOP = 63;
    private static final double ROW_HEIGHT = 20;
    private static final int VISIBLE_ROWS = 14;
    private static final double STATUS_Y = 371;

    // Buttons: x, Breite
    private static final double BUTTON_Y = 350;
    private static final double BUTTON_HEIGHT = 30;
    private static final double[] BUTTON_X = {30, 240, 390};
    private static final double[] BUTTON_W = {200, 140, 140};
    private static final int BTN_ADD = 0, BTN_RESCAN = 1, BTN_FOLDERS = 2;
    private static final double SCAN_STATUS_X = 550;

    // Monospace-Schrift: jedes Zeichen ist gleich breit
    private static final double CHAR_WIDTH = 18 * 0.6;

    // FELDER ++++++++++==========++++++++++
    private final Canvas canvas;
    private final Font displayFont;
    private final double[] scrollOffset = new double[3];

    private MediaLibrary library;
    private List<String> artists = List.of();
    private List<MediaLibrary.AlbumInfo> albums = List.of();
    private List<LibraryEntry> tracks = List.of();
    private int selectedArtist = -1;
    private int selectedAlbum = -1;
    private int selectedTrack = -1;
    private Consumer<List<Track>> addTracksAction;
    private int pressedButton = -1;
    private boolean scanning;
    private String scanStatus;

    // KONSTRUKTOR ++++++++++==========++++++++++
    public LibraryDisplay(Font displayFont) {
        this.displayFont = displayFont;
        canvas = new Canvas();
        getChildren().add(canvas);
        canvas.setOnMousePressed(e -> {
            if (e.getButton() != MouseButton.PRIMARY) {return;}
            int button = buttonAt(e.getX(), e.getY());
            if (button >= 0) {
                pressedButton = button;
                draw();
                return;
            }
            int column = columnAt(e.getX());
            int row = rowAt(column, e.getY());
            if (row < 0) {return;}
            switch (column) {
                case COL_ARTIST -> {if (row != selectedArtist) {selectArtist(row);}}
                case COL_ALBUM -> {if (row != selectedAlbum) {selectAlbum(row);}}
                case COL_TITLE -> selectedTrack = row;
                default -> { }
            }
            draw();
        });
        canvas.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY || e.getClickCount() != 2) {return;}
            if (columnAt(e.getX()) != COL_TITLE) {return;}
            int row = rowAt(COL_TITLE, e.getY());
            if (row >= 0 && addTracksAction != null) {addTracksAction.accept(List.of(tracks.get(row).toTrack()));}
        });
        canvas.setOnMouseReleased(e -> {
            int button = pressedButton;
            pressedButton = -1;
            boolean inside = button >= 0 && buttonAt(e.getX(), e.getY()) == button;
            draw();
            if (!inside || library == null) {return;}
            switch (button) {
                case BTN_ADD -> addSelectionToPlaylist();
                case BTN_RESCAN -> {if (scanning) {library.cancelScan();} else {startRescan();}}
                case BTN_FOLDERS -> { }   // kommt in 5f
                default -> { }
            }
        });
        canvas.setOnScroll(e -> { 
            int column = columnAt(e.getX());
            if (column < 0) {return;}
            double rows = e.getDeltaY() > 0 ? -3 : (e.getDeltaY() < 0 ? 3 : 0);
            scrollBy(column, rows * ROW_HEIGHT);
        });
    }

    // AUSWAHL ++++++++++==========++++++++++
    private void reloadArtists() {
        artists = library == null ? List.of() : library.getArtists();
        scrollOffset[COL_ARTIST] = 0;
        selectArtist(artists.isEmpty() ? -1 : 0);
    }

    private void selectArtist(int index) {
        selectedArtist = index;
        albums = index < 0 ? List.of() : library.getAlbums(artists.get(index));
        scrollOffset[COL_ALBUM] = 0;
        selectAlbum(albums.isEmpty() ? -1 : 0);
    }

    private void selectAlbum(int index) {
        selectedAlbum = index;
        selectedTrack = -1;
        tracks = index < 0 ? List.of() : library.getTracks(artists.get(selectedArtist), albums.get(index).name());
        scrollOffset[COL_TITLE] = 0;
    }

    // AKTIONEN ++++++++++==========++++++++++
    /** Ausgewählter Titel oder, wenn keiner gewählt ist, das ganze Album. */
    private void addSelectionToPlaylist() {
        if (addTracksAction == null || tracks.isEmpty()) {return;}
        List<LibraryEntry> entries = selectedTrack >= 0 ? List.of(tracks.get(selectedTrack)) : tracks;
        addTracksAction.accept(entries.stream().map(LibraryEntry::toTrack).toList());
    }

    private void startRescan() {
        if (scanning) {return;}
        scanning = true;
        scanStatus = "SCANNING ...";
        draw();
        Thread thread = new Thread(() -> {
            String text;
            try {
                LibraryScanner.ScanResult result = library.rescan((done, total, file) ->
                    Platform.runLater(() -> {
                        scanStatus = "SCANNING " + done + "/" + total;
                        draw();
                    }));
                library.save();
                text = resultText(result);
            } catch (Exception ex) {
                text = "SCAN FAILED: " + ex.getMessage();
            }
            String finalText = text;
            Platform.runLater(() -> scanFinished(finalText));
        }, "library-scan");
        thread.setDaemon(true);
        thread.start();
    }

    private void scanFinished(String text) {
        scanning = false;
        scanStatus = text;
        reloadKeepingSelection();
        draw();
    }

    /** Lädt die Listen neu und stellt Interpret und Album anhand der Namen wieder her. */
    private void reloadKeepingSelection() {
        String artist = selectedArtist >= 0 ? artists.get(selectedArtist) : null;
        String album = selectedAlbum >= 0 ? albums.get(selectedAlbum).name() : null;
        double artistScroll = scrollOffset[COL_ARTIST];

        reloadArtists();
        int artistIndex = artist == null ? -1 : artists.indexOf(artist);
        if (artistIndex < 0) {return;}
        selectArtist(artistIndex);
        scrollOffset[COL_ARTIST] = Math.min(artistScroll, maxScroll(COL_ARTIST));

        for (int i = 0; i < albums.size(); i++) {
            if (albums.get(i).name().equals(album)) {
                selectAlbum(i);
                return;
            }
        }
    }

    private static String resultText(LibraryScanner.ScanResult r) {
        if (r.cancelled()) {return "SCAN CANCELLED";}
        String text = "+" + r.added() + " NEW  ~" + r.updated() + " CHANGED  -" + r.removed() + " REMOVED";
        if (r.offline() > 0) {text += "  (" + r.offline() + " OFFLINE)";}
        return text;
    }    

    // ZEICHNEN ++++++++++==========++++++++++
    @Override
    public void draw() {
        double w = getWidth();
        double h = getHeight();
        canvas.setWidth(w);
        canvas.setHeight(h);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        gc.setFont(displayFont);
        gc.setFill(UiColors.accentLight());
        gc.setStroke(UiColors.accentLight());
        gc.setLineWidth(1);
        gc.setEffect(UiColors.displayGlow());

        // Spaltenköpfe mit Anzahl
        int[] counts = {artists.size(), albums.size(), tracks.size()};
        for (int c = 0; c < 3; c++) {
            gc.fillText(COL_HEADER[c] + " (" + counts[c] + ")", COL_LEFT[c] + 10, HEADER_Y);
        }

        // Linien oben und unten
        gc.strokeLine(20, 55, COL_RIGHT[COL_TITLE], 55);
        gc.strokeLine(20, 345, COL_RIGHT[COL_TITLE], 345);

        // Spaltentrenner, mittig in der 10-px-Lücke
        gc.strokeLine(COL_RIGHT[COL_ARTIST] + 5, LIST_TOP, COL_RIGHT[COL_ARTIST] + 5, LIST_BOTTOM);
        gc.strokeLine(COL_RIGHT[COL_ALBUM] + 5, LIST_TOP, COL_RIGHT[COL_ALBUM] + 5, LIST_BOTTOM);

        // Buttons
        String[] labels = {"ADD TO PLAYLIST", scanning ? "CANCEL" : "RESCAN", "FOLDERS"};
        for (int i = 0; i < labels.length; i++) {
            SettingsDisplay.drawSubmenuButton(gc, displayFont, BUTTON_X[i], BUTTON_Y, BUTTON_W[i], BUTTON_HEIGHT, labels[i], i == pressedButton);
        }
        gc.setFill(UiColors.accentLight());
        if (scanStatus != null) {gc.fillText(scanStatus, SCAN_STATUS_X, STATUS_Y);}

        // Statuszeile unten rechts
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText(statusText(), COL_RIGHT[COL_TITLE] - 10, STATUS_Y);
        gc.setTextAlign(TextAlignment.LEFT);

        // Listen ohne Glow, wie in TrackListDisplay
        gc.setEffect(null);
        if (library != null && artists.isEmpty()) {
            gc.setFill(UiColors.accentLight().deriveColor(0, 1, 1, 0.45));
            gc.fillText("LIBRARY IS EMPTY  -  ADD A FOLDER AND RESCAN", 30, 120);
        }
        drawColumn(gc, COL_ARTIST, artists, null, selectedArtist);
        drawColumn(gc, COL_ALBUM, albumTexts(), null, selectedAlbum);
        drawColumn(gc, COL_TITLE, trackTexts(), trackDurations(), selectedTrack);
    }

    /** Zeichnet eine Spalte; rightTexts (optional) erscheinen rechtsbündig. */
    private void drawColumn(GraphicsContext gc, int column, List<String> rows, List<String> rightTexts, int selected) {
        double left = COL_LEFT[column];
        double right = COL_RIGHT[column];
        int maxChars = (int) ((right - left - 20) / CHAR_WIDTH);
        if (rightTexts != null) {maxChars -= 7;}

        gc.save();
        gc.beginPath();
        gc.rect(left, LIST_TOP, right - left, LIST_BOTTOM - LIST_TOP);
        gc.closePath();
        gc.clip();

        int first = Math.max(0, (int) (scrollOffset[column] / ROW_HEIGHT));
        int last = Math.min(rows.size() - 1, first + VISIBLE_ROWS + 1);
        for (int i = first; i <= last; i++) {
            double rowTop = FIRST_ROW_TOP + i * ROW_HEIGHT - scrollOffset[column];
            double baseline = rowTop + 17;
            boolean isSelected = i == selected;
            if (isSelected) {
                gc.setFill(UiColors.accentLight().deriveColor(0, 1, 1, 0.15));
                gc.fillRect(left + 5, rowTop, right - left - 10, ROW_HEIGHT);
            }
            gc.setFill(isSelected ? UiColors.accentLight() : UiColors.accentLight().deriveColor(0, 1, 0.85, 1));
            gc.fillText(TrackListDisplay.limitText(rows.get(i), maxChars), left + 10, baseline);
            if (rightTexts != null) {
                gc.setTextAlign(TextAlignment.RIGHT);
                gc.fillText(rightTexts.get(i), right - 10, baseline);
                gc.setTextAlign(TextAlignment.LEFT);
            }
        }
        gc.restore();
    }

    // HIT-TESTS / SCROLLEN ++++++++++==========++++++++++
    private int columnAt(double x) {
        for (int c = 0; c < COL_LEFT.length; c++) {
            if (x >= COL_LEFT[c] && x <= COL_RIGHT[c]) {return c;}
        }
        return -1;
    }
    private int rowAt(int column, double y) {
        if (column < 0 || y < LIST_TOP || y >= LIST_BOTTOM) {return -1;}
        int index = (int) Math.floor((y - FIRST_ROW_TOP + scrollOffset[column]) / ROW_HEIGHT);
        return index >= 0 && index < rowCount(column) ? index : -1;
    }
    private int rowCount(int column) {
        return switch (column) {
            case COL_ARTIST -> artists.size();
            case COL_ALBUM -> albums.size();
            case COL_TITLE -> tracks.size();
            default -> 0;
        };
    }
    private double maxScroll(int column) {
        return Math.max(0, rowCount(column) - VISIBLE_ROWS) * ROW_HEIGHT;
    }
    private void scrollBy(int column, double delta) {
        scrollOffset[column] = Math.max(0, Math.min(maxScroll(column), scrollOffset[column] + delta));
        draw();
    }
    private int buttonAt(double x, double y) {
        if (y < BUTTON_Y || y > BUTTON_Y + BUTTON_HEIGHT) {return -1;}
        for (int i = 0; i < BUTTON_X.length; i++) {
            if (x >= BUTTON_X[i] && x <= BUTTON_X[i] + BUTTON_W[i]) {return i;}
        }
        return -1;
    }

    // TEXTE ++++++++++==========++++++++++
    private List<String> albumTexts() {
        return albums.stream()
                     .map(a -> (a.year() > 0 ? a.year() + "  " : "      ") + a.name())
                     .toList();
    }

    private List<String> trackTexts() {
        return tracks.stream()
                     .map(t -> (t.getTrackNumber() > 0 ? String.format("%02d  ", t.getTrackNumber()) : "    ") + t.getDisplayTitle())
                     .toList();
    }

    private List<String> trackDurations() {
        return tracks.stream()
                     .map(t -> TrackListDisplay.formatDuration(t.getDuration()))
                     .toList();
    }

    private String statusText() {
        if (library == null) {return "NO LIBRARY";}
        int count = library.getTrackCount();
        String text = count + (count == 1 ? " TRACK" : " TRACKS");
        long total = library.getTotalDurationMillis();
        if (total > 0) {text += "  /  " + TrackListDisplay.formatDuration(total);}
        return text;
    }

    // ÖFFENTLICHE API ++++++++++==========++++++++++
    @Override
    public void setPlaylist(Playlist playlist) {}

    public void setLibrary(MediaLibrary library) {
        this.library = library;
        reloadArtists();
        draw();
    }
    public void setAddTracksAction(Consumer<List<Track>> action) {this.addTracksAction = action;}
}