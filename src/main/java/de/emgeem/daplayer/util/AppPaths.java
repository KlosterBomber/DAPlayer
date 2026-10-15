package de.emgeem.daplayer.util;

import java.io.File;

public class AppPaths {

    private AppPaths() {}

    /**
     * Plattformabhängiges Konfigurationsverzeichnis:
     * Windows: %APPDATA%\emgeem\DAPlayer
     * macOS:   ~/Library/Application Support/emgeem/DAPlayer
     * Linux:   $XDG_CONFIG_HOME/emgeem/DAPlayer (bzw. ~/.config/emgeem/DAPlayer)
     */
    public static File getConfigDirectory() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home");
        File base;
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            base = appData != null && !appData.isBlank() ? new File(appData) : new File(home, "AppData/Roaming");
        } else if (os.contains("mac")) {
            base = new File(home, "Library/Application Support");
        } else {
            String xdg = System.getenv("XDG_CONFIG_HOME");
            base = xdg != null && !xdg.isBlank() ? new File(xdg) : new File(home, ".config");
        }
        File configDir = new File(base, "emgeem/DAPlayer");
        configDir.mkdirs();
        return configDir;
    }
}
