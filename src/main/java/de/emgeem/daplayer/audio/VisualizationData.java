package de.emgeem.daplayer.audio;

public class VisualizationData {

    private final float[] samples;
    private final float sampleRate;

    public VisualizationData(float[] samples, float sampleRate) {
        this.samples = samples;
        this.sampleRate = sampleRate;
    }
    public float[] getSamples() {return samples;}
    public float getSampleRate() {return sampleRate;}
}
