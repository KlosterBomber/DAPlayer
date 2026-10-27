package de.emgeem.daplayer.library;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.emgeem.daplayer.model.LibraryEntry;

public class MediaLibrary {

    public static final String VARIOUS_ARTISTS = "VARIOUS ARTISTS";
    private static final String VARIOUS_KEY = LibraryEntry.toKey(VARIOUS_ARTISTS);
    /** Album-Interpreten, die einen Sampler kennzeichnen (als Schlüssel: klein, Leerzeichen normalisiert). */
    private static final Set<String> VARIOUS_NAMES = Set.of(
        "various artists", "various", "va", "v.a.", "v.a", "v. a.",
        "verschiedene interpreten", "verschiedene künstler", "diverse", "diverse interpreten");

    /** key = Ordner + Albumname, eindeutig auch bei gleichnamigen Alben verschiedener Interpreten. */
    public record AlbumInfo(String key, String name, int year, int trackCount, boolean compilation) {}

    /** Unveränderlicher Schnappschuss aller abgeleiteten Daten; wird bei jeder Änderung komplett ersetzt. */
    private record Index(List<LibraryEntry> entries, long totalDurationMillis, List<String> artists,
                         Map<String, List<AlbumInfo>> albumsByArtist, Map<String, List<LibraryEntry>> tracksByAlbum) {
        static final Index EMPTY = new Index(List.of(), 0, List.of(), Map.of(), Map.of());
    }

    // FELDER ++++++++++==========++++++++++
    private final LibraryStore store;
    private final LibraryScanner scanner;

    private volatile Index index = Index.EMPTY;
    private volatile List<String> folders = List.of();

    // KONSTRUKTOR ++++++++++==========++++++++++
    public MediaLibrary(LibraryStore store, LibraryScanner scanner) {
        this.store = store;
        this.scanner = scanner;
    }

    // LADEN / SPEICHERN / SCANNEN ++++++++++==========++++++++++
    public void load() {
        LibraryStore.LibraryData data = store.load();
        index = buildIndex(data.getEntries());
        folders = List.copyOf(data.getFolders());
    }
    public void save() throws IOException {store.save(new LibraryStore.LibraryData(folders, index.entries()));}

    public LibraryScanner.ScanResult rescan(LibraryScanner.ScanListener listener) throws IOException {
        LibraryScanner.ScanResult result = scanner.scan(folders, index.entries(), listener);
        if (!result.cancelled()) {
            index = buildIndex(result.entries());
            save();
        }
        return result;
    }
    public void cancelScan() {scanner.cancel();}

    // ORDNER ++++++++++==========++++++++++
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

    // ABFRAGEN ++++++++++==========++++++++++
    public int getTrackCount() {return index.entries().size();}
    public long getTotalDurationMillis() {return index.totalDurationMillis();}
    public List<String> getArtists() {return index.artists();}
    public List<AlbumInfo> getAlbums(String artist) {return index.albumsByArtist().getOrDefault(LibraryEntry.toKey(artist), List.of());}
    public List<LibraryEntry> getTracks(String albumKey) {return index.tracksByAlbum().getOrDefault(albumKey, List.of());}

    public static boolean isVariousArtists(String artist) {return artist != null && VARIOUS_NAMES.contains(LibraryEntry.toKey(artist));}

    public List<LibraryEntry> search(String query) {
        if (query == null || query.isBlank()) {return List.of();}
        String[] words = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        return index.entries().stream().filter(e -> {
            String text = (e.getDisplayTitle() + " " + e.getDisplayArtist() + " " + e.getDisplayAlbum() + " " + nullToEmpty(e.getArtist())).toLowerCase(Locale.ROOT);
            for (String word : words) {if (!text.contains(word)) {return false;}}
            return true;
        }).sorted(Comparator.comparing(LibraryEntry::getDisplayArtist, textOrder())
            .thenComparing(LibraryEntry::getDisplayAlbum, textOrder())
            .thenComparing(albumOrder()))
            .toList();
    }

    // INDEX ++++++++++==========++++++++++
    private static Index buildIndex(List<LibraryEntry> source) {
        List<LibraryEntry> entries = List.copyOf(source);
        Map<String, List<LibraryEntry>> grouped = entries.stream().collect(Collectors.groupingBy(MediaLibrary::albumKey));

        Map<String, List<LibraryEntry>> tracksByAlbum = new HashMap<>();
        Map<String, List<AlbumInfo>> albumsByArtist = new HashMap<>();
        Map<String, Map<String, Long>> spellings = new HashMap<>();

        for (Map.Entry<String, List<LibraryEntry>> group : grouped.entrySet()) {
            List<LibraryEntry> tracks = group.getValue().stream().sorted(albumOrder()).toList();
            boolean compilation = isCompilation(tracks);
            int year = tracks.stream().mapToInt(LibraryEntry::getYear).filter(y -> y > 0).min().orElse(0);
            AlbumInfo album = new AlbumInfo(group.getKey(), tracks.get(0).getDisplayAlbum(), year, tracks.size(), compilation);
            tracksByAlbum.put(album.key(), tracks);

            // Wem gehört das Album? Normal: dem Anzeige-Interpreten, Sampler: jedem Interpreten + VARIOUS ARTISTS
            Set<String> owners = new HashSet<>();
            for (LibraryEntry e : tracks) {
                String owner = compilation ? e.getTrackArtist() : e.getDisplayArtist();
                String ownerKey = LibraryEntry.toKey(owner);
                spellings.computeIfAbsent(ownerKey, k -> new HashMap<>()).merge(owner, 1L, Long::sum);
                owners.add(ownerKey);
            }
            if (compilation) {owners.add(VARIOUS_KEY);}
            for (String owner : owners) {albumsByArtist.computeIfAbsent(owner, k -> new ArrayList<>()).add(album);}
        }

        // Interpretenliste: häufigste Schreibweise je Schlüssel
        List<String> artists = new ArrayList<>();
        for (Map<String, Long> variants : spellings.values()) {
            artists.add(prettify(Collections.max(variants.entrySet(), Map.Entry.comparingByValue()).getKey()));
        }
        if (albumsByArtist.containsKey(VARIOUS_KEY) && !spellings.containsKey(VARIOUS_KEY)) {artists.add(VARIOUS_ARTISTS);}
        artists.sort(textOrder());

        // Albenlisten sortieren (Jahr, dann Name) und unveränderlich machen
        Comparator<AlbumInfo> albumSort = Comparator.comparingInt((AlbumInfo a) -> a.year() == 0 ? Integer.MAX_VALUE : a.year())
                                                    .thenComparing(AlbumInfo::name, textOrder());
        Map<String, List<AlbumInfo>> frozen = new HashMap<>();
        albumsByArtist.forEach((artist, albums) -> {
            albums.sort(albumSort);
            frozen.put(artist, List.copyOf(albums));
        });

        long total = entries.stream().mapToLong(LibraryEntry::getDuration).sum();
        return new Index(entries, total, List.copyOf(artists), Map.copyOf(frozen), Map.copyOf(tracksByAlbum));
    }

    private static String albumKey(LibraryEntry e) {
        String folder = String.valueOf(new File(e.getPath()).getParent());
        return LibraryEntry.toKey(folder) + "|" + LibraryEntry.toKey(e.getDisplayAlbum());
    }

    /** Sampler: Album-Interpret wie "Various Artists" oder – ohne Album-Interpret – kein Interpret stellt die Hälfte der Titel. */
    private static boolean isCompilation(List<LibraryEntry> tracks) {
        boolean hasAlbumArtist = false;
        for (LibraryEntry e : tracks) {
            String albumArtist = e.getAlbumArtist();
            if (albumArtist == null || albumArtist.isBlank()) {continue;}
            if (isVariousArtists(albumArtist)) {return true;}
            hasAlbumArtist = true;
        }
        if (hasAlbumArtist) {return false;}
        Map<String, Long> counts = tracks.stream()
                .collect(Collectors.groupingBy(e -> LibraryEntry.toKey(e.getTrackArtist()), Collectors.counting()));
        long largest = Collections.max(counts.values());
        return largest * 2 < tracks.size();
    }

    // HILFSMETHODEN ++++++++++==========++++++++++
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