package de.emgeem.daplayer.playlist;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.emgeem.daplayer.decoder.DecoderManager;
import de.emgeem.daplayer.model.Playlist;
import de.emgeem.daplayer.model.Track;
import de.emgeem.daplayer.util.AppPaths;

/**
 * Speichert/lädt Playlists. Formate:
 * <ul>
 *   <li>.daplist – ein absoluter Pfad bzw. eine URL pro Zeile (UTF-8)</li>
 *   <li>.m3u / .m3u8 – Extended M3U, relative Pfade werden aufgelöst</li>
 * </ul>
 */
public class PlaylistManager {

    private static final String SESSION_FILE = "last-session.daplist";

    public void save(Playlist playlist, File file) throws IOException {
        List<String> lines = new ArrayList<>();
        boolean m3u = isM3u(file);
        if (m3u) {lines.add("#EXTM3U");}
        for (Track track : playlist.getTracks()) {
            if (m3u) {
                long seconds = track.getDuration() > 0 ? track.getDuration() / 1000 : -1;
                String artist = track.getArtist() != null && !track.getArtist().isBlank() ? track.getArtist() + " - " : "";
                lines.add("#EXTINF:" + seconds + "," + artist + track.getDisplayTitle());
            }
            lines.add(track.getLocation());
        }
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
    }

    /** Liest nur die Einträge (Pfad/URL), ohne Metadaten – schnell, für Hintergrund-Laden. */
    public List<String> readEntries(File file) throws IOException {
        List<String> raw;
        try {
            raw = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (java.nio.charset.MalformedInputException e) {
            raw = Files.readAllLines(file.toPath(), StandardCharsets.ISO_8859_1); // alte .m3u-Dateien
        }
        File baseDir = file.getAbsoluteFile().getParentFile();
        List<String> entries = new ArrayList<>();
        for (String line : raw) {
            line = line.strip();
            if (line.startsWith("﻿")) {line = line.substring(1);}
            if (line.isEmpty() || line.startsWith("#")) {continue;}
            if (Track.isUrl(line)) {
                if (line.toLowerCase(Locale.ROOT).startsWith("file://")) {
                    try {entries.add(new File(java.net.URI.create(line)).getAbsolutePath());} catch (Exception ignored) {}
                } else {
                    entries.add(line);
                }
                continue;
            }
            File audioFile = new File(line);
            if (!audioFile.isAbsolute() && baseDir != null) {audioFile = new File(baseDir, line);}
            if (audioFile.isFile()) {entries.add(audioFile.getAbsolutePath());}
        }
        return entries;
    }

    public Track createTrack(String entry, DecoderManager decoderManager) {
        return Track.isUrl(entry) ? decoderManager.readStream(entry) : decoderManager.readTrack(new File(entry));
    }

    public void load(Playlist playlist, File file, DecoderManager decoderManager) throws IOException {
        List<String> entries = readEntries(file);
        playlist.clear();
        for (String entry : entries) {
            Track track = createTrack(entry, decoderManager);
            if (track != null) {playlist.addTrack(track);}
        }
    }

    public static boolean isM3u(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".m3u") || name.endsWith(".m3u8");
    }

    public File getPlaylistDirectory() {return AppPaths.getConfigDirectory();}

    /** Datei, in der die aktuelle Playlist beim Beenden gesichert wird. */
    public File getSessionFile() {return new File(AppPaths.getConfigDirectory(), SESSION_FILE);}
}
