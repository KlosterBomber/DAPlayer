package de.emgeem.daplayer.decoder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.emgeem.daplayer.model.Track;

public class DecoderManager {

    private final List<AudioDecoder> decoders = new ArrayList<>();

    public DecoderManager() {decoders.add(new WavDecoder());}

    public Track readTrack(File file) {
        for (AudioDecoder decoder : decoders) {if (decoder.supports(file)) {return decoder.readTrack(file);}}
        return new Track(file);
    }
}