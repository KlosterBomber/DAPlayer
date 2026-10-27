package de.emgeem.daplayer.ui.display;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

import de.emgeem.daplayer.library.LibraryScanner;
import de.emgeem.daplayer.library.MediaLibrary;
import de.emgeem.daplayer.model.LibraryEntry;
import de.emgeem.daplayer.model.Playlist;
import de.emgeem.daplayer.model.Track;
import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

public class LibraryDisplay extends DisplayMode {

    // GEOMETRIE ++++++++++==========++++++++++
    private static final int COL_ARTIST = 0, COL_ALBUM = 1, COL_TITLE = 2, COL_SEARCH = 3, COL_FOLDERS = 4;
    private static final double[] COL_LEFT = {20, 330, 680, 20, 20};
    private static final double[] COL_RIGHT = {320, 670, 1138, 1138, 1138};
    private static final String[] COL_HEADER = {"ARTISTS", "ALBUMS", "TITLES", "RESULTS", "FOLDERS"};

    private static final double HEADER_Y = 50;
    private static final double LIST_TOP = 60;
    private static final double LIST_BOTTOM = 340;
    private static final double FIRST_ROW_TOP = 63;
    private static final double ROW_HEIGHT = 20;
    private static final int VISIBLE_ROWS = 14;
    private static final double STATUS_Y = 371;

    // Suchfeld
    private static final double SEARCH_X = 860;
    private static final double SEARCH_Y = 27;
    private static final double SEARCH_W = 268;
    private static final double SEARCH_H = 24;
    private static final int MAX_QUERY = 40;

    // Buttons: x, Breite
    private static final double BUTTON_Y = 350;
    private static final double BUTTON_HEIGHT = 30;
    private static final double[] BUTTON_X = {30, 240, 390};
    private static final double[] BUTTON_W = {200, 140, 140};
    private static final int BTN_ADD = 0, BTN_RESCAN = 1, BTN_FOLDERS = 2;
    private static final int BTN_ADD_FOLDER = 0, BTN_REMOVE_FOLDER = 1, BTN_BACK = 2;
    private static final double SCAN_STATUS_X = 550;

    // Monospace-Schrift: jedes Zeichen ist gleich breit
    private static final double CHAR_WIDTH = 18 * 0.6;

    // FELDER ++++++++++==========++++++++++
    private final Canvas canvas;
    private final Font displayFont;
    private final Font listFont;
    private final double[] scrollOffset = new double[5];
    private final PauseTransition messageTimer = new PauseTransition(Duration.seconds(4));
    private final PauseTransition searchTimer = new PauseTransition(Duration.millis(200));

    private MediaLibrary library;
    private List<String> artists = List.of();
    private List<MediaLibrary.AlbumInfo> albums = List.of();
    private List<LibraryEntry> tracks = List.of();
    private int selectedArtist = -1;
    private int selectedAlbum = -1;
    private int selectedTrack = -1;
    private int activeColumn = COL_ARTIST;
    private String searchQuery = "";
    private boolean searchFocused;
    private List<LibraryEntry> results = List.of();
    private int selectedResult = -1;
    private Consumer<List<Track>> addTracksAction;
    private int pressedButton = -1;
    private boolean scanning;
    private String statusMessage;
    private boolean folderMode;
    private List<String> folders = List.of();
    private List<String> folderStates = List.of();
    private int selectedFolder = -1;

    // KONSTRUKTOR ++++++++++==========++++++++++
    public LibraryDisplay(Font displayFont, Font listFont) {
        this.displayFont = displayFont;
        this.listFont = listFont;
        canvas = new Canvas();
        getChildren().add(canvas);
        messageTimer.setOnFinished(e -> {
            if (scanning) {return;}
            statusMessage = null;
            draw();
        });
        searchTimer.setOnFinished(e -> {
            updateSearch();
            draw();
        });
        canvas.setOnMousePressed(e -> {
            if (e.getButton() != MouseButton.PRIMARY) {return;}
            if (!folderMode && isInSearchField(e.getX(), e.getY())) {
                searchFocused = true;
                draw();
                return;
            }
            searchFocused = false;
            int button = buttonAt(e.getX(), e.getY());
            if (button >= 0) {
                pressedButton = button;
                draw();
                return;
            }
            int column = columnAt(e.getX());
            if (column >= 0) {activeColumn = column;}
            int row = rowAt(column, e.getY());
            if (row < 0) {return;}
            switch (column) {
                case COL_ARTIST -> {if (row != selectedArtist) {selectArtist(row);}}
                case COL_ALBUM -> {if (row != selectedAlbum) {selectAlbum(row);}}
                case COL_TITLE -> selectedTrack = row;
                case COL_SEARCH -> selectedResult = row;
                case COL_FOLDERS -> selectedFolder = row;
                default -> { }
            }
            draw();
        });
        canvas.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY || e.getClickCount() != 2) {return;}
            int column = columnAt(e.getX());
            LibraryEntry entry = entryAt(column, rowAt(column, e.getY()));
            if (entry != null) {addEntries(List.of(entry));}
        });
        canvas.setOnMouseReleased(e -> {
            int button = pressedButton;
            pressedButton = -1;
            boolean inside = button >= 0 && buttonAt(e.getX(), e.getY()) == button;
            draw();
            if (!inside || library == null) {return;}
            if (folderMode) {
                switch (button) {
                    case BTN_ADD_FOLDER -> chooseFolder();
                    case BTN_REMOVE_FOLDER -> removeSelectedFolder();
                    case BTN_BACK -> showFolders(false);
                    default -> { }
                }
                return;
            }
            switch (button) {
                case BTN_ADD -> addSelectionToPlaylist();
                case BTN_RESCAN -> {if (scanning) {library.cancelScan();} else {startRescan();}}
                case BTN_FOLDERS -> showFolders(true);
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
        tracks = index < 0 ? List.of() : library.getTracks(albums.get(index).key());
        scrollOffset[COL_TITLE] = 0;
    }

    private boolean isCompilationSelected() {return selectedAlbum >= 0 && selectedAlbum < albums.size() && albums.get(selectedAlbum).compilation();}

    private IntPredicate otherArtistRows() {
        if (!isCompilationSelected() || selectedArtist < 0) {return null;}
        String artist = artists.get(selectedArtist);
        if (MediaLibrary.isVariousArtists(artist)) {return null;}
        String key = LibraryEntry.toKey(artist);
        return row -> !LibraryEntry.toKey(tracks.get(row).getTrackArtist()).equals(key);
    }

    // SUCHE ++++++++++==========++++++++++
    private boolean isSearching() {return !searchQuery.isBlank();}
    private void updateSearch() {
        results = isSearching() ? library.search(searchQuery) : List.of();
        selectedResult = -1;
        scrollOffset[COL_SEARCH] = 0;
        activeColumn = isSearching() ? COL_SEARCH : COL_ARTIST;
    }
    private void clearSearch() {
        searchTimer.stop();
        searchQuery = "";
        searchFocused = false;
        updateSearch();
    }

    // ORDNER ++++++++++==========++++++++++
    private boolean isColumnMode() {return !folderMode && !isSearching();}
    private void showFolders(boolean show) {
        if (show) {
            clearSearch();
            reloadFolders();
            activeColumn = COL_FOLDERS;
        } else {
            activeColumn = COL_ARTIST;
        }
        folderMode = show;
        draw();
    }
    /** Liest die Ordnerliste und prüft einmalig, welche Ordner erreichbar sind. */
    private void reloadFolders() {
        folders = library == null ? List.of() : library.getFolders();
        folderStates = folders.stream()
                              .map(f -> new File(f).isDirectory() ? "" : "OFFLINE")
                              .toList();
        if (selectedFolder >= folders.size()) {selectedFolder = folders.size() - 1;}
        scrollOffset[COL_FOLDERS] = Math.min(scrollOffset[COL_FOLDERS], maxScroll(COL_FOLDERS));
    }
    private void chooseFolder() {
        if (scanning) {
            showMessage("WAIT FOR SCAN TO FINISH");
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Add library folder");
        File folder = chooser.showDialog(getScene().getWindow());
        if (folder == null) {return;}
        if (!library.addFolder(folder.getAbsolutePath())) {
            showMessage("FOLDER ALREADY ADDED");
            return;
        }
        reloadFolders();
        selectedFolder = folders.size() - 1;
        ensureVisible(COL_FOLDERS, selectedFolder);
        startRescan();
    }
    private void removeSelectedFolder() {
        if (scanning) {
            showMessage("WAIT FOR SCAN TO FINISH");
            return;
        }
        if (selectedFolder < 0 || selectedFolder >= folders.size()) {return;}
        library.removeFolder(folders.get(selectedFolder));
        reloadFolders();
        startRescan();
    }

    // AKTIONEN ++++++++++==========++++++++++
    /** Ausgewählter Titel oder, wenn keiner gewählt ist, das ganze Album. */
    private void addSelectionToPlaylist() {
        List<LibraryEntry> source = isSearching() ? results : tracks;
        int selected = isSearching() ? selectedResult : selectedTrack;
        addEntries(selected >= 0 ? List.of(source.get(selected)) : source);
    }
    /** Fügt Einträge der Playlist hinzu und meldet das in der Statuszeile. */
    private void addEntries(List<LibraryEntry> entries) {
        if (addTracksAction == null || entries.isEmpty()) {return;}
        addTracksAction.accept(entries.stream().map(LibraryEntry::toTrack).toList());
        showMessage(entries.size() == 1 ? "ADDED: " + entries.get(0).getDisplayTitle() : "ADDED " + entries.size() + " TRACKS");
    }
    private void showMessage(String text) {
        statusMessage = text;
        messageTimer.playFromStart();
        draw();
    }
    private void startRescan() {
        if (scanning) {return;}
        scanning = true;
        statusMessage = "SCANNING ...";
        draw();
        Thread thread = new Thread(() -> {
            String text;
            try {
                LibraryScanner.ScanResult result = library.rescan((done, total, file) ->
                    Platform.runLater(() -> {
                        statusMessage = "SCANNING " + done + "/" + total;
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
        reloadKeepingSelection();
        reloadFolders();
        if (isSearching()) {
            results = library.search(searchQuery);
            selectedResult = -1;
        }
        showMessage(text);
    }
    /** Lädt die Listen neu und stellt Interpret und Album anhand der Namen wieder her. */
    private void reloadKeepingSelection() {
        String artist = selectedArtist >= 0 ? artists.get(selectedArtist) : null;
        String albumKey = selectedAlbum >= 0 ? albums.get(selectedAlbum).key() : null;
        double artistScroll = scrollOffset[COL_ARTIST];
        reloadArtists();
        int artistIndex = artist == null ? -1 : artists.indexOf(artist);
        if (artistIndex < 0) {return;}
        selectArtist(artistIndex);
        scrollOffset[COL_ARTIST] = Math.min(artistScroll, maxScroll(COL_ARTIST));
        for (int i = 0; i < albums.size(); i++) {
            if (albums.get(i).key().equals(albumKey)) {
                selectAlbum(i);
                return;
            }
        }
    }
    private static String resultText(LibraryScanner.ScanResult r) {
        if (r.cancelled()) {return "SCAN CANCELLED";}
        String text = "+" + r.added() + " NEW  ~" + r.updated() + " CHG  -" + r.removed() + " DEL";
        if (r.offline() > 0) {text += "  (" + r.offline() + " OFFLINE)";}
        return text;
    }
    
    // TASTATUR ++++++++++==========++++++++++
    /** Wird von DAPlayerView zuerst gefragt; true = Taste verbraucht. */
    public boolean handleKey(KeyEvent e) {
        if (library == null) {return false;}
        if (e.isShortcutDown()) {
            if (e.getCode() != KeyCode.F || folderMode) {return false;}
            searchFocused = true;
            draw();
            return true;
        }
        if (searchFocused) {
            switch (e.getCode()) {
                case BACK_SPACE -> {
                    if (!searchQuery.isEmpty()) {
                        searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                        searchTimer.playFromStart();
                    }
                }
                case ESCAPE -> clearSearch();
                case ENTER, DOWN -> {
                    searchFocused = false;
                    if (!results.isEmpty()) {
                        selectedResult = 0;
                        ensureVisible(COL_SEARCH, 0);
                    }
                }
                default -> { }   // alles andere verschlucken, Zeichen kommen über KEY_TYPED
            }
            draw();
            return true;
        }
        switch (e.getCode()) {
            case UP -> moveSelection(-1);
            case DOWN -> moveSelection(1);
            case LEFT -> {if (isColumnMode()) {activeColumn = Math.max(COL_ARTIST, activeColumn - 1);}}
            case RIGHT -> {if (isColumnMode()) {activeColumn = Math.min(COL_TITLE, activeColumn + 1);}}
            case ENTER -> {
                if (folderMode) {return false;}
                addSelectionToPlaylist();
            }
            case DELETE -> {
                if (!folderMode) {return false;}
                removeSelectedFolder();
            }
            case ESCAPE -> {
                if (folderMode) {showFolders(false);}
                else if (isSearching()) {clearSearch();}
                else {return false;}
            }
            default -> {return false;}
        }
        draw();
        return true;
    }
    /** Zeicheneingabe (KEY_TYPED); true = Zeichen verbraucht. */
    public boolean handleTyped(String character) {
        if (!searchFocused || character == null || character.isEmpty()) {return false;}
        char c = character.charAt(0);
        // Rücktaste, Enter, Esc, Strg+F ... erzeugen ebenfalls KEY_TYPED, als Steuerzeichen
        if (Character.isISOControl(c)) {return true;}
        if (searchQuery.length() >= MAX_QUERY) {return true;}
        searchQuery += character;
        searchTimer.playFromStart();
        draw();
        return true;
    }
    private void moveSelection(int delta) {
        int count = rowCount(activeColumn);
        if (count == 0) {return;}
        int current = switch (activeColumn) {
            case COL_ARTIST -> selectedArtist;
            case COL_ALBUM -> selectedAlbum;
            case COL_TITLE -> selectedTrack;
            case COL_SEARCH -> selectedResult;
            case COL_FOLDERS -> selectedFolder;
            default -> -1;
        };
        int target = current < 0 ? 0 : Math.max(0, Math.min(count - 1, current + delta));
        if (target == current) {return;}
        switch (activeColumn) {
            case COL_ARTIST -> selectArtist(target);
            case COL_ALBUM -> selectAlbum(target);
            case COL_TITLE -> selectedTrack = target;
            case COL_SEARCH -> selectedResult = target;
            case COL_FOLDERS -> selectedFolder = target;
            default -> { }
        }
        ensureVisible(activeColumn, target);
    }
    /** Scrollt die Spalte so, dass die Zeile index sichtbar ist. */
    private void ensureVisible(int column, int index) {
        double rowTop = index * ROW_HEIGHT;
        double visibleHeight = VISIBLE_ROWS * ROW_HEIGHT;
        if (rowTop < scrollOffset[column]) {scrollOffset[column] = rowTop;}
        else if (rowTop + ROW_HEIGHT > scrollOffset[column] + visibleHeight) {scrollOffset[column] = rowTop + ROW_HEIGHT - visibleHeight;}
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

        if (folderMode) {
            gc.fillText(COL_HEADER[COL_FOLDERS] + " (" + folders.size() + ")", COL_LEFT[COL_FOLDERS] + 10, HEADER_Y);
        } else if (isSearching()) {
            gc.fillText(COL_HEADER[COL_SEARCH] + " (" + results.size() + ")", COL_LEFT[COL_SEARCH] + 10, HEADER_Y);
        } else {
            // Spaltenköpfe mit Anzahl, aktive Spalte hell
            int[] counts = {artists.size(), albums.size(), tracks.size()};
            for (int c = 0; c < 3; c++) {
                gc.setFill(c == activeColumn ? UiColors.accentLight() : UiColors.accentLight().deriveColor(0, 1, 1, 0.5));
                gc.fillText(COL_HEADER[c] + " (" + counts[c] + ")", COL_LEFT[c] + 10, HEADER_Y);
            }
            gc.setFill(UiColors.accentLight());
            // Spaltentrenner, mittig in der 10-px-Lücke
            gc.strokeLine(COL_RIGHT[COL_ARTIST] + 5, LIST_TOP, COL_RIGHT[COL_ARTIST] + 5, LIST_BOTTOM);
            gc.strokeLine(COL_RIGHT[COL_ALBUM] + 5, LIST_TOP, COL_RIGHT[COL_ALBUM] + 5, LIST_BOTTOM);
        }
        if (!folderMode) {drawSearchField(gc);}

        // Linien oben und unten
        gc.strokeLine(20, 55, COL_RIGHT[COL_TITLE], 55);
        gc.strokeLine(20, 345, COL_RIGHT[COL_TITLE], 345);

        // Buttons
        String[] labels = folderMode
                ? new String[] {"ADD FOLDER", "REMOVE", "BACK"}
                : new String[] {"ADD TO PLAYLIST", scanning ? "CANCEL" : "RESCAN", "FOLDERS"};
        for (int i = 0; i < labels.length; i++) {
            SettingsDisplay.drawSubmenuButton(gc, displayFont, BUTTON_X[i], BUTTON_Y, BUTTON_W[i], BUTTON_HEIGHT, labels[i], i == pressedButton);
        }
        gc.setFill(UiColors.accentLight());

        // Statuszeile rechts: Trackzahl und Gesamtdauer
        String status = statusText();
        double statusLeft = COL_RIGHT[COL_TITLE] - 10 - status.length() * CHAR_WIDTH;
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText(status, COL_RIGHT[COL_TITLE] - 10, STATUS_Y);
        gc.setTextAlign(TextAlignment.LEFT);

        // Meldunglinks: nur so lang, wie Platzt bis zum rechten Text ist
        if (statusMessage != null) {
            int maxChars = (int) ((statusLeft - 20 - SCAN_STATUS_X) / CHAR_WIDTH);
            if (maxChars >= 4) {gc.fillText(TrackListDisplay.limitText(statusMessage, maxChars), SCAN_STATUS_X, STATUS_Y);}
        }

        // Listen ohne Glow, wie in TrackListDisplay
        gc.setEffect(null);
        if (folderMode) {
            if (folders.isEmpty()) {
                gc.setFill(UiColors.accentLight().deriveColor(0, 1, 1, 0.45));
                gc.fillText("NO FOLDERS  -  ADD A FOLDER TO BUILD YOUR LIBRARY", 30, 120);
            }
            drawColumn(gc, COL_FOLDERS, folders, folderStates, selectedFolder, null);
            return;
        }
        if (isSearching()) {
            if (results.isEmpty() && searchTimer.getStatus() != Animation.Status.RUNNING) {
                gc.setFill(UiColors.accentLight().deriveColor(0, 1, 1, 0.45));
                gc.fillText("NO MATCHES FOR \"" + searchQuery.strip().toUpperCase() + "\"", 30, 120);
            } else if (results.isEmpty()) {
                gc.setFill(UiColors.accentLight().deriveColor(0, 1, 1, 0.45));
                gc.fillText("SEARCHING ...", 30, 120);
            }
            drawColumn(gc, COL_SEARCH, resultTexts(), durations(results), selectedResult, null);
            return;
        }
        if (library != null && artists.isEmpty()) {
            gc.setFill(UiColors.accentLight().deriveColor(0, 1, 1, 0.45));
            gc.fillText("LIBRARY IS EMPTY  -  CLICK FOLDERS TO ADD ONE", 30, 120);
        }
        drawColumn(gc, COL_ARTIST, artists, null, selectedArtist, null);
        drawColumn(gc, COL_ALBUM, albumTexts(), null, selectedAlbum, null);
        drawColumn(gc, COL_TITLE, trackTexts(), durations(tracks), selectedTrack, otherArtistRows());
    }
    private void drawSearchField(GraphicsContext gc) {
        Color accent = UiColors.accentLight();
        gc.setFill(accent.deriveColor(0, 1, 1, searchFocused ? 0.12 : 0.05));
        gc.setStroke(searchFocused ? accent : accent.deriveColor(0, 1, 1, 0.5));
        gc.fillRoundRect(SEARCH_X, SEARCH_Y, SEARCH_W, SEARCH_H, 6, 6);
        gc.strokeRoundRect(SEARCH_X, SEARCH_Y, SEARCH_W, SEARCH_H, 6, 6);
        String text;
        if (searchQuery.isEmpty() && !searchFocused) {
            gc.setFill(accent.deriveColor(0, 1, 1, 0.45));
            text = "SEARCH (CTRL+F)";
        } else {
            gc.setFill(accent);
            text = searchQuery + (searchFocused ? "_" : "");
        }
        // Zu lange Eingaben: das Ende zeigen, wo gerade getippt wird
        int maxChars = (int) ((SEARCH_W) / CHAR_WIDTH);
        if (text.length() > maxChars) {text = text.substring(text.length() - maxChars);}
        gc.fillText(text, SEARCH_X + 8, SEARCH_Y + 18);
        gc.setFill(accent);
        gc.setStroke(accent);
    }

    /** Zeichnet eine Spalte; rightTexts (optional) erscheinen rechtsbündig. */
    private void drawColumn(GraphicsContext gc, int column, List<String> rows, List<String> rightTexts, int selected, IntPredicate dimmed) {
        double left = COL_LEFT[column];
        double right = COL_RIGHT[column];
        double charWidth = listFont.getSize() * 0.6;
        int maxChars = (int) ((right - left - 20) / charWidth);
        if (rightTexts != null) {maxChars -= 7;}

        gc.save();
        gc.beginPath();
        gc.rect(left, LIST_TOP, right - left, LIST_BOTTOM - LIST_TOP);
        gc.closePath();
        gc.clip();
        gc.setFont(listFont);

        int first = Math.max(0, (int) (scrollOffset[column] / ROW_HEIGHT));
        int last = Math.min(rows.size() - 1, first + VISIBLE_ROWS + 1);
        for (int i = first; i <= last; i++) {
            double rowTop = FIRST_ROW_TOP + i * ROW_HEIGHT - scrollOffset[column];
            double baseline = rowTop + 15;
            boolean isSelected = i == selected;
            if (isSelected) {
                gc.setFill(UiColors.accentLight().deriveColor(0, 1, 1, 0.15));
                gc.fillRect(left + 5, rowTop, right - left - 10, ROW_HEIGHT);
            }
            Color normal = UiColors.accentLight().deriveColor(0, 1, 0.85, 1);
            if (dimmed != null && dimmed.test(i)) {normal = UiColors.accentLight().deriveColor(0, 1, 1, 0.4);}
            gc.setFill(isSelected ? UiColors.accentLight() : normal);
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
        if (folderMode) {return x >= COL_LEFT[COL_FOLDERS] && x <= COL_RIGHT[COL_FOLDERS] ? COL_FOLDERS : -1;}
        if (isSearching()) {return x >= COL_LEFT[COL_SEARCH] && x <= COL_RIGHT[COL_SEARCH] ? COL_SEARCH : -1;}
        for (int c = 0; c < 3; c++) {if (x >= COL_LEFT[c] && x <= COL_RIGHT[c]) {return c;}}
        return -1;
    }
    private boolean isInSearchField(double x, double y) {
        return x >= SEARCH_X && x <= SEARCH_X + SEARCH_W && y >= SEARCH_Y && y <= SEARCH_Y + SEARCH_H;
    }
    private LibraryEntry entryAt(int column, int row) {
        if (row < 0) {return null;}
        if (column == COL_TITLE) {return tracks.get(row);}
        if (column == COL_SEARCH) {return results.get(row);}
        return null;
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
            case COL_SEARCH -> results.size();
            case COL_FOLDERS -> folders.size();
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
        boolean compilation = isCompilationSelected();
        return tracks.stream()
                     .map(t -> (t.getTrackNumber() > 0 ? String.format("%02d  ", t.getTrackNumber()) : "    ") 
                     + (compilation ? t.getTrackArtist() + " - " : "") + t.getDisplayTitle()).toList();
    }
    private static List<String> durations(List<LibraryEntry> entries) {
        return entries.stream()
                      .map(t -> TrackListDisplay.formatDuration(t.getDuration()))
                      .toList();
    }
    private List<String> resultTexts() {
        return results.stream()
                      .map(t -> t.getDisplayArtist() + "  -  " + t.getDisplayTitle() + "  [" + t.getDisplayAlbum() + "]")
                      .toList();
    }
    private String statusText() {
        if (library == null) {return "NO LIBRARY";}
        int count = library.getTrackCount();
        String text = count + (count == 1 ? " TRACK" : " TRACKS");
        long total = library.getTotalDurationMillis();
        if (total > 0) {text += " / " + TrackListDisplay.formatDuration(total);}
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