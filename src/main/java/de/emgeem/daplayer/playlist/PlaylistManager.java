package de.emgeem.daplayer.playlist;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import de.emgeem.daplayer.decoder.DecoderManager;
import de.emgeem.daplayer.model.Playlist;
import de.emgeem.daplayer.model.Track;
import de.emgeem.daplayer.util.AppPaths;

public class PlaylistManager {

    public void save(Playlist playlist, File file) throws IOException {
        List<String> paths = playlist.getTracks().stream().map(track -> track.getFile().getAbsolutePath()).toList();
        Files.write(file.toPath(), paths);
    }

    public void load(Playlist playlist, File file, DecoderManager decoderManager) throws IOException {
        List<String> paths = Files.readAllLines(file.toPath());
        playlist.clear();
        for (String path : paths) {
            if (path.isBlank()) {continue;}
            File audioFile = new File(path);
            if (!audioFile.isFile()) {continue;}
            Track track = decoderManager.readTrack(audioFile);
            if (track != null) {playlist.addTrack(track);}
        }
    }

    public File getPlaylistDirectory() {return AppPaths.getConfigDirectory();}
}
