package de.emgeem.daplayer.ui.display;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import de.emgeem.daplayer.model.Track;
import javafx.scene.image.Image;

/**
 * Liefert Cover-Bilder mit kleinem LRU-Cache. Früher wurde das Cover bei jedem Neuzeichnen
 * (~60x pro Sekunde während der Wiedergabe) erneut aus der Datei gelesen.
 */
public class ArtworkManager {

    private static final int CACHE_SIZE = 16;
    private static final String[] FOLDER_IMAGES = {"folder.jpg", "folder.png", "cover.jpg", "cover.png", "front.jpg", "front.png", "albumart.jpg"};

    private final Map<String, Image> cache = new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {return size() > CACHE_SIZE;}
    };
    private Image defaultArtwork;

    public Image getArtwork(Track track) {
        if (track == null) {return null;}
        if (track.getFile() == null) {return getDefaultArtwork();}
        String key = track.getLocation();
        Image cached = cache.get(key);
        if (cached != null) {return cached;}
        Image image = getEmbeddedArtwork(track.getFile());
        if (image == null) {image = getFolderArtwork(track.getFile());}
        if (image == null) {image = getDefaultArtwork();}
        cache.put(key, image);
        return image;
    }

    private Image getEmbeddedArtwork(File file) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            if (tag == null) {return null;}
            Artwork artwork = tag.getFirstArtwork();
            if (artwork == null || artwork.getBinaryData() == null) {return null;}
            Image image = new Image(new java.io.ByteArrayInputStream(artwork.getBinaryData()), 500, 500, true, true);
            return image.isError() ? null : image;
        } catch (Exception | LinkageError e) {return null;}
    }

    private Image getFolderArtwork(File file) {
        File folder = file.getParentFile();
        if (folder == null) {return null;}
        for (String name : FOLDER_IMAGES) {
            File artworkFile = new File(folder, name);
            if (artworkFile.isFile()) {
                Image image = new Image(artworkFile.toURI().toString(), 500, 500, true, true);
                if (!image.isError()) {return image;}
            }
        }
        return null;
    }

    private Image getDefaultArtwork() {
        if (defaultArtwork == null) {
            defaultArtwork = new Image(getClass().getResource("/img/DAP_logo_01.png").toExternalForm());
        }
        return defaultArtwork;
    }
}
