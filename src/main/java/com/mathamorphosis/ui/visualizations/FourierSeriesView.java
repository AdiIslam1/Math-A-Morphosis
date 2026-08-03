package com.mathamorphosis.ui.visualizations;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

public class FourierSeriesView extends StackPane {

    private static final double TWO_PI = Math.PI * 2;
    private static final int MAX_DRAWING_SAMPLES = 300;
    private static final double TARGET_FRAME_NANOS = 1_000_000_000.0 / 60.0;

    private final ResizableCanvas canvas;
    private final GraphicsContext gc;

    private final Slider termsSlider;
    private final Slider speedSlider;
    private final ComboBox<String> modeCombo;

    private List<Epicycle> epicycles = new ArrayList<>();
    private final List<Double> waveHistory = new ArrayList<>();
    private final List<Complex> pathHistory2D = new ArrayList<>();

    private final List<Complex> userDrawing = new ArrayList<>();
    private boolean isDrawing = false;

    private double time = 0;
    private double speed = 0.02;
    private double scale = 1.0;
    private double offsetX = 500;
    private double offsetY = 300;
    private double lastMouseX;
    private double lastMouseY;
    private long lastFrameNanos;

    private AnimationTimer timer;

    private double WIDTH = 1000;
    private double HEIGHT = 600;

    private final Text infoText = new Text();

    static class Complex {
        double re, im;
        Complex(double re, double im) { this.re = re; this.im = im; }
    }

    static class Epicycle {
        double freq;
        double radius;
        double phase;
    }

    // A fully responsive Canvas that integrates seamlessly into JavaFX layout engine
    class ResizableCanvas extends Canvas {
        @Override
        public boolean isResizable() {
            return true;
        }
        @Override
        public double prefWidth(double height) {
            return WIDTH;
        }
        @Override
        public double prefHeight(double width) {
            return HEIGHT;
        }
        @Override
        public double maxWidth(double height) {
            return Double.MAX_VALUE;
        }
        @Override
        public double maxHeight(double width) {
            return Double.MAX_VALUE;
        }
        @Override
        public void resize(double width, double height) {
            super.setWidth(width);
            super.setHeight(height);
        }
    }

    public FourierSeriesView() {
        this.getStyleClass().add("root");
        this.setStyle("-fx-background-color: #14142a;");
        this.setMinSize(0, 0);
        this.setPrefSize(WIDTH, HEIGHT);
        this.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        canvas = new ResizableCanvas();
        gc = canvas.getGraphicsContext2D();

        BorderPane uiLayer = new BorderPane();
        uiLayer.setPickOnBounds(false);

        VBox topBox = new VBox(5);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(10, 20, 10, 20));
        topBox.setStyle("-fx-background-color: rgba(15, 23, 42, 0.85); -fx-background-radius: 0 0 20 20; -fx-border-color: #32325a; -fx-border-width: 0 1px 1px 1px; -fx-border-radius: 0 0 20 20;");
        topBox.setMaxWidth(600);

        Label title = new Label("Fourier Series Epicycles");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        Label subtitle = new Label("Pan with mouse drag, Zoom with scroll. Draw in 2D mode!");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #6868a0;");
        topBox.getChildren().addAll(title, subtitle);
        BorderPane.setAlignment(topBox, Pos.TOP_CENTER);

        HBox controls = new HBox(20);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(15, 30, 15, 30));
        controls.setStyle("-fx-background-color: rgba(15, 23, 42, 0.85); -fx-background-radius: 25; -fx-border-color: #32325a; -fx-border-width: 1px; -fx-border-radius: 25;");
        controls.setMaxWidth(800);

        Label modeLabel = new Label("Mode:");
        modeLabel.setStyle("-fx-text-fill: #5ba8e0; -fx-font-size: 14px; -fx-font-weight: bold;");

        modeCombo = new ComboBox<>();
        modeCombo.getItems().addAll("Square Wave (1D)", "Sawtooth Wave (1D)", "Custom Drawing (2D)");
        modeCombo.setValue("Square Wave (1D)");
        modeCombo.setStyle("-fx-font-size: 12px;");

        Label termsLabel = new Label("Circles:");
        termsLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 14px; -fx-font-weight: bold;");

        termsSlider = new Slider(1, 150, 5);
        termsSlider.setShowTickMarks(false);
        termsSlider.setPrefWidth(120);

        Label speedLabel = new Label("Speed:");
        speedLabel.setStyle("-fx-text-fill: #c084fc; -fx-font-size: 14px; -fx-font-weight: bold;");

        speedSlider = new Slider(0.005, 0.1, 0.02);
        speedSlider.setShowTickMarks(false);
        speedSlider.setPrefWidth(120);

        controls.getChildren().addAll(modeLabel, modeCombo, termsLabel, termsSlider, speedLabel, speedSlider);
        BorderPane.setAlignment(controls, Pos.BOTTOM_CENTER);
        BorderPane.setMargin(controls, new Insets(0, 0, 20, 0));

        uiLayer.setTop(topBox);
        uiLayer.setBottom(controls);

        infoText.setFill(Color.web("#facc15"));
        infoText.setFont(Font.font("Segoe UI", 16));
        infoText.setStyle("-fx-font-weight: bold;");

        BorderPane infoOverlay = new BorderPane();
        infoOverlay.setPickOnBounds(false);
        infoOverlay.setPadding(new Insets(20));
        infoOverlay.setTop(infoText);
        BorderPane.setAlignment(infoText, Pos.TOP_LEFT);

        this.getChildren().addAll(canvas, uiLayer, infoOverlay);

        canvas.widthProperty().addListener((obs, oldW, newW) -> {
            WIDTH = newW.doubleValue();
            recenterAnimation();
        });

        canvas.heightProperty().addListener((obs, oldH, newH) -> {
            HEIGHT = newH.doubleValue();
            recenterAnimation();
        });

        setupInteractions();

        modeCombo.setOnAction(e -> applySettings());

        termsSlider.valueProperty().addListener((obs, o, n) -> {
            if (!modeCombo.getValue().equals("Custom Drawing (2D)")) {
                updateEpicyclesOnly();
                waveHistory.clear();
            } else {
                pathHistory2D.clear();
                time = 0;
            }
        });

        applySettings();

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double frameScale = lastFrameNanos == 0
                        ? 1.0
                        : Math.min(3.0, (now - lastFrameNanos) / TARGET_FRAME_NANOS);
                lastFrameNanos = now;
                render(frameScale);
            }
        };
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                timer.stop();
                lastFrameNanos = 0;
            } else {
                timer.start();
            }
        });
        timer.start();
    }

    private void recenterAnimation() {
        if (modeCombo.getValue() == null) return;
        if (!modeCombo.getValue().equals("Custom Drawing (2D)")) {
            offsetX = Math.max(WIDTH / 4, 300);
        } else {
            offsetX = Math.max(WIDTH / 2, 500);
        }
        offsetY = Math.max(HEIGHT / 2, 300);
    }

    private void setupInteractions() {
        canvas.setOnMousePressed(e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();

            if (e.isSecondaryButtonDown() || !modeCombo.getValue().equals("Custom Drawing (2D)")) {
                return;
            }

            isDrawing = true;
            userDrawing.clear();
            waveHistory.clear();
            pathHistory2D.clear();
            epicycles.clear();
            time = 0;

            userDrawing.add(new Complex((e.getX() - offsetX) / scale, (e.getY() - offsetY) / scale));
        });

        canvas.setOnMouseDragged(e -> {
            if (e.isSecondaryButtonDown() || !modeCombo.getValue().equals("Custom Drawing (2D)") || !isDrawing) {
                double dx = e.getX() - lastMouseX;
                double dy = e.getY() - lastMouseY;
                offsetX += dx;
                offsetY += dy;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                return;
            }

            userDrawing.add(new Complex((e.getX() - offsetX) / scale, (e.getY() - offsetY) / scale));
            lastMouseX = e.getX();
            lastMouseY = e.getY();
        });

        canvas.setOnMouseReleased(e -> {
            if (!isDrawing) return;
            isDrawing = false;

            List<Complex> sampled = resampleClosedPath(userDrawing, MAX_DRAWING_SAMPLES);

            if (sampled.size() > 2) {
                epicycles = dft(sampled);
                waveHistory.clear();
                pathHistory2D.clear();
            }
        });

        canvas.setOnScroll(e -> {
            double zoomFactor = 1.1;
            double oldScale = scale;
            if (e.getDeltaY() > 0) {
                scale *= zoomFactor;
            } else if (e.getDeltaY() < 0) {
                scale /= zoomFactor;
            }

            if (scale < 0.1) scale = 0.1;
            if (scale > 10) scale = 10;

            double mouseX = e.getX();
            double mouseY = e.getY();

            offsetX = mouseX - (mouseX - offsetX) * (scale / oldScale);
            offsetY = mouseY - (mouseY - offsetY) * (scale / oldScale);
        });
    }

    private void applySettings() {
        if (modeCombo.getValue() == null) return;
        String mode = modeCombo.getValue();
        waveHistory.clear();
        pathHistory2D.clear();
        time = 0;
        userDrawing.clear();

        scale = 1.0;
        recenterAnimation();

        updateEpicyclesOnly();
    }

    private void updateEpicyclesOnly() {
        if (modeCombo.getValue() == null) return;
        String mode = modeCombo.getValue();
        int nTerms = (int) termsSlider.getValue();
        epicycles.clear();

        if (mode.equals("Square Wave (1D)")) {
            for (int i = 0; i < nTerms; i++) {
                int n = i * 2 + 1;
                double radius = (150.0 * 4) / (n * Math.PI);
                Epicycle e = new Epicycle();
                e.freq = n;
                e.radius = radius;
                e.phase = 0;
                epicycles.add(e);
            }
        } else if (mode.equals("Sawtooth Wave (1D)")) {
            for (int i = 0; i < nTerms; i++) {
                int n = i + 1;
                double radius = (150.0 * 2) / (n * Math.PI);
                Epicycle e = new Epicycle();
                e.freq = n;
                e.radius = radius;
                e.phase = (n % 2 == 0) ? Math.PI : 0;
                epicycles.add(e);
            }
        }
    }

    static List<Complex> resampleClosedPath(List<Complex> points, int maxSamples) {
        List<Complex> clean = new ArrayList<>();
        for (Complex point : points) {
            if (clean.isEmpty() || distance(clean.get(clean.size() - 1), point) > 1e-6) {
                clean.add(point);
            }
        }

        if (clean.size() < 2 || maxSamples < 3) {
            return clean;
        }

        int segmentCount = clean.size();
        double[] cumulativeLengths = new double[segmentCount + 1];
        for (int i = 0; i < segmentCount; i++) {
            Complex start = clean.get(i);
            Complex end = clean.get((i + 1) % clean.size());
            cumulativeLengths[i + 1] = cumulativeLengths[i] + distance(start, end);
        }

        double totalLength = cumulativeLengths[segmentCount];
        if (totalLength <= 1e-6) {
            return new ArrayList<>();
        }

        int sampleCount = Math.min(maxSamples, Math.max(32, clean.size()));
        List<Complex> sampled = new ArrayList<>(sampleCount);
        int segment = 0;
        for (int i = 0; i < sampleCount; i++) {
            double targetLength = totalLength * i / sampleCount;
            while (segment + 1 < segmentCount
                    && cumulativeLengths[segment + 1] <= targetLength) {
                segment++;
            }

            Complex start = clean.get(segment);
            Complex end = clean.get((segment + 1) % clean.size());
            double segmentLength = cumulativeLengths[segment + 1] - cumulativeLengths[segment];
            double fraction = segmentLength <= 1e-12
                    ? 0
                    : (targetLength - cumulativeLengths[segment]) / segmentLength;
            sampled.add(new Complex(
                    start.re + (end.re - start.re) * fraction,
                    start.im + (end.im - start.im) * fraction));
        }
        return sampled;
    }

    private static double distance(Complex a, Complex b) {
        return Math.hypot(b.re - a.re, b.im - a.im);
    }

    static List<Epicycle> dft(List<Complex> x) {
        int N = x.size();
        List<Epicycle> result = new ArrayList<>();
        if (N == 0) return result;

        for (int k = 0; k < N; k++) {
            double re = 0;
            double im = 0;
            for (int n = 0; n < N; n++) {
                double angle = (TWO_PI * k * n) / N;
                re += x.get(n).re * Math.cos(angle) + x.get(n).im * Math.sin(angle);
                im += -x.get(n).re * Math.sin(angle) + x.get(n).im * Math.cos(angle);
            }
            re /= N;
            im /= N;

            Epicycle e = new Epicycle();
            e.freq = k;
            e.radius = Math.hypot(re, im);
            e.phase = Math.atan2(im, re);
            result.add(e);
        }

        for (Epicycle e : result) {
            if (e.freq > N / 2) {
                e.freq = e.freq - N;
            }
        }

        result.sort((a, b) -> Double.compare(b.radius, a.radius));
        return result;
    }

    private void render(double frameScale) {
        speed = speedSlider.getValue();

        // Clear canvas
        gc.clearRect(0, 0, WIDTH, HEIGHT);

        // Draw engineering grid to definitively show the massive canvas bounds
        gc.setStroke(Color.web("#32325a", 0.3));
        gc.setLineWidth(1);
        gc.beginPath();
        for (double x = offsetX % 50; x < WIDTH; x += 50) {
            gc.moveTo(x, 0);
            gc.lineTo(x, HEIGHT);
        }
        for (double y = offsetY % 50; y < HEIGHT; y += 50) {
            gc.moveTo(0, y);
            gc.lineTo(WIDTH, y);
        }
        gc.stroke();

        if (isDrawing) {
            infoText.setText("Status: Drawing...");
            if (userDrawing.size() > 1) {
                gc.setStroke(Color.web("#6868a0"));
                gc.setLineWidth(4);
                gc.setLineDashes(5, 5);
                gc.beginPath();
                for (int i = 0; i < userDrawing.size(); i++) {
                    double px = offsetX + userDrawing.get(i).re * scale;
                    double py = offsetY + userDrawing.get(i).im * scale;
                    if (i == 0) gc.moveTo(px, py);
                    else gc.lineTo(px, py);
                }
                gc.stroke();
                gc.setLineDashes();
            }
            return;
        }

        if (modeCombo.getValue() == null) return;
        boolean is1D = !modeCombo.getValue().equals("Custom Drawing (2D)");

        if (epicycles.isEmpty()) {
            if (!is1D) infoText.setText("Status: Left click and drag to draw a shape. Right click and drag to pan.");
            return;
        }

        int nTerms = is1D ? epicycles.size() : Math.min(epicycles.size(), (int) termsSlider.getValue());
        infoText.setText("Circles Active: " + nTerms);

        double mathX = 0;
        double mathY = 0;

        for (int i = 0; i < nTerms; i++) {
            Epicycle epi = epicycles.get(i);

            double prevX = offsetX + mathX * scale;
            double prevY = offsetY + mathY * scale;

            mathX += epi.radius * Math.cos(epi.freq * time + epi.phase);
            mathY += epi.radius * Math.sin(epi.freq * time + epi.phase);

            double sx = offsetX + mathX * scale;
            double sy = offsetY + mathY * scale;

            double r = epi.radius * scale;

            gc.setStroke(Color.web("#5ba8e0", 0.6));
            gc.setLineWidth(2);
            if (epi.freq != 0) {
                gc.strokeOval(prevX - r, prevY - r, r * 2, r * 2);
            }

            gc.setStroke(Color.web("#ffffff", 0.8));
            gc.setLineWidth(2);
            gc.strokeLine(prevX, prevY, sx, sy);
        }

        double finalSx = offsetX + mathX * scale;
        double finalSy = offsetY + mathY * scale;

        if (is1D) {
            waveHistory.add(0, mathY);

            double waveStartX = offsetX + 300 * scale;

            int maxHistorySize = (int) Math.max(100, (WIDTH - waveStartX) / scale) + 250;
            while (waveHistory.size() > maxHistorySize) {
                waveHistory.remove(waveHistory.size() - 1);
            }

            gc.setStroke(Color.web("#b0b0d0", 0.5));
            gc.setLineWidth(2);
            gc.setLineDashes(5, 5);
            gc.strokeLine(finalSx, finalSy, waveStartX, finalSy);
            gc.setLineDashes();

            if (waveHistory.size() > 1) {
                // Glow effect
                gc.setStroke(Color.web("#facc15", 0.3));
                gc.setLineWidth(10);
                gc.beginPath();
                for (int i = 0; i < waveHistory.size(); i++) {
                    double px = waveStartX + i * scale;
                    double py = offsetY + waveHistory.get(i) * scale;
                    if (i == 0) gc.moveTo(px, py);
                    else gc.lineTo(px, py);
                }
                gc.stroke();

                gc.setStroke(Color.web("#facc15"));
                gc.setLineWidth(3);
                gc.beginPath();
                for (int i = 0; i < waveHistory.size(); i++) {
                    double px = waveStartX + i * scale;
                    double py = offsetY + waveHistory.get(i) * scale;
                    if (i == 0) gc.moveTo(px, py);
                    else gc.lineTo(px, py);
                }
                gc.stroke();
            }
        } else {
            pathHistory2D.add(new Complex(mathX, mathY));

            int max2DFrames = (int) (Math.PI * 2 / speed) + 5;
            while (pathHistory2D.size() > max2DFrames) {
                pathHistory2D.remove(0);
            }

            if (userDrawing.size() > 1) {
                gc.setStroke(Color.web("#6868a0", 0.3));
                gc.setLineWidth(2);
                gc.beginPath();
                for (int i = 0; i < userDrawing.size(); i++) {
                    double px = offsetX + userDrawing.get(i).re * scale;
                    double py = offsetY + userDrawing.get(i).im * scale;
                    if (i == 0) gc.moveTo(px, py);
                    else gc.lineTo(px, py);
                }
                gc.closePath();
                gc.stroke();
            }

            if (pathHistory2D.size() > 1) {
                // Glow effect
                gc.setStroke(Color.web("#facc15", 0.3));
                gc.setLineWidth(10);
                gc.beginPath();
                for (int i = 0; i < pathHistory2D.size(); i++) {
                    double px = offsetX + pathHistory2D.get(i).re * scale;
                    double py = offsetY + pathHistory2D.get(i).im * scale;
                    if (i == 0) gc.moveTo(px, py);
                    else gc.lineTo(px, py);
                }
                gc.stroke();

                gc.setStroke(Color.web("#facc15"));
                gc.setLineWidth(3);
                gc.beginPath();
                for (int i = 0; i < pathHistory2D.size(); i++) {
                    double px = offsetX + pathHistory2D.get(i).re * scale;
                    double py = offsetY + pathHistory2D.get(i).im * scale;
                    if (i == 0) gc.moveTo(px, py);
                    else gc.lineTo(px, py);
                }
                gc.stroke();
            }
        }

        time = (time + speed * frameScale) % TWO_PI;
    }
}
