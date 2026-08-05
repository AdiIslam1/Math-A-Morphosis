package com.mathamorphosis.ui.visualizations;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for fourier_series_view.fxml.
 * Owns all Fourier Series Epicycles logic, driven by FXML-injected nodes.
 * The ResizableCanvas is built in code and inserted as child[0] of rootPane.
 */
public class FourierSeriesViewController {

    // ── FXML-injected nodes ──────────────────────────────────────────────────
    @FXML private StackPane rootPane;
    @FXML private ComboBox<String> modeCombo;
    @FXML private Slider termsSlider;
    @FXML private Slider speedSlider;
    @FXML private Label  infoLabel;

    // ── Canvas (built in initialize) ─────────────────────────────────────────
    private ResizableCanvas canvas;
    private GraphicsContext gc;

    // ── Constants ────────────────────────────────────────────────────────────
    private static final double TWO_PI              = Math.PI * 2;
    private static final int    MAX_DRAWING_SAMPLES = 300;
    private static final double TARGET_FRAME_NANOS  = 1_000_000_000.0 / 60.0;

    // ── State ────────────────────────────────────────────────────────────────
    private List<Epicycle> epicycles    = new ArrayList<>();
    private final List<Double>  waveHistory   = new ArrayList<>();
    private final List<Complex> pathHistory2D = new ArrayList<>();
    private final List<Complex> userDrawing   = new ArrayList<>();

    private boolean isDrawing    = false;
    private double  time         = 0;
    private double  speed        = 0.02;
    private double  scale        = 1.0;
    private double  offsetX      = 500;
    private double  offsetY      = 300;
    private double  lastMouseX;
    private double  lastMouseY;
    private long    lastFrameNanos;

    private double WIDTH  = 1000;
    private double HEIGHT = 600;

    private AnimationTimer timer;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // Build and insert the resizable canvas before any UI overlay
        canvas = new ResizableCanvas();
        gc     = canvas.getGraphicsContext2D();
        rootPane.getChildren().add(0, canvas);   // behind the overlay BorderPanes

        // Clip ComboBox to stack context
        canvas.widthProperty().addListener((obs, o, n) -> { WIDTH  = n.doubleValue(); recenterAnimation(); });
        canvas.heightProperty().addListener((obs, o, n) -> { HEIGHT = n.doubleValue(); recenterAnimation(); });

        // Style the ComboBox dropdown
        styleComboBox();

        // Populate mode items
        modeCombo.getItems().addAll("Square Wave (1D)", "Sawtooth Wave (1D)", "Custom Drawing (2D)");
        modeCombo.setValue("Square Wave (1D)");
        modeCombo.setOnAction(e -> applySettings());

        termsSlider.valueProperty().addListener((obs, o, n) -> {
            if (!modeCombo.getValue().equals("Custom Drawing (2D)")) {
                updateEpicyclesOnly(); waveHistory.clear();
            } else {
                pathHistory2D.clear(); time = 0;
            }
        });

        setupInteractions();
        applySettings();

        // Animation loop
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                double frameScale = lastFrameNanos == 0
                    ? 1.0 : Math.min(3.0, (now - lastFrameNanos) / TARGET_FRAME_NANOS);
                lastFrameNanos = now;
                render(frameScale);
            }
        };
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) { timer.stop(); lastFrameNanos = 0; }
            else                 { timer.start(); }
        });
        timer.start();
    }

    // ── ComboBox styling ─────────────────────────────────────────────────────

    private void styleComboBox() {
        Callback<javafx.scene.control.ListView<String>, ListCell<String>> cf = lv ->
            new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("-fx-background-color: #0d1b2e;");
                    } else {
                        setText(item);
                        String base = "-fx-background-color: #0d1b2e; -fx-text-fill: #5ba8e0;" +
                                      "-fx-font-size: 13px; -fx-font-style: italic; -fx-font-weight: bold; -fx-padding: 6 12;";
                        String hover = "-fx-background-color: #1a3a5c; -fx-text-fill: #ffffff;" +
                                       "-fx-font-size: 13px; -fx-font-style: italic; -fx-font-weight: bold; -fx-padding: 6 12;";
                        setStyle(base);
                        setOnMouseEntered(e -> setStyle(hover));
                        setOnMouseExited(e  -> setStyle(base));
                    }
                }
            };
        modeCombo.setButtonCell(cf.call(null));
        modeCombo.setCellFactory(cf);
    }

    // ── Settings / Epicycle builders ─────────────────────────────────────────

    private void recenterAnimation() {
        if (modeCombo.getValue() == null) return;
        offsetX = !modeCombo.getValue().equals("Custom Drawing (2D)")
            ? Math.max(WIDTH / 4, 300) : Math.max(WIDTH / 2, 500);
        offsetY = Math.max(HEIGHT / 2, 300);
    }

    private void applySettings() {
        if (modeCombo.getValue() == null) return;
        waveHistory.clear(); pathHistory2D.clear();
        time = 0; scale = 1.0;
        userDrawing.clear();
        recenterAnimation();
        updateEpicyclesOnly();
    }

    private void updateEpicyclesOnly() {
        if (modeCombo.getValue() == null) return;
        int nTerms = (int) termsSlider.getValue();
        epicycles.clear();
        String mode = modeCombo.getValue();
        if (mode.equals("Square Wave (1D)")) {
            for (int i = 0; i < nTerms; i++) {
                int n = i * 2 + 1;
                Epicycle e = new Epicycle(); e.freq = n; e.radius = (150.0 * 4) / (n * Math.PI); e.phase = 0;
                epicycles.add(e);
            }
        } else if (mode.equals("Sawtooth Wave (1D)")) {
            for (int i = 0; i < nTerms; i++) {
                int n = i + 1;
                Epicycle e = new Epicycle(); e.freq = n; e.radius = (150.0 * 2) / (n * Math.PI);
                e.phase = (n % 2 == 0) ? Math.PI : 0;
                epicycles.add(e);
            }
        }
    }

    // ── Interactions ─────────────────────────────────────────────────────────

    private void setupInteractions() {
        canvas.setOnMousePressed(e -> {
            lastMouseX = e.getX(); lastMouseY = e.getY();
            if (e.isSecondaryButtonDown() || !modeCombo.getValue().equals("Custom Drawing (2D)")) return;
            isDrawing = true;
            userDrawing.clear(); waveHistory.clear(); pathHistory2D.clear(); epicycles.clear(); time = 0;
            userDrawing.add(new Complex((e.getX() - offsetX) / scale, (e.getY() - offsetY) / scale));
        });
        canvas.setOnMouseDragged(e -> {
            if (e.isSecondaryButtonDown() || !modeCombo.getValue().equals("Custom Drawing (2D)") || !isDrawing) {
                offsetX += e.getX() - lastMouseX; offsetY += e.getY() - lastMouseY;
                lastMouseX = e.getX(); lastMouseY = e.getY();
                return;
            }
            userDrawing.add(new Complex((e.getX() - offsetX) / scale, (e.getY() - offsetY) / scale));
            lastMouseX = e.getX(); lastMouseY = e.getY();
        });
        canvas.setOnMouseReleased(e -> {
            if (!isDrawing) return;
            isDrawing = false;
            List<Complex> sampled = resampleClosedPath(userDrawing, MAX_DRAWING_SAMPLES);
            if (sampled.size() > 2) { epicycles = dft(sampled); waveHistory.clear(); pathHistory2D.clear(); }
        });
        canvas.setOnScroll(e -> {
            double factor = e.getDeltaY() > 0 ? 1.1 : (1 / 1.1);
            double oldScale = scale;
            scale = Math.max(0.1, Math.min(10, scale * factor));
            offsetX = e.getX() - (e.getX() - offsetX) * (scale / oldScale);
            offsetY = e.getY() - (e.getY() - offsetY) * (scale / oldScale);
        });
    }

    // ── Render ───────────────────────────────────────────────────────────────

    private void render(double frameScale) {
        speed = speedSlider.getValue();
        gc.clearRect(0, 0, WIDTH, HEIGHT);

        // Grid
        gc.setStroke(Color.web("#32325a", 0.3)); gc.setLineWidth(1); gc.beginPath();
        for (double x = offsetX % 50; x < WIDTH;  x += 50) { gc.moveTo(x, 0); gc.lineTo(x, HEIGHT); }
        for (double y = offsetY % 50; y < HEIGHT; y += 50) { gc.moveTo(0, y); gc.lineTo(WIDTH, y); }
        gc.stroke();

        if (isDrawing) {
            infoLabel.setText("Status: Drawing...");
            if (userDrawing.size() > 1) {
                gc.setStroke(Color.web("#6868a0")); gc.setLineWidth(4); gc.setLineDashes(5, 5);
                gc.beginPath();
                for (int i = 0; i < userDrawing.size(); i++) {
                    double px = offsetX + userDrawing.get(i).re * scale;
                    double py = offsetY + userDrawing.get(i).im * scale;
                    if (i == 0) gc.moveTo(px, py); else gc.lineTo(px, py);
                }
                gc.stroke(); gc.setLineDashes();
            }
            return;
        }

        if (modeCombo.getValue() == null) return;
        boolean is1D = !modeCombo.getValue().equals("Custom Drawing (2D)");

        if (epicycles.isEmpty()) {
            if (!is1D) infoLabel.setText("Status: Left click and drag to draw a shape. Right click and drag to pan.");
            return;
        }

        int nTerms = is1D ? epicycles.size() : Math.min(epicycles.size(), (int) termsSlider.getValue());
        infoLabel.setText("Circles Active: " + nTerms);

        double mathX = 0, mathY = 0;
        for (int i = 0; i < nTerms; i++) {
            Epicycle epi  = epicycles.get(i);
            double prevX  = offsetX + mathX * scale;
            double prevY  = offsetY + mathY * scale;
            mathX += epi.radius * Math.cos(epi.freq * time + epi.phase);
            mathY += epi.radius * Math.sin(epi.freq * time + epi.phase);
            double sx = offsetX + mathX * scale, sy = offsetY + mathY * scale;
            double r  = epi.radius * scale;
            gc.setStroke(Color.web("#5ba8e0", 0.6)); gc.setLineWidth(2);
            if (epi.freq != 0) gc.strokeOval(prevX - r, prevY - r, r * 2, r * 2);
            gc.setStroke(Color.web("#ffffff", 0.8)); gc.setLineWidth(2);
            gc.strokeLine(prevX, prevY, sx, sy);
        }

        double finalSx = offsetX + mathX * scale, finalSy = offsetY + mathY * scale;

        if (is1D) {
            waveHistory.add(0, mathY);
            double waveStartX = offsetX + 300 * scale;
            int maxSize = (int) Math.max(100, (WIDTH - waveStartX) / scale) + 250;
            while (waveHistory.size() > maxSize) waveHistory.remove(waveHistory.size() - 1);

            gc.setStroke(Color.web("#b0b0d0", 0.5)); gc.setLineWidth(2); gc.setLineDashes(5, 5);
            gc.strokeLine(finalSx, finalSy, waveStartX, finalSy); gc.setLineDashes();

            if (waveHistory.size() > 1) {
                for (int pass = 0; pass < 2; pass++) {
                    gc.setStroke(pass == 0 ? Color.web("#facc15", 0.3) : Color.web("#facc15"));
                    gc.setLineWidth(pass == 0 ? 10 : 3); gc.beginPath();
                    for (int i = 0; i < waveHistory.size(); i++) {
                        double px = waveStartX + i * scale, py = offsetY + waveHistory.get(i) * scale;
                        if (i == 0) gc.moveTo(px, py); else gc.lineTo(px, py);
                    }
                    gc.stroke();
                }
            }
        } else {
            pathHistory2D.add(new Complex(mathX, mathY));
            int max2D = (int)(Math.PI * 2 / speed) + 5;
            while (pathHistory2D.size() > max2D) pathHistory2D.remove(0);

            if (userDrawing.size() > 1) {
                gc.setStroke(Color.web("#6868a0", 0.3)); gc.setLineWidth(2); gc.beginPath();
                for (int i = 0; i < userDrawing.size(); i++) {
                    double px = offsetX + userDrawing.get(i).re * scale;
                    double py = offsetY + userDrawing.get(i).im * scale;
                    if (i == 0) gc.moveTo(px, py); else gc.lineTo(px, py);
                }
                gc.closePath(); gc.stroke();
            }

            if (pathHistory2D.size() > 1) {
                for (int pass = 0; pass < 2; pass++) {
                    gc.setStroke(pass == 0 ? Color.web("#facc15", 0.3) : Color.web("#facc15"));
                    gc.setLineWidth(pass == 0 ? 10 : 3); gc.beginPath();
                    for (int i = 0; i < pathHistory2D.size(); i++) {
                        double px = offsetX + pathHistory2D.get(i).re * scale;
                        double py = offsetY + pathHistory2D.get(i).im * scale;
                        if (i == 0) gc.moveTo(px, py); else gc.lineTo(px, py);
                    }
                    gc.stroke();
                }
            }
        }
        time = (time + speed * frameScale) % TWO_PI;
    }

    // ── DFT + path helpers ───────────────────────────────────────────────────

    static List<Complex> resampleClosedPath(List<Complex> points, int maxSamples) {
        List<Complex> clean = new ArrayList<>();
        for (Complex p : points) {
            if (clean.isEmpty() || distance(clean.get(clean.size() - 1), p) > 1e-6) clean.add(p);
        }
        if (clean.size() < 2 || maxSamples < 3) return clean;

        int n = clean.size();
        double[] cumLen = new double[n + 1];
        for (int i = 0; i < n; i++)
            cumLen[i + 1] = cumLen[i] + distance(clean.get(i), clean.get((i + 1) % n));
        double total = cumLen[n];
        if (total <= 1e-6) return new ArrayList<>();

        int sampleCount = Math.min(maxSamples, Math.max(32, n));
        List<Complex> sampled = new ArrayList<>(sampleCount);
        int seg = 0;
        for (int i = 0; i < sampleCount; i++) {
            double target = total * i / sampleCount;
            while (seg + 1 < n && cumLen[seg + 1] <= target) seg++;
            Complex s = clean.get(seg), e = clean.get((seg + 1) % n);
            double segLen = cumLen[seg + 1] - cumLen[seg];
            double frac   = segLen <= 1e-12 ? 0 : (target - cumLen[seg]) / segLen;
            sampled.add(new Complex(s.re + (e.re - s.re) * frac, s.im + (e.im - s.im) * frac));
        }
        return sampled;
    }

    private static double distance(Complex a, Complex b) { return Math.hypot(b.re - a.re, b.im - a.im); }

    static List<Epicycle> dft(List<Complex> x) {
        int N = x.size();
        List<Epicycle> result = new ArrayList<>();
        if (N == 0) return result;
        for (int k = 0; k < N; k++) {
            double re = 0, im = 0;
            for (int n = 0; n < N; n++) {
                double angle = (TWO_PI * k * n) / N;
                re += x.get(n).re * Math.cos(angle) + x.get(n).im * Math.sin(angle);
                im += -x.get(n).re * Math.sin(angle) + x.get(n).im * Math.cos(angle);
            }
            Epicycle e = new Epicycle();
            e.freq = k; e.radius = Math.hypot(re / N, im / N); e.phase = Math.atan2(im / N, re / N);
            result.add(e);
        }
        for (Epicycle e : result) { if (e.freq > N / 2) e.freq = e.freq - N; }
        result.sort((a, b) -> Double.compare(b.radius, a.radius));
        return result;
    }

    // ── Inner types ──────────────────────────────────────────────────────────

    static class Complex {
        double re, im;
        Complex(double re, double im) { this.re = re; this.im = im; }
    }

    static class Epicycle {
        double freq, radius, phase;
    }

    /**
     * A fully responsive Canvas that integrates with JavaFX's layout engine
     * by overriding resize/prefWidth/prefHeight.
     */
    class ResizableCanvas extends Canvas {
        @Override public boolean isResizable()          { return true; }
        @Override public double prefWidth(double h)     { return WIDTH; }
        @Override public double prefHeight(double w)    { return HEIGHT; }
        @Override public double maxWidth(double h)      { return Double.MAX_VALUE; }
        @Override public double maxHeight(double w)     { return Double.MAX_VALUE; }
        @Override public void resize(double w, double h){ super.setWidth(w); super.setHeight(h); }
    }
}
