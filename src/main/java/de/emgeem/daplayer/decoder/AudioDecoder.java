package de.emgeem.daplayer.decoder;

import java.io.File;

import javax.sound.sampled.AudioFormat;

import de.emgeem.daplayer.model.Track;

public interface AudioDecoder {

    boolean supports(File file);
    Track readTrack(File file);
    void open(File file) throws Exception;
    int decode(byte[] buffer) throws Exception;
    AudioFormat getAudioFormat();
    void reset() throws Exception;
    void close() throws Exception;
}
