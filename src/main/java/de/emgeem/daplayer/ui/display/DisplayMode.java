package de.emgeem.daplayer.ui.display;

import de.emgeem.daplayer.model.Playlist;
import javafx.scene.layout.StackPane;

public abstract class DisplayMode extends StackPane {

    public abstract void draw();

    public abstract void setPlaylist(Playlist playlist);
}