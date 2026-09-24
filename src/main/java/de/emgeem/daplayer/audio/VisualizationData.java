package de.emgeem.daplayer.audio;

public class VisualizationData {

    private final float[] samples;
    private final float sampleRate;
    private final double leftPeak;
    private final double rightPeak;
    private final double leftRms;
    private final double rightRms;
    private final float[] leftSamples;
    private final float[] rightSamples;    

    public VisualizationData(float[] samples, float sampleRate, double leftPeak, double rightPeak, double leftRms, double rightRms, float[] leftSamples, float[] rightSamples) {
        this.samples = samples;
        this.sampleRate = sampleRate;
        this.leftPeak = leftPeak;
        this.rightPeak = rightPeak;
        this.leftRms = leftRms;
        this.rightRms = rightRms;
        this.leftSamples = leftSamples;
        this.rightSamples = rightSamples;
    }
    public float[] getSamples() {return samples;}
    public float getSampleRate() {return sampleRate;}
    public double getLeftPeak() {return leftPeak;}
    public double getRightPeak() {return rightPeak;}
    public double getLeftRms() {return leftRms;}
    public double getRightRms() {return rightRms;}
    public float[] getLeftSamples() {return leftSamples;}
    public float[] getRightSamples() {return rightSamples;}
}
