package de.emgeem.daplayer.audio;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import de.emgeem.daplayer.decoder.AudioDecoder;
import de.emgeem.daplayer.decoder.DecoderManager;
import de.emgeem.daplayer.model.Track;

/**
 * Wiedergabe-Engine. Jede Wiedergabe läuft in einer eigenen {@link Session} mit eigenem
 * Thread, Decoder und Audio-Line. Öffnen von Datei/Stream geschieht im Wiedergabe-Thread,
 * damit langsame Netzwerk-Streams die Oberfläche nicht blockieren.
 */
public class AudioEngine {

    private static final int DECODE_BUFFER_BYTES = 8192;
    private static final double LINE_BUFFER_SECONDS = 0.3;

    private final AudioDeviceManager deviceManager;
    private final DecoderManager decoderManager;
    private final Equalizer equalizer;

    private volatile Session session;
    private volatile Consumer<Track> trackFinishedListener;
    private volatile BiConsumer<Track, Exception> errorListener;
    private volatile VisualizationListener visualizationListener;

    private volatile float volume = 1.0f;
    private volatile boolean muted = false;

    public AudioEngine(AudioDeviceManager deviceManager, DecoderManager decoderManager, Equalizer equalizer) {
        this.deviceManager = deviceManager;
        this.decoderManager = decoderManager;
        this.equalizer = equalizer;
    }

    public boolean isPlaying() {Session s = session; return s != null && s.running;}
    public boolean isPaused() {Session s = session; return s != null && s.running && s.paused;}
    public Track getCurrentTrack() {Session s = session; return s != null && s.running ? s.track : null;}
    public void setTrackFinishedListener(Consumer<Track> listener) {this.trackFinishedListener = listener;}
    /** Wird aus dem Wiedergabe-Thread aufgerufen, wenn ein Track nicht abgespielt werden kann. */
    public void setErrorListener(BiConsumer<Track, Exception> listener) {this.errorListener = listener;}
    public void setVisualizationListener(VisualizationListener listener) {this.visualizationListener = listener;}

    public void play(Track track) {play(track, 0);}

    /** Startet die Wiedergabe ab startMillis (z. B. nach Gerätewechsel). */
    public void play(Track track, long startMillis) {
        stop();
        Session s = new Session(track, Math.max(0, startMillis));
        session = s;
        Thread thread = new Thread(s, "DAPlayer-Playback");
        thread.setDaemon(true);
        s.thread = thread;
        thread.start();
    }

    public void pause() {
        Session s = session;
        if (s == null || !s.running) {return;}
        s.paused = true;
        SourceDataLine line = s.line;
        if (line != null) {line.stop();}
    }

    public void resume() {
        Session s = session;
        if (s == null || !s.running) {return;}
        s.paused = false;
        SourceDataLine line = s.line;
        if (line != null) {line.start();}
    }

    public void stop() {
        Session s = session;
        session = null;
        if (s == null) {return;}
        s.running = false;
        SourceDataLine line = s.line;
        if (line != null) {
            try {line.stop(); line.flush();} catch (Exception ignored) {}
        }
        Thread thread = s.thread;
        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();
            try {thread.join(1000);}
            catch (InterruptedException e) {Thread.currentThread().interrupt();}
        }
    }

    // ============================================================
    // LAUTSTÄRKE
    // ============================================================

    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
        Session s = session;
        if (s != null) {s.applyVolume();}
    }
    public float getVolume() {return volume;}

    public void setMuted(boolean muted) {
        this.muted = muted;
        Session s = session;
        if (s != null) {s.applyVolume();}
    }
    public boolean isMuted() {return muted;}

    // ============================================================
    // POSITION / SEEK
    // ============================================================

    public long getCurrentPositionMillis() {
        Session s = session;
        return s != null ? s.positionMillis() : 0;
    }

    public long getTotalDurationMillis() {
        Session s = session;
        return s != null ? s.track.getDuration() : 0;
    }

    /** false bei Live-Streams. */
    public boolean isSeekable() {
        Session s = session;
        return s != null && s.seekable && s.track.getDuration() > 0;
    }

    public void seekToMillis(long positionMillis) {
        Session s = session;
        if (s == null || !s.running || !s.seekable) {return;}
        long duration = s.track.getDuration();
        long target = Math.max(0, duration > 0 ? Math.min(positionMillis, duration) : positionMillis);
        s.seekRequest = target;
        SourceDataLine line = s.line;
        if (line != null) {
            try {line.flush();} catch (Exception ignored) {} // blockierendes write() freigeben
        }
    }

    // ============================================================
    // SESSION
    // ============================================================

    private final class Session implements Runnable {

        final Track track;
        volatile boolean running = true;
        volatile boolean paused = false;
        volatile Long seekRequest;
        volatile SourceDataLine line;
        volatile boolean seekable = true;
        volatile Thread thread;

        private volatile long basePositionMillis;
        private volatile long framesWritten;
        private float frameRate;
        private int frameSize;
        private FloatControl gainControl;
        private volatile float softwareGain = 1f;

        Session(Track track, long startMillis) {
            this.track = track;
            this.basePositionMillis = startMillis;
            if (startMillis > 0) {seekRequest = startMillis;}
        }

        long positionMillis() {
            Long pendingSeek = seekRequest;
            if (pendingSeek != null) {return pendingSeek;}
            SourceDataLine l = line;
            if (l == null || frameRate <= 0) {return basePositionMillis;}
            long buffered = 0;
            try {buffered = Math.max(0, (l.getBufferSize() - l.available()) / frameSize);} catch (Exception ignored) {}
            long played = Math.max(0, framesWritten - buffered);
            long position = basePositionMillis + (long) (played * 1000.0 / frameRate);
            long duration = track.getDuration();
            return duration > 0 ? Math.min(position, duration) : position;
        }

        void applyVolume() {
            float v = volume;
            boolean m = muted;
            FloatControl gain = gainControl;
            if (gain != null) {
                try {
                    if (m || v <= 0f) {
                        gain.setValue(gain.getMinimum());
                    } else {
                        float dB = (float) (20.0 * Math.log10(v));
                        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
                    }
                    softwareGain = 1f;
                    return;
                } catch (Exception ignored) {}
            }
            softwareGain = m ? 0f : v;
        }

        @Override
        public void run() {
            AudioDecoder decoder = null;
            SourceDataLine localLine = null;
            boolean naturalEnd = false;
            try {
                decoder = decoderManager.openDecoder(track);
                if (!running) {return;}
                seekable = decoder.isSeekable();
                AudioFormat format = decoder.getAudioFormat();
                localLine = openLine(format);
                frameRate = format.getFrameRate();
                frameSize = format.getFrameSize();
                gainControl = localLine.isControlSupported(FloatControl.Type.MASTER_GAIN)
                        ? (FloatControl) localLine.getControl(FloatControl.Type.MASTER_GAIN) : null;
                line = localLine;
                applyVolume();
                if (!paused) {localLine.start();}

                byte[] buffer = new byte[DECODE_BUFFER_BYTES - DECODE_BUFFER_BYTES % frameSize];
                while (running) {
                    Long seekPosition = seekRequest;
                    if (seekPosition != null) {
                        localLine.flush();
                        if (seekable) {decoder.seekToMillis(seekPosition);}
                        framesWritten = 0;
                        basePositionMillis = seekPosition;
                        if (seekRequest == seekPosition) {seekRequest = null;}
                        continue;
                    }
                    if (paused) {
                        Thread.sleep(10);
                        continue;
                    }
                    int read = decoder.decode(buffer);
                    if (read < 0) {
                        naturalEnd = true;
                        break;
                    }
                    if (read == 0) {continue;}
                    equalizer.process(buffer, read, format);
                    VisualizationListener vis = visualizationListener;
                    if (vis != null) {vis.onAudioData(createVisualizationData(buffer, read, format));}
                    float gain = softwareGain;
                    if (gain < 0.999f) {applySoftwareGain(buffer, read, gain);}
                    int written = 0;
                    while (written < read && running && seekRequest == null) {
                        written += localLine.write(buffer, written, read - written);
                        if (paused && written < read) {Thread.sleep(10);}
                    }
                    framesWritten += written / frameSize;
                }
                if (naturalEnd && running) {
                    localLine.drain();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception | LinkageError e) {
                if (running) {
                    e.printStackTrace();
                    Exception error = e instanceof Exception ex ? ex
                            : new Exception("Decoder nicht verfügbar (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")", e);
                    BiConsumer<Track, Exception> listener = errorListener;
                    if (listener != null && session == this) {listener.accept(track, error);}
                }
                naturalEnd = false;
            } finally {
                boolean notify = naturalEnd && running && session == this;
                running = false;
                line = null;
                if (localLine != null) {
                    try {localLine.stop(); localLine.flush(); localLine.close();} catch (Exception ignored) {}
                }
                if (decoder != null) {
                    try {decoder.close();} catch (Exception ignored) {}
                }
                Consumer<Track> listener = trackFinishedListener;
                if (notify && listener != null) {listener.accept(track);}
            }
        }
    }

    private SourceDataLine openLine(AudioFormat format) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        Mixer.Info device = deviceManager.getSelectedDevice();
        SourceDataLine line = null;
        if (device != null) {
            Mixer mixer = AudioSystem.getMixer(device);
            if (mixer.isLineSupported(info)) {
                line = (SourceDataLine) mixer.getLine(info);
            } else {
                System.err.println("Gerät unterstützt das Format nicht, nutze Standardgerät: " + device.getName());
            }
        }
        if (line == null) {line = (SourceDataLine) AudioSystem.getLine(info);}
        int bufferBytes = (int) (format.getFrameRate() * format.getFrameSize() * LINE_BUFFER_SECONDS);
        bufferBytes -= bufferBytes % format.getFrameSize();
        line.open(format, Math.max(bufferBytes, DECODE_BUFFER_BYTES * 2));
        return line;
    }

    private static void applySoftwareGain(byte[] buffer, int length, float gain) {
        for (int i = 0; i + 1 < length; i += 2) {
            short s = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xff));
            int v = Math.round(s * gain);
            buffer[i] = (byte) (v & 0xff);
            buffer[i + 1] = (byte) ((v >> 8) & 0xff);
        }
    }

    /** Berechnet Mono-, L/R-Samples sowie Peak/RMS in einem Durchlauf (16 Bit LE). */
    static VisualizationData createVisualizationData(byte[] buffer, int length, AudioFormat format) {
        int channels = format.getChannels();
        int frameSize = format.getFrameSize();
        int frameCount = length / frameSize;
        float[] mono = new float[frameCount];
        float[] left = new float[frameCount];
        float[] right = new float[frameCount];
        double leftPeak = 0, rightPeak = 0, leftSum = 0, rightSum = 0;
        for (int frame = 0; frame < frameCount; frame++) {
            int index = frame * frameSize;
            float l = sample(buffer, index);
            float r = channels > 1 ? sample(buffer, index + 2) : l;
            left[frame] = l;
            right[frame] = r;
            mono[frame] = (l + r) * 0.5f;
            leftPeak = Math.max(leftPeak, Math.abs(l));
            rightPeak = Math.max(rightPeak, Math.abs(r));
            leftSum += l * l;
            rightSum += r * r;
        }
        double leftRms = frameCount > 0 ? Math.sqrt(leftSum / frameCount) : 0.0;
        double rightRms = frameCount > 0 ? Math.sqrt(rightSum / frameCount) : 0.0;
        return new VisualizationData(mono, format.getSampleRate(), leftPeak, rightPeak, leftRms, rightRms, left, right);
    }

    private static float sample(byte[] buffer, int index) {
        short s = (short) ((buffer[index + 1] << 8) | (buffer[index] & 0xff));
        return s / 32768f;
    }
}
