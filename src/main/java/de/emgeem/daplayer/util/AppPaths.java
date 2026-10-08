package de.emgeem.daplayer.util;

import java.io.File;

public class AppPaths {

    private AppPaths() {}

    public static File getConfigDirectory() {
        File configDir = new File(System.getProperty("user.home"), "AppData/Roaming/emgeem/DAPlayer");
        configDir.mkdirs();
        return configDir;
    }
}
