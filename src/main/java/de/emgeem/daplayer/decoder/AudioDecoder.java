package de.emgeem.daplayer.decoder;

import java.io.File;

import javax.sound.sampled.AudioFormat;

import de.emgeem.daplayer.model.Track;

/**
 * Ein Decoder liefert immer 16-Bit signed little-endian PCM.
 * Für jede Wiedergabe wird eine eigene Instanz erzeugt (siehe DecoderManager).
 */
public interface AudioDecoder {

    boolean supports(File file);
    Track readTrack(File file);
    void open(File file) throws Exception;
    /** Liefert die Anzahl geschriebener Bytes (vollständige Frames) oder -1 am Ende. */
    int decode(byte[] buffer) throws Exception;
    AudioFormat getAudioFormat();
    void seekToMillis(long positionMillis) throws Exception;
    void reset() throws Exception;
    void close() throws Exception;

    /** true, wenn der Decoder URLs (Netzwerk-Streams) öffnen kann. */
    default boolean supportsStreams() {return false;}

    default void openStream(String url) throws Exception {
        throw new UnsupportedOperationException("Streams werden von diesem Decoder nicht unterstützt");
    }

    /** false für Live-Streams ohne Seek-Möglichkeit. */
    default boolean isSeekable() {return true;}
}
