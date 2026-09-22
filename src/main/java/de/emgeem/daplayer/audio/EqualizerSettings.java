package de.emgeem.daplayer.audio;

import java.util.prefs.Preferences;

public final class EqualizerSettings {

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(EqualizerSettings.class);

    private static final String KEY_PREFIX = "band_";

    private EqualizerSettings() {}

    public static void load(Equalizer equalizer) {
        for (int i = 0; i < equalizer.getBandCount(); i++) {
            double gain = PREFS.getDouble(KEY_PREFIX + i, 0.0);
            equalizer.setBandGain(i, gain);
        }
    }

    public static void save(Equalizer equalizer) {
        for (int i = 0; i < equalizer.getBandCount(); i++) {
            PREFS.putDouble(KEY_PREFIX + i, equalizer.getBandGain(i));
        }
    }
}