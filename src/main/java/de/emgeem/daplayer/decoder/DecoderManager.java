package de.emgeem.daplayer.decoder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.emgeem.daplayer.model.Track;

public class DecoderManager {

    private final List<AudioDecoder> decoders = new ArrayList<>();

    public DecoderManager() {
        decoders.add(new WavDecoder());
        decoders.add(new Mp3Decoder());
    }

    public Track readTrack(File file) {
        for (AudioDecoder decoder : decoders) {if (decoder.supports(file)) {return decoder.readTrack(file);}}
        return new Track(file);
    }

// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

public AudioDecoder getDecoder(File file) {
    for (AudioDecoder decoder : decoders) {
        if (decoder.supports(file)) {
            return decoder;
        }
    }
    return null;
}

public AudioDecoder openDecoder(File file) throws Exception {
    AudioDecoder decoder = getDecoder(file);
    if (decoder == null) {
        throw new IllegalArgumentException("Kein Decoder für Datei: " + file.getName());
    }
    decoder.open(file);
    return decoder;
}
}