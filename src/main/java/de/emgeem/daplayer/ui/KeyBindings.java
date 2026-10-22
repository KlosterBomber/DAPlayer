package de.emgeem.daplayer.ui;

import java.util.List;

/** Tastaturbelegung – Grundlage für die Anzeige unter SETTINGS / KEY CONTROLS. */
public final class KeyBindings {

    public record Binding(String keys, String action) {}

    public static final List<Binding> ALL = List.of(
        new Binding("SPACE", "PLAY / PAUSE"),
        new Binding("S", "STOP"),
        new Binding("N  /  PAGE DOWN", "NEXT TRACK"),
        new Binding("B  /  PAGE UP", "PREVIOUS TRACK"),
        new Binding("LEFT / RIGHT", "SEEK -5 / +5 S  (SHIFT: 30 S)"),
        new Binding("+ / -", "VOLUME UP / DOWN"),
        new Binding("UP / DOWN", "VOLUME  (PLAYLIST: SELECT)"),
        new Binding("ENTER", "PLAY SELECTED TRACK"),
        new Binding("DELETE", "REMOVE SELECTED TRACK"),
        new Binding("M", "MUTE"),
        new Binding("H  /  R", "SHUFFLE  /  REPEAT"),
        new Binding("1 - 6", "LIST/INFO/LIBRARY/SETTINGS/VISUALS/EQ"),
        new Binding("CTRL+O / CTRL+SHIFT+O", "ADD FILES / ADD FOLDER"),
        new Binding("CTRL+L", "ADD LOCATION (URL)"),
        new Binding("CTRL+S / CTRL+Q", "SAVE PLAYLIST / QUIT")
    );

    private KeyBindings() {}
}
