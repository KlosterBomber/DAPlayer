package de.emgeem.daplayer.audio;

@FunctionalInterface 
public interface VisualizationListener {
    void onAudioData(VisualizationData data);
}
