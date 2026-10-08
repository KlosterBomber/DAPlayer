package de.emgeem.daplayer.ui.display;

import java.io.File;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import de.emgeem.daplayer.model.Track;
import javafx.scene.image.Image;

public class ArtworkManager {

    public Image getArtwork(Track track) {
        if (track == null || track.getFile() == null) {return null;}
        Image embeddedArtwork = getEmbeddedArtwork(track.getFile());
        if (embeddedArtwork != null) {return embeddedArtwork;}
        Image folderArtwork = getFolderArtwork(track.getFile());
        if (folderArtwork != null) {return folderArtwork;}
        return getDefaultArtwork();
    }

    private Image getEmbeddedArtwork(File file) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            if (tag == null) {return null;}
            Artwork artwork = tag.getFirstArtwork();
            if (artwork == null || artwork.getBinaryData() == null) {return null;}
            return new Image(new java.io.ByteArrayInputStream(artwork.getBinaryData()));
        } catch (Exception e) {return null;}
    }

    private Image getFolderArtwork(File file) {
        File folder = file.getParentFile();
        if (folder == null) {return null;}
        File artworkFile = new File(folder, "folder.jpg");
        if (!artworkFile.isFile()) {return null;}
        return new Image(artworkFile.toURI().toString());
    }

    private Image getDefaultArtwork() {return new Image(getClass().getResource("/img/DAP_logo_01.png").toExternalForm());}
}