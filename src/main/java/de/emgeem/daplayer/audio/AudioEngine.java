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
    private final Equalizer equalizer;

    private AudioDecoder decoder;
    private SourceDataLine line;
    private Thread playbackThread;
    private Consumer<Track> trackFinishedListener;

    private volatile Long requestedSeekMillis;
    private volatile boolean playing = false;
    private volatile boolean paused = false;
    private float volume = 1.0f;
    private boolean muted = false;

    private long currentPositionMillis;
    private long totalDurationMillis;

    public AudioEngine(AudioDeviceManager deviceManager, DecoderManager decoderManager, Equalizer equalizer) {
        this.deviceManager = deviceManager;
        this.decoderManager = decoderManager;
        this.equalizer = equalizer;
    }

    public boolean isPlaying() {return playing;}
    public boolean isPaused() {return paused;}
    public void setTrackFinishedListener(Consumer<Track> listener) {this.trackFinishedListener = listener;}

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
            playbackThread = new Thread(() -> runPlayback(track), "DAPlayer-Playback");
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

    private void runPlayback(Track track) {
        byte[] buffer = new byte[8192];
        boolean naturalEnd = false;
        try {
            while (playing && !Thread.currentThread().isInterrupted()) {
                Long seekPosition = requestedSeekMillis;
                if (seekPosition != null) {
                    requestedSeekMillis = null;
                    decoder.seekToMillis(seekPosition);
                    currentPositionMillis = seekPosition;
                    continue;
                }
                if (paused) {
                    Thread.sleep(10);
                    continue;
                }
                int read = decoder.decode(buffer);
                if (read <= 0) {
                    naturalEnd = true;
                    break;
                }
                SourceDataLine currentLine = line;
                if (currentLine == null) {break;}
                equalizer.process(buffer, read, currentLine.getFormat());
                currentLine.write(buffer, 0, read);
                currentPositionMillis += bytesToMillis(read, currentLine.getFormat());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            if (playing) {e.printStackTrace();}
        } finally {
            boolean wasNaturalEnd = naturalEnd && playing;
            playing = false;
            paused = false;
            cleanup(false);
            if (wasNaturalEnd && trackFinishedListener != null) {trackFinishedListener.accept(track);}
        }
    }

    public void pause() {if (playing) {paused = true;}}

    public void resume() {if (playing) {paused = false;}}

    public void stop() {
        playing = false;
        paused = false;
        Thread thread = playbackThread;
        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();
            try {thread.join(1000);}
            catch (InterruptedException e) {Thread.currentThread().interrupt();}
        }
        cleanup(true);
        playbackThread = null;
    }

    private synchronized void cleanup(boolean immediate) {
        SourceDataLine currentLine = line;
        line = null;
        AudioDecoder currentDecoder = decoder;
        decoder = null;
        try {
            if (currentLine != null) {
                if (immediate) {
                    currentLine.stop();
                    currentLine.flush();
                } else {
                    currentLine.drain();
                    currentLine.stop();
                }
                currentLine.close();
            }
        } catch (Exception ignored) {}
        try {
            if (currentDecoder != null) {
                currentDecoder.close();
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

    private long bytesToMillis(int bytes, AudioFormat format) {
        float bytesPerSecond = format.getFrameRate() * format.getFrameSize();
        if (bytesPerSecond <= 0) {return 0;}
        return (long) ((bytes / bytesPerSecond) * 1000.0);
    }

    public long getCurrentPositionMillis() {return currentPositionMillis;}
    public long getTotalDurationMillis() {return totalDurationMillis;}

    public void seekToMillis(long positionMillis) {
        if (!playing || decoder == null) {return;}
        long target = Math.max(0, Math.min(positionMillis, totalDurationMillis));
        requestedSeekMillis = target;
        currentPositionMillis = target;
        if (line != null) {line.flush();}
    }
}
