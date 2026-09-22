package de.emgeem.daplayer.audio;

import javax.sound.sampled.AudioFormat;

public class Equalizer {

    private static final double[] FREQUENCIES = {31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000};
    private static final double MIN_GAIN_DB = -12.0;
    private static final double MAX_GAIN_DB = 12.0;

    private final double[] gains = new double[FREQUENCIES.length];

    private Biquad[] filters;
    private float sampleRate;
    private int channels;

    public Equalizer() {reset();}

    public void reset() {for (int i = 0; i < gains.length; i++) {gains[i] = 0.0;}}

    public void setBandGain(int band, double gainDb) {
        if (band < 0 || band >= gains.length) {return;}
        gains[band] = Math.max(MIN_GAIN_DB, Math.min(MAX_GAIN_DB, gainDb));
        if (filters != null) {filters[band].setGain(gains[band], sampleRate);}
    }

    public double getBandGain(int band) {
        if (band < 0 || band >= gains.length) {return 0.0;}
        return gains[band];
    }

    public double getFrequency(int band) {
        if (band < 0 || band >= FREQUENCIES.length) {return 0.0;}
        return FREQUENCIES[band];
    }

    public int getBandCount() {
        return FREQUENCIES.length;
    }

    public void process(byte[] buffer, int length, AudioFormat format) {
        if (buffer == null || length <= 0) {return;}
        if (format.getSampleSizeInBits() != 16 || format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED || format.isBigEndian()) {return;}
        if (sampleRate != format.getSampleRate() || channels != format.getChannels() || filters == null) {
            sampleRate = format.getSampleRate();
            channels = format.getChannels();
            rebuildFilters();
        }
        if (filters == null) {return;}
        int frameSize = format.getFrameSize();
        for (int offset = 0; offset + frameSize <= length; offset += frameSize) {
            for (int channel = 0; channel < channels; channel++) {
                int index = offset + channel * 2;
                short sample = (short) ((buffer[index + 1] << 8) | (buffer[index] & 0xff));
                double value = sample / 32768.0;
                for (Biquad filter : filters) {value = filter.process(value, channel);}
                value = Math.max(-1.0, Math.min(1.0, value));
                short output = (short) Math.round(value * 32767.0);
                buffer[index] = (byte) (output & 0xff);
                buffer[index + 1] = (byte) ((output >> 8) & 0xff);
            }
        }
    }

    private void rebuildFilters() {
        if (sampleRate <= 0 || channels <= 0) {return;}
        filters = new Biquad[FREQUENCIES.length];
        for (int i = 0; i < FREQUENCIES.length; i++) {filters[i] = new Biquad(FREQUENCIES[i], gains[i], sampleRate, channels);}
    }



    private static class Biquad {

        private double b0;
        private double b1;
        private double b2;
        private double a1;
        private double a2;

        private final double frequency;
        private final double[] x1;
        private final double[] x2;
        private final double[] y1;
        private final double[] y2;

        Biquad(double frequency, double gainDb, double sampleRate, int channels) {
            this.frequency = frequency;
            this.x1 = new double[channels];
            this.x2 = new double[channels];
            this.y1 = new double[channels];
            this.y2 = new double[channels];
            updateGain(gainDb, sampleRate);
        }
        void setGain(double gainDb, double sampleRate) {
            updateGain(gainDb, sampleRate);
        }
        private void updateGain(double gainDb, double sampleRate) {
            double A = Math.pow(10.0, gainDb / 40.0);
            double omega = 2.0 * Math.PI * frequency / sampleRate;
            double cos = Math.cos(omega);
            double sin = Math.sin(omega);
            double Q = 1.0;
            double alpha = sin / (2.0 * Q);
            double rawB0 = 1.0 + alpha * A;
            double rawB1 = -2.0 * cos;
            double rawB2 = 1.0 - alpha * A;
            double rawA0 = 1.0 + alpha / A;
            double rawA1 = -2.0 * cos;
            double rawA2 = 1.0 - alpha / A;
            b0 = rawB0 / rawA0;
            b1 = rawB1 / rawA0;
            b2 = rawB2 / rawA0;
            a1 = rawA1 / rawA0;
            a2 = rawA2 / rawA0;
        }        
        double process(double input, int channel) {
            double output = b0 * input + b1 * x1[channel] + b2 * x2[channel] - a1 * y1[channel] - a2 * y2[channel];
            x2[channel] = x1[channel];
            x1[channel] = input;
            y2[channel] = y1[channel];
            y1[channel] = output;
            return output;
        }
    }


}
