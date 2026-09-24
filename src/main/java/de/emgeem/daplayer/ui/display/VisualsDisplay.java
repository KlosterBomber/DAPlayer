package de.emgeem.daplayer.ui.display;

import de.emgeem.daplayer.audio.VisualizationData;
import de.emgeem.daplayer.audio.VisualizationListener;
import de.emgeem.daplayer.ui.display.visual.SpectrumVisualization;
import de.emgeem.daplayer.ui.display.visual.VectorScope;
import de.emgeem.daplayer.ui.display.visual.VuVisualization;
import de.emgeem.daplayer.ui.display.visual.WaveformVisualization;
import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class VisualsDisplay extends DisplayMode implements VisualizationListener {

    private final Font displayFont;
    private final Canvas canvas;

    private SpectrumVisualization spectrumVisualization;
    private WaveformVisualization waveformVisualization;
    private VuVisualization vuVisualization;
    private VectorScope vectorScope;
    private VisualizationData latestData;
    private VisualizationMode currentMode = VisualizationMode.SPECTRUM;

    public VisualsDisplay(Font displayFont) {
        this.displayFont = displayFont;
        canvas = new Canvas();
        getChildren().add(canvas);
        spectrumVisualization = new SpectrumVisualization(canvas.getGraphicsContext2D());
        waveformVisualization = new WaveformVisualization(canvas.getGraphicsContext2D());
        vuVisualization = new VuVisualization(canvas.getGraphicsContext2D());
        vectorScope = new VectorScope(canvas.getGraphicsContext2D());
        AnimationTimer animationTimer = new AnimationTimer() {
            @Override 
            public void handle(long now) {
                if (latestData == null) {return;}
                double visualWidth = canvas.getWidth() - 60;
                double visualHeight = 300;
                GraphicsContext gc = canvas.getGraphicsContext2D();
                gc.clearRect(30, 30, visualWidth, visualHeight);
                gc.save();
                gc.translate(30, 30);
                if (currentMode == VisualizationMode.SPECTRUM) {
                    spectrumVisualization.setSize(visualWidth, visualHeight);
                    spectrumVisualization.update(latestData);
                    spectrumVisualization.draw();
                } else if (currentMode == VisualizationMode.WAVEFORM) {
                    waveformVisualization.setSize(visualWidth, visualHeight);
                    waveformVisualization.update(latestData);
                    waveformVisualization.draw();
                } else if (currentMode == VisualizationMode.VU_METER) {
                    vuVisualization.setSize(visualWidth, visualHeight);
                    vuVisualization.update(latestData);
                    vuVisualization.draw();
                } else if (currentMode == VisualizationMode.VECTSCOP) {
                    vectorScope.setSize(visualWidth, visualHeight);
                    vectorScope.update(latestData);
                    vectorScope.draw();
                }
                gc.restore();
            }
        };
        animationTimer.start();
        widthProperty().addListener((obs, oldValue, newValue) -> draw());
        heightProperty().addListener((obs, oldValue, newValue) -> draw());

        canvas.setOnMouseClicked(event -> {
            double x = event.getX();
            double y = event.getY();
            if (y >= 350 && y <= 380) {
                if (x >= 30 && x <= 180) {currentMode = VisualizationMode.SPECTRUM;}
                else if (x >= 190 && x <= 340) {currentMode = VisualizationMode.WAVEFORM;}
                else if (x >= 350 && x <= 500) {currentMode = VisualizationMode.VU_METER;}
                else if (x >= 510 && x <= 660) {currentMode = VisualizationMode.VECTSCOP;}
                draw();
            }            
        });
    }

    @Override
    public void draw() {
        double w = getWidth();
        double h = getHeight();
        canvas.setWidth(w);
        canvas.setHeight(h);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        // Visualisierungsbereich
        gc.setFill(Color.TRANSPARENT);
        gc.fillRect(30, 30, w - 60, 300);
        // Trennlinie
        gc.setStroke(UiColors.accentLight());
        gc.setEffect(UiColors.displayGlow());
        gc.setLineWidth(1);
        gc.strokeLine(20, 345, w - 20, 345);
        // Untermenübuttons
        drawButton(gc, 30, 350, 150, 30, "SPECTRUM");
        drawButton(gc, 190, 350, 150, 30, "WAVEFORM");
        drawButton(gc, 350, 350, 150, 30, "VU-METER");
        drawButton(gc, 510, 350, 150, 30, "VECTOR-SCOPE");
        gc.setEffect(null);
    }

    private void drawButton(GraphicsContext gc, double x, double y, double width, double height, String text) {
        gc.setFill(UiColors.accentLight().deriveColor(0, 1, 1, 0.08));
        gc.fillRoundRect(x, y, width, height, 6, 6);
        gc.setStroke(UiColors.accentLight());
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, width, height, 6, 6);
        gc.setFill(UiColors.accentLight());
        gc.setFont(displayFont);
        double textWidth = gc.getFont().getSize() * text.length() * 0.6;
        gc.fillText(text, x + (width - textWidth) / 2, y + 21);
    }

    @Override
    public void onAudioData(VisualizationData data) {latestData = data;}

    @Override
    public void setPlaylist(
            de.emgeem.daplayer.model.Playlist playlist) {
        // Visualisierung benötigt keine Playlist.
    }
}