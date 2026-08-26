package de.emgeem.daplayer.ui.painter;

import java.util.prefs.Preferences;

import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

public final class UiColors {

    private static final Preferences PREFS = Preferences.userNodeForPackage(UiColors.class);
    private static final String ACCENT_KEY = "accentColor";
    private static final Color DEFAULT_ACCENT = Color.web("#88ff39");
    private static Color accent = loadAccent();
    private UiColors() {}
    public static Color getAccent() {return accent;}
    public static void setAccent(Color color) {
        accent = color;
        saveAccent(color);
    }
    private static Color loadAccent() {
        String value = PREFS.get(ACCENT_KEY, null);
        if (value == null) {return DEFAULT_ACCENT;}try {return Color.web(value);} catch (IllegalArgumentException e) {return DEFAULT_ACCENT;}
    }
    private static void saveAccent(Color color) {PREFS.put(ACCENT_KEY, color.toString());}
    public static Color accentDark() {return accent.deriveColor(0, 1.0, 0.5, 1.0);}
    public static Color accentLight() {return accent.interpolate(Color.WHITE, 0.75);}
    public static DropShadow displayGlow() {
        DropShadow glow = new DropShadow();
        glow.setBlurType(BlurType.GAUSSIAN);
        glow.setRadius(8);
        glow.setSpread(0.25);
        glow.setColor(accent);
        return glow;
    }
}