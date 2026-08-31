package de.emgeem.daplayer.ui.display;

import java.io.File;

import de.emgeem.daplayer.model.Track;
import javafx.scene.image.Image;

public class ArtworkManager {

    public Image getArtwork(Track track) {
        if (track == null || track.getFile() == null) {return null;}
        File folder = track.getFile().getParentFile();
        if (folder == null) {return null;}
        File artworkFile = new File(folder, "folder.jpg");
        if (!artworkFile.isFile()) {return null;}
        return new Image(artworkFile.toURI().toString());
    }
}