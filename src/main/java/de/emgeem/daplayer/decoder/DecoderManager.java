package de.emgeem.daplayer.decoder;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

import de.emgeem.daplayer.model.Track;

public class DecoderManager {

    /** Alle Dateiendungen, die DAPlayer abspielen kann (klein, ohne Punkt). */
    public static final Set<String> SUPPORTED_EXTENSIONS = new LinkedHashSet<>(List.of(
        "wav", "mp3", "flac", "wma", "ogg", "oga", "opus", "m4a", "aac", "aiff", "aif", "ape", "wv", "mpc"
    ));

    private final List<Supplier<AudioDecoder>> factories = new ArrayList<>();
    /** Prototypen nur für supports()/readTrack() – zustandslos benutzt. */
    private final List<AudioDecoder> prototypes = new ArrayList<>();

    public DecoderManager() {
        register(WavDecoder::new);
        register(Mp3Decoder::new);
        register(FfmpegDecoder::new);
    }

    /** Für Tests bzw. Erweiterungen. */
    public DecoderManager(List<Supplier<AudioDecoder>> factories) {
        factories.forEach(this::register);
    }

    private void register(Supplier<AudioDecoder> factory) {
        factories.add(factory);
        prototypes.add(factory.get());
    }

    public static boolean isSupportedFile(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 && SUPPORTED_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    public Track readTrack(File file) {
        for (AudioDecoder decoder : prototypes) {
            if (decoder.supports(file)) {
                Track track = decoder.readTrack(file);
                if (track != null) {return track;}
            }
        }
        return new Track(file);
    }

    public Track readStream(String url) {return new Track(url);}

    /** Erzeugt eine neue, noch nicht geöffnete Decoder-Instanz. */
    public AudioDecoder getDecoder(File file) {
        for (int i = 0; i < prototypes.size(); i++) {
            if (prototypes.get(i).supports(file)) {return factories.get(i).get();}
        }
        return null;
    }

    /** Öffnet einen neuen Decoder für Datei oder Stream des Tracks. */
    public AudioDecoder openDecoder(Track track) throws Exception {
        if (track.isStream()) {
            for (int i = 0; i < prototypes.size(); i++) {
                if (prototypes.get(i).supportsStreams()) {
                    AudioDecoder decoder = factories.get(i).get();
                    try {
                        decoder.openStream(track.getLocation());
                    } catch (Exception | LinkageError e) {
                        try {decoder.close();} catch (Exception | LinkageError ignored) {}
                        throw e;
                    }
                    return decoder;
                }
            }
            throw new IllegalArgumentException("Kein Decoder für Streams verfügbar");
        }
        return openDecoder(track.getFile());
    }

    public AudioDecoder openDecoder(File file) throws Exception {
        AudioDecoder decoder = getDecoder(file);
        if (decoder == null) {
            throw new IllegalArgumentException("Kein Decoder für Datei: " + file.getName());
        }
        try {
            decoder.open(file);
        } catch (Exception | LinkageError e) {
            try {decoder.close();} catch (Exception | LinkageError ignored) {}
            throw e;
        }
        return decoder;
    }
}
