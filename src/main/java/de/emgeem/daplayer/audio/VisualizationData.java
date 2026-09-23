package de.emgeem.daplayer.audio;

public class VisualizationData {

    private final float[] samples;
    private final float sampleRate;
    private final double leftPeak;
    private final double rightPeak;
    private final double leftRms;
    private final double rightRms;    

    public VisualizationData(float[] samples, float sampleRate, double leftPeak, double rightPeak, double leftRms, double rightRms) {
        this.samples = samples;
        this.sampleRate = sampleRate;
        this.leftPeak = leftPeak;
        this.rightPeak = rightPeak;
        this.leftRms = leftRms;
        this.rightRms = rightRms;
    }
    public float[] getSamples() {return samples;}
    public float getSampleRate() {return sampleRate;}
    public double getLeftPeak() {return leftPeak;}
    public double getRightPeak() {return rightPeak;}
    public double getLeftRms() {return leftRms;}
    public double getRightRms() {return rightRms;}
}
