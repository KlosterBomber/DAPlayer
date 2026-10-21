package de.emgeem.daplayer.library;

import de.emgeem.daplayer.decoder.DecoderManager;
import de.emgeem.daplayer.model.LibraryEntry;

public class MediaLibraryDemo {

    public static void main(String[] args) throws Exception {
        MediaLibrary library = new MediaLibrary(new LibraryStore(), new LibraryScanner(new DecoderManager()));
        library.load();
// String path = "C:/Users/admin/Music";
// System.out.println(path + " existiert: " + new java.io.File(path).isDirectory());
        if (library.getFolders().isEmpty()) {
            library.addFolder("C:/Users/admin/Music");   // anpassen!
        }
        LibraryScanner.ScanResult result = library.rescan(null);
        library.save();
        System.out.println("Scan: " + result.durationMillis() + " ms, " + library.getTrackCount() + " Tracks");
        System.out.println();

        for (String artist : library.getArtists()) {
            System.out.println(artist);
            for (MediaLibrary.AlbumInfo album : library.getAlbums(artist)) {
                String year = album.year() > 0 ? album.year() + "  " : "      ";
                System.out.println("    " + year + album.name() + "  (" + album.trackCount() + ")");
                for (LibraryEntry track : library.getTracks(artist, album.name())) {
                    System.out.println("        " + track.getTrackNumber() + ". " + track.getDisplayTitle());
                }
            }
        }

        System.out.println();
        System.out.println("Suche 'prodigy':");
        for (LibraryEntry hit : library.search("prodigy")) {
            System.out.println("    " + hit.getDisplayArtist() + " - " + hit.getDisplayTitle());
        }
    }
}