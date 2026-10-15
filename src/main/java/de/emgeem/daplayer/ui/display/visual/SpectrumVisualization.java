package de.emgeem.daplayer.ui.display.visual;

import de.emgeem.daplayer.audio.VisualizationData;
import de.emgeem.daplayer.ui.painter.UiColors;
import javafx.scene.canvas.GraphicsContext;

/**
 * Spektrum mit {@value #BAR_COUNT} logarithmisch verteilten Bändern (30 Hz – 18 kHz),
 * Hann-Fenster und sanftem Abfall der Balken.
 */
public class SpectrumVisualization {

    private static final int BAR_COUNT = 80;
    private static final double MIN_FREQUENCY = 30.0;
    private static final double MAX_FREQUENCY = 18000.0;
    private static final double DB_RANGE = 70.0;
    private static final double FALL_PER_FRAME = 0.025;

    private final GraphicsContext gc;
    private final double[] levels = new double[BAR_COUNT];

    private double width;
    private double height;

    private float[] samples;
    private float sampleRate;
    private float[] lastAnalyzed;

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
        if (samples == null || samples.length < 2 || sampleRate <= 0) {return;}
        if (samples != lastAnalyzed) {
            analyze();
            lastAnalyzed = samples;
        } else {
            // keine neuen Daten (z. B. Pause): langsam abfallen lassen
            for (int i = 0; i < BAR_COUNT; i++) {levels[i] = Math.max(0, levels[i] - FALL_PER_FRAME * 0.2);}
        }
        if (UiColors.isRgbVisualization()) {gc.setFill(UiColors.accentRGB());} else {gc.setFill(UiColors.getAccent());}
        double slot = width / BAR_COUNT;
        double barWidth = Math.max(1, slot - 3);
        for (int i = 0; i < BAR_COUNT; i++) {
            double barHeight = levels[i] * height;
            if (barHeight < 1) {continue;}
            gc.fillRect(i * slot + 1.5, height - barHeight, barWidth, barHeight);
        }
    }

    private void analyze() {
        int fftSize = Integer.highestOneBit(samples.length);
        if (fftSize < 2) {return;}
        double[] real = new double[fftSize];
        double[] imag = new double[fftSize];
        int offset = samples.length - fftSize; // jüngste Samples verwenden
        for (int i = 0; i < fftSize; i++) {
            double window = 0.5 - 0.5 * Math.cos(2.0 * Math.PI * i / (fftSize - 1)); // Hann
            real[i] = samples[offset + i] * window;
        }
        fft(real, imag);
        int binCount = fftSize / 2;
        double binWidth = sampleRate / (double) fftSize;
        double maxFrequency = Math.min(MAX_FREQUENCY, sampleRate / 2.0);
        double ratio = Math.log(maxFrequency / MIN_FREQUENCY);
        for (int bar = 0; bar < BAR_COUNT; bar++) {
            double fLow = MIN_FREQUENCY * Math.exp(ratio * bar / BAR_COUNT);
            double fHigh = MIN_FREQUENCY * Math.exp(ratio * (bar + 1) / BAR_COUNT);
            int lowBin = Math.max(1, (int) Math.floor(fLow / binWidth));
            int highBin = Math.min(binCount - 1, Math.max(lowBin, (int) Math.ceil(fHigh / binWidth)));
            double peak = 0;
            for (int b = lowBin; b <= highBin; b++) {
                peak = Math.max(peak, Math.hypot(real[b], imag[b]));
            }
            // Normierung: Hann-Fenster halbiert die Amplitude -> * 4 / N
            double level = peak * 4.0 / fftSize;
            double db = 20.0 * Math.log10(level + 1e-9);
            double normalized = Math.max(0.0, Math.min(1.0, (db + DB_RANGE) / DB_RANGE));
            levels[bar] = normalized > levels[bar] ? normalized : Math.max(normalized, levels[bar] - FALL_PER_FRAME);
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
