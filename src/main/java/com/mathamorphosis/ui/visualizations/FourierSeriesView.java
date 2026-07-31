package com.mathamorphosis.ui.visualizations;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;

public class FourierSeriesView extends BorderPane {

    private final Pane canvasPane;
    
    private final Slider termsSlider;
    private final ComboBox<String> modeCombo;
    
    private List<Epicycle> epicycles = new ArrayList<>();
    private final List<Double> waveHistory = new ArrayList<>();
    private final Polyline pathLine;
    
    // For 2D drawing
    private final List<Complex> userDrawing = new ArrayList<>();
    private final Polyline userDrawingLine = new Polyline();
    private boolean isDrawing = false;
    
    private double time = 0;
    private double speed = 0.02;
    private AnimationTimer timer;
    
    private double WIDTH = 1000;
    private double HEIGHT = 600;
    
    // Graphics elements to reuse to prevent excessive object creation
    private final List<Circle> circleShapes = new ArrayList<>();
    private final List<Line> lineShapes = new ArrayList<>();
    private final Line tracerLine = new Line();

    static class Complex {
        double re, im;
        Complex(double re, double im) { this.re = re; this.im = im; }
    }

    static class Epicycle {
        double freq;
        double radius;
        double phase;
    }

    public FourierSeriesView() {
        this.getStyleClass().add("root");

        VBox topBox = new VBox(10);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(10, 0, 10, 0));
        
        Label title = new Label("Fourier Series Epicycles");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        Label subtitle = new Label("Everything is made of circles. Select a wave or draw your own closed loop in 2D.");
        subtitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #94a3b8;");
        
        topBox.getChildren().addAll(title, subtitle);

        canvasPane = new Pane();
        canvasPane.setStyle("-fx-background-color: #050505;");
        
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(canvasPane.widthProperty());
        clip.heightProperty().bind(canvasPane.heightProperty());
        canvasPane.setClip(clip);

        canvasPane.widthProperty().addListener((obs, o, n) -> WIDTH = n.doubleValue());
        canvasPane.heightProperty().addListener((obs, o, n) -> HEIGHT = n.doubleValue());
        
        pathLine = new Polyline();
        pathLine.setStroke(Color.web("#facc15")); // yellow
        pathLine.setStrokeWidth(3);
        pathLine.setEffect(new javafx.scene.effect.DropShadow(10, Color.web("#facc15")));
        
        userDrawingLine.setStroke(Color.web("#94a3b8"));
        userDrawingLine.setStrokeWidth(2);
        userDrawingLine.getStrokeDashArray().addAll(5d, 5d);
        
        tracerLine.setStroke(Color.web("#cbd5e1", 0.5));
        tracerLine.getStrokeDashArray().addAll(5d, 5d);
        
        canvasPane.getChildren().addAll(userDrawingLine, tracerLine, pathLine);

        HBox controls = new HBox(30);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(20));

        Label modeLabel = new Label("Mode:");
        modeLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        modeCombo = new ComboBox<>();
        modeCombo.getItems().addAll("Square Wave (1D)", "Sawtooth Wave (1D)", "Custom Drawing (2D)");
        modeCombo.setValue("Square Wave (1D)");
        modeCombo.setStyle("-fx-font-size: 14px;");
        
        Label termsLabel = new Label("Number of Circles:");
        termsLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        termsSlider = new Slider(1, 100, 5);
        termsSlider.setShowTickMarks(true);
        termsSlider.setShowTickLabels(true);
        termsSlider.setMajorTickUnit(10);
        termsSlider.setBlockIncrement(1);
        termsSlider.setPrefWidth(200);
        
        controls.getChildren().addAll(modeLabel, modeCombo, termsLabel, termsSlider);

        this.setTop(topBox);
        this.setCenter(canvasPane);
        this.setBottom(controls);
        // BorderPane.setMargin(canvasPane, new Insets(0));

        setupInteractions();
        
        modeCombo.setOnAction(e -> applySettings());
        termsSlider.valueProperty().addListener((obs, o, n) -> {
            if (!modeCombo.getValue().equals("Custom Drawing (2D)")) {
                applySettings();
            }
            // For custom drawing, we don't recalculate DFT, just limit the number of drawn circles
            // It will be handled in the animation loop
        });

        applySettings();
        
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                render();
            }
        };
        timer.start();
    }

    private void setupInteractions() {
        canvasPane.setOnMousePressed(e -> {
            if (!modeCombo.getValue().equals("Custom Drawing (2D)")) return;
            isDrawing = true;
            userDrawing.clear();
            userDrawingLine.getPoints().clear();
            waveHistory.clear();
            pathLine.getPoints().clear();
            epicycles.clear();
            time = 0;
        });
        
        canvasPane.setOnMouseDragged(e -> {
            if (!isDrawing) return;
            userDrawingLine.getPoints().addAll(e.getX(), e.getY());
            userDrawing.add(new Complex(e.getX() - WIDTH / 2, e.getY() - HEIGHT / 2));
        });
        
        canvasPane.setOnMouseReleased(e -> {
            if (!isDrawing) return;
            isDrawing = false;
            
            // Subsample if too many points to avoid massive DFT overhead
            int MAX_POINTS = 200;
            List<Complex> sampled = new ArrayList<>();
            if (userDrawing.size() > MAX_POINTS) {
                double step = (double) userDrawing.size() / MAX_POINTS;
                for (double i = 0; i < userDrawing.size(); i += step) {
                    sampled.add(userDrawing.get((int) i));
                }
            } else {
                sampled.addAll(userDrawing);
            }
            
            if (sampled.size() > 2) {
                epicycles = dft(sampled);
                // Adjust speed based on number of points so it draws exactly one loop per cycle
                speed = (Math.PI * 2) / sampled.size();
                waveHistory.clear();
                pathLine.getPoints().clear();
            }
        });
    }

    private void applySettings() {
        String mode = modeCombo.getValue();
        int nTerms = (int) termsSlider.getValue();
        waveHistory.clear();
        pathLine.getPoints().clear();
        time = 0;
        userDrawingLine.getPoints().clear();
        userDrawing.clear();
        
        if (mode.equals("Square Wave (1D)")) {
            speed = 0.03;
            epicycles.clear();
            for (int i = 0; i < nTerms; i++) {
                int n = i * 2 + 1; 
                double radius = (100.0 * 4) / (n * Math.PI);
                Epicycle e = new Epicycle();
                e.freq = n;
                e.radius = radius;
                e.phase = 0; 
                epicycles.add(e);
            }
        } else if (mode.equals("Sawtooth Wave (1D)")) {
            speed = 0.03;
            epicycles.clear();
            for (int i = 0; i < nTerms; i++) {
                int n = i + 1; 
                double radius = (100.0 * 2) / (n * Math.PI);
                Epicycle e = new Epicycle();
                e.freq = n;
                e.radius = radius;
                e.phase = (n % 2 == 0) ? Math.PI : 0;
                epicycles.add(e);
            }
        } else {
            // Custom drawing mode, clear everything and wait for user
            epicycles.clear();
        }
    }
    
    private List<Epicycle> dft(List<Complex> x) {
        int N = x.size();
        List<Epicycle> result = new ArrayList<>();
        for (int k = 0; k < N; k++) {
            double re = 0;
            double im = 0;
            for (int n = 0; n < N; n++) {
                double angle = (Math.PI * 2 * k * n) / N;
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

    private void render() {
        if (isDrawing || epicycles.isEmpty()) return;
        
        boolean is1D = !modeCombo.getValue().equals("Custom Drawing (2D)");
        
        // Origin coordinates
        double cx = is1D ? 300 : WIDTH / 2;
        double cy = is1D ? HEIGHT / 2 : HEIGHT / 2;
        
        int nTerms = is1D ? epicycles.size() : Math.min(epicycles.size(), (int) termsSlider.getValue());
        
        // Match UI elements to nTerms
        while (circleShapes.size() < nTerms) {
            Circle c = new Circle();
            c.setFill(Color.TRANSPARENT);
            c.setStroke(Color.web("#38bdf8", 0.4));
            circleShapes.add(c);
            canvasPane.getChildren().add(c);
            c.toBack();
        }
        while (circleShapes.size() > nTerms) {
            Circle c = circleShapes.remove(circleShapes.size() - 1);
            canvasPane.getChildren().remove(c);
        }
        
        while (lineShapes.size() < nTerms) {
            Line l = new Line();
            l.setStroke(Color.web("#ffffff", 0.6));
            lineShapes.add(l);
            canvasPane.getChildren().add(l);
            l.toBack();
        }
        while (lineShapes.size() > nTerms) {
            Line l = lineShapes.remove(lineShapes.size() - 1);
            canvasPane.getChildren().remove(l);
        }
        
        // Calculate epicycles
        for (int i = 0; i < nTerms; i++) {
            Epicycle epi = epicycles.get(i);
            
            double prevX = cx;
            double prevY = cy;
            
            cx += epi.radius * Math.cos(epi.freq * time + epi.phase);
            cy += epi.radius * Math.sin(epi.freq * time + epi.phase);
            
            Circle circle = circleShapes.get(i);
            circle.setCenterX(prevX);
            circle.setCenterY(prevY);
            circle.setRadius(epi.radius);
            
            Line line = lineShapes.get(i);
            line.setStartX(prevX);
            line.setStartY(prevY);
            line.setEndX(cx);
            line.setEndY(cy);
        }
        
        if (is1D) {
            waveHistory.add(0, cy);
            if (waveHistory.size() > 500) waveHistory.remove(waveHistory.size() - 1);
            
            tracerLine.setVisible(true);
            tracerLine.setStartX(cx);
            tracerLine.setStartY(cy);
            tracerLine.setEndX(500);
            tracerLine.setEndY(waveHistory.get(0));
            
            pathLine.getPoints().clear();
            for (int i = 0; i < waveHistory.size(); i++) {
                pathLine.getPoints().addAll(500.0 + i, waveHistory.get(i));
            }
        } else {
            tracerLine.setVisible(false);
            
            pathLine.getPoints().addAll(cx, cy);
            
            // To prevent memory leak in 2D mode, remove oldest points if it exceeds 1 full loop roughly
            int max2DPoints = epicycles.size(); // the N sampled points
            if (pathLine.getPoints().size() / 2 > max2DPoints * 2) { 
                pathLine.getPoints().remove(0, 2); 
            }
        }
        
        time += speed;
    }
}
