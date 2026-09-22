package de.emgeem.daplayer.ui.display.visual;

import de.emgeem.daplayer.audio.VisualizationData;
import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.GraphicsContext;

public class SpectrumVisualization {

    private final GraphicsContext gc;

    private double width;
    private double height;

    private static final int BAR_COUNT = 80;

    private float[] samples;
    private float sampleRate;

    public SpectrumVisualization(GraphicsContext gc) {
        this.gc = gc;
    }

    public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public void update(VisualizationData data) {
        this.samples = data.getSamples();
        this.sampleRate = data.getSampleRate();
    }

    public void draw() {
        if (samples == null || samples.length == 0) {return;}
        int fftSize = Integer.highestOneBit(samples.length);
        if (fftSize < 2) {return;}
        double[] real = new double[fftSize];
        double[] imag = new double[fftSize];
        for (int i = 0; i < fftSize; i++) {real[i] = samples[i];}
        fft(real, imag);
        int binCount = fftSize / 2;
        double barWidth = width / binCount;
        gc.setFill(UiColors.getAccent());
        for (int i = 1; i < binCount; i++) {
            double magnitude = Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
            double level = magnitude / fftSize;
            // logarithmische Pegeldarstellung
            double db = 20.0 * Math.log10(level + 1e-9);
            double normalized = (db + 80.0) / 80.0;
            normalized = Math.max(0.0, Math.min(1.0, normalized));
            double barHeight = normalized * height;
            double x = i * barWidth;
            double y = height - barHeight;
            gc.fillRect(x, y, Math.max(1, barWidth - 1), barHeight);
        }
    }

    private void fft(double[] real, double[] imag) {
        int n = real.length;
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            while ((j & bit) != 0) {
                j ^= bit;
                bit >>= 1;
            }
            j ^= bit;
            if (i < j) {
                double temp = real[i];
                real[i] = real[j];
                real[j] = temp;
                temp = imag[i];
                imag[i] = imag[j];
                imag[j] = temp;
            }
        }
        for (int length = 2; length <= n; length <<= 1) {
            double angle = -2.0 * Math.PI / length;
            double wReal = Math.cos(angle);
            double wImag = Math.sin(angle);
            for (int i = 0; i < n; i += length) {
                double currentReal = 1.0;
                double currentImag = 0.0;
                int half = length / 2;
                for (int j = 0; j < half; j++) {
                    int even = i + j;
                    int odd = even + half;
                    double oddReal = real[odd] * currentReal - imag[odd] * currentImag;
                    double oddImag = real[odd] * currentImag + imag[odd] * currentReal;
                    real[odd] = real[even] - oddReal;
                    imag[odd] = imag[even] - oddImag;
                    real[even] += oddReal;
                    imag[even] += oddImag;
                    double nextReal = currentReal * wReal - currentImag * wImag;
                    double nextImag = currentReal * wImag + currentImag * wReal;
                    currentReal = nextReal;
                    currentImag = nextImag;
                }
            }
        }
    }
}