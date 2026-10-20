package de.emgeem.daplayer.library;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import de.emgeem.daplayer.model.LibraryEntry;

public class MediaLibrary {

    public record AlbumInfo(String name, int year, int trackCount) {}

    private final LibraryStore store;
    private final LibraryScanner scanner;

    private volatile List<LibraryEntry> entries = List.of();
    private volatile List<String> folders = List.of();

    public MediaLibrary(LibraryStore store, LibraryScanner scanner) {
        this.store = store;
        this.scanner = scanner;
    }

    public void load() {
        LibraryStore.LibraryData data = store.load();
        entries = List.copyOf(data.getEntries());
        folders = List.copyOf(data.getFolders());
    }
    public void save() throws IOException {store.save(new LibraryStore.LibraryData(folders, entries));}

    public LibraryScanner.ScanResult rescan(LibraryScanner.ScanListener listener) throws IOException {
        LibraryScanner.ScanResult result = scanner.scan(folders, entries, listener);
        if (!result.cancelled()) {
            entries = List.copyOf(result.entries());
            save();
        }
        return result;
    }
    public void cancelScan() {scanner.cancel();}

    public List<String> getFolders() {return folders;}
    public boolean addFolder(String folder) {
        String path = new File(folder).getAbsolutePath();
        if (folders.contains(path)) {return false;}
        List<String> copy = new ArrayList<>(folders);
        copy.add(path);
        folders = List.copyOf(copy);
        return true;
    }
    public boolean removeFolder(String folder) {
        List<String> copy = new ArrayList<>(folders);
        boolean removed = copy.remove(folder);
        folders = List.copyOf(copy);
        return removed;
    }

    public int getTrackCount() {return entries.size();}
    public long getTotalDurationMillis() {return entries.stream().mapToLong(LibraryEntry::getDuration).sum();}
    public List<String> getArtists() {
        Map<String, Map<String, Long>> spellings = entries.stream()
                .collect(Collectors.groupingBy(
                        LibraryEntry::getArtistKey,
                        Collectors.groupingBy(LibraryEntry::getDisplayArtist, Collectors.counting())));
        return spellings.values().stream()
                .map(variants -> Collections.max(variants.entrySet(), Map.Entry.comparingByValue()).getKey())
                .map(MediaLibrary::prettify)
                .sorted(textOrder())
                .toList();
    }
    public List<AlbumInfo> getAlbums (String artist) {
        String artistKey = LibraryEntry.toKey(artist);
        Map<String, List<LibraryEntry>> byAlbum = entries.stream()
                .filter(e -> e.getArtistKey().equals(artistKey))
                .collect(Collectors.groupingBy(LibraryEntry::getDisplayAlbum));
        List<AlbumInfo> albums = new ArrayList<>();
        for (Map.Entry<String, List<LibraryEntry>> album : byAlbum.entrySet()) {
            int year = album.getValue().stream().mapToInt(LibraryEntry::getYear).filter(y -> y > 0).min().orElse(0);
            albums.add(new AlbumInfo(album.getKey(), year, album.getValue().size()));
        }
        albums.sort(Comparator.comparingInt((AlbumInfo a) -> a.year() == 0 ? Integer.MAX_VALUE : a.year()).thenComparing(AlbumInfo::name, textOrder()));
        return albums;
    }
    public List<LibraryEntry> getTracks(String artist, String album) {
        String artistKey = LibraryEntry.toKey(artist);
        return entries.stream()
            .filter(e -> e.getArtistKey().equals(artistKey))
            .filter(e -> e.getDisplayAlbum().equals(album))
            .sorted(albumOrder())
            .toList();
    }
    public List<LibraryEntry> search(String query) {
        if (query == null || query.isBlank()) {return List.of();}
        String[] words = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        return entries.stream().filter(e -> {
            String text = (e.getDisplayTitle() + " " + e.getDisplayArtist() + " " + e.getDisplayAlbum() + " " + nullToEmpty(e.getArtist())).toLowerCase(Locale.ROOT);
            for (String word : words) {if (!text.contains(word)) {return false;}}
            return true;
        }).sorted(Comparator.comparing(LibraryEntry::getDisplayArtist, textOrder())
            .thenComparing(LibraryEntry::getDisplayAlbum, textOrder())
            .thenComparing(albumOrder()))
            .toList();
    }

    private static Comparator<String> textOrder() {
        Collator collator = Collator.getInstance(Locale.GERMAN);
        return collator::compare;
    }

    private static Comparator<LibraryEntry> albumOrder() {
        return Comparator.comparingInt(LibraryEntry::getDiscNumber)
                .thenComparingInt(LibraryEntry::getTrackNumber)
                .thenComparing(LibraryEntry::getDisplayTitle, textOrder());
    }

    private static String nullToEmpty(String value) {return value == null ? "" : value;}

    private static String prettify(String name) {
        if (!name.equals(name.toLowerCase(Locale.ROOT))) {return name;}
        StringBuilder result = new StringBuilder(name.length());
        boolean startOfWord = true;
        for (char c : name.toCharArray()) {
            result.append(startOfWord ? Character.toUpperCase(c) : c);
            startOfWord = Character.isWhitespace(c) || c == '-' || c == '(';
        }
        return result.toString();
    }
}
