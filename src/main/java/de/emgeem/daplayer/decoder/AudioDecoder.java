package de.emgeem.daplayer.decoder;

import java.io.File;

import de.emgeem.daplayer.model.Track;

public interface AudioDecoder {

    boolean supports(File file);
    Track readTrack(File file);
}
