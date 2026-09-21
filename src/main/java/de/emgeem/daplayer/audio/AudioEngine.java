package de.emgeem.daplayer.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import de.emgeem.daplayer.decoder.AudioDecoder;
import de.emgeem.daplayer.decoder.DecoderManager;
import de.emgeem.daplayer.model.Track;

public class AudioEngine {

    private final AudioDeviceManager deviceManager;
    private final DecoderManager decoderManager;
    private final List<Consumer<Float>> volumeListeners = new ArrayList<>();

    private AudioDecoder decoder;
    private SourceDataLine line;
    private Thread playbackThread;

    private volatile boolean playing = false;
    private volatile boolean paused = false;
    private float volume = 1.0f;
    private boolean muted = false;

    private long currentPositionMillis;
    private long totalDurationMillis;

    public AudioEngine(AudioDeviceManager deviceManager, DecoderManager decoderManager) {
        this.deviceManager = deviceManager;
        this.decoderManager = decoderManager;
    }

    public boolean isPlaying() {return playing;}
    public boolean isPaused() {return paused;}

    public void play(Track track) {
        stop(); // immer zuerst stoppen
        try {

            totalDurationMillis = track.getDuration();
            currentPositionMillis = 0;

            decoder = decoderManager.openDecoder(track.getFile());
            AudioFormat sourceFormat = decoder.getAudioFormat();
            AudioFormat playbackFormat = normalizeFormat(sourceFormat);
            line = openLine(playbackFormat);
            setVolume(volume);
            playing = true;
            paused = false;
            playbackThread = new Thread(this::runPlayback, "DAPlayer-Playback");
            playbackThread.setDaemon(true);
            playbackThread.start();
        } catch (Exception e) {
            e.printStackTrace();
            stop();
        }
    }

    private AudioFormat normalizeFormat(AudioFormat f) {
        if (f.getSampleSizeInBits() == 16 && f.getEncoding() == AudioFormat.Encoding.PCM_SIGNED && !f.isBigEndian()) {return f;}
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, f.getSampleRate(), 16, f.getChannels(), f.getChannels() * 2, f.getSampleRate(), false);
    }

    private SourceDataLine openLine(AudioFormat format) throws Exception {
        Mixer.Info device = deviceManager.getSelectedDevice();
        if (device == null) {throw new IllegalStateException("Kein Audiogerät ausgewählt");}
        Mixer mixer = AudioSystem.getMixer(device);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        if (!mixer.isLineSupported(info)) {throw new IllegalArgumentException("Gerät unterstützt das Format nicht: " + device.getName());}
        SourceDataLine line = (SourceDataLine) mixer.getLine(info);
        line.open(format);
        line.start();
        return line;
    }

    private void runPlayback() {
        byte[] buffer = new byte[8192];
        try {
            while (playing) {
                if (paused) {
                    Thread.sleep(10);
                    continue;
                }
                int read = decoder.decode(buffer);
                if (read <= 0) break;
                line.write(buffer, 0, read);
                currentPositionMillis += bytesToMillis(read);
            }
        } catch (Exception e) {e.printStackTrace();}
        cleanup();
    }

    public void pause() {if (playing) {paused = true;}}

    public void resume() {if (playing) {paused = false;}}

    public void stop() {
        playing = false;
        paused = false;
        cleanup();
    }

    private void cleanup() {
        try {
            if (line != null) {
                line.drain();
                line.stop();
                line.close();
                line = null;
            }
            if (decoder != null) {
                decoder.close();
                decoder = null;
            }
        } catch (Exception ignored) {}
    }

    public void setVolume(float volume) {
        this.volume = volume;
        applyVolume();
        for (Consumer<Float> listener : volumeListeners) {listener.accept(volume);}
    }
    public float getVolume() {return volume;}
    private void applyVolume() {
        if (line == null) {return;}
        FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        if (muted || volume <= 0f) {
            gain.setValue(gain.getMinimum());
        } else {
            float dB = (float) (20.0 * Math.log10(volume));
            dB = Math.max(dB, gain.getMinimum());
            gain.setValue(dB);
        }
    }
    public void setMuted(boolean muted) {
        this.muted = muted;
        applyVolume();
    }
    public boolean isMuted() {return muted;}

    private long bytesToMillis(int bytes) {
        if (line == null) return 0;
        AudioFormat f = line.getFormat();
        float bytesPerSecond = f.getFrameRate() * f.getFrameSize();
        return (long) ((bytes / bytesPerSecond) * 1000.0);
    }

    public long getCurrentPositionMillis() {return currentPositionMillis;}
    public long getTotalDurationMillis() {return totalDurationMillis;}
}
