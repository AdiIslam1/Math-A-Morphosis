package com.mathamorphosis.ui.visualizations;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class VectorView extends VBox {

    private Canvas canvas;
    private GraphicsContext gc;
    
    private final double WIDTH = 800;
    private final double HEIGHT = 600;
    private final double ORIGIN_X = WIDTH / 2;
    private final double ORIGIN_Y = HEIGHT / 2;
    
    // Vectors
    private double bx = 200, by = 0; // Vector B (Anchor, initially drawn on X axis for simplicity)
    private double ax = 100, ay = -150; // Vector A (Draggable)

    private boolean draggingA = false;
    private boolean draggingB = false;

    private AnimationTimer timer;
    private boolean isRunning = false;

    public VectorView() {
        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(20));

        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();

        canvas.setOnMousePressed(e -> {
            double mouseX = e.getX();
            double mouseY = e.getY();
            
            double distA = Math.hypot(mouseX - (ORIGIN_X + ax), mouseY - (ORIGIN_Y + ay));
            double distB = Math.hypot(mouseX - (ORIGIN_X + bx), mouseY - (ORIGIN_Y + by));
            
            if (distB < distA) {
                draggingB = true;
                draggingA = false;
            } else {
                draggingA = true;
                draggingB = false;
            }
        });

        canvas.setOnMouseReleased(e -> {
            draggingA = false;
            draggingB = false;
        });

        canvas.setOnMouseDragged(e -> {
            if (draggingB) {
                bx = e.getX() - ORIGIN_X;
                by = e.getY() - ORIGIN_Y;
            } else {
                ax = e.getX() - ORIGIN_X;
                ay = e.getY() - ORIGIN_Y;
            }
            // Stop animation if user manually drags
            if (isRunning && timer != null) {
                timer.stop();
                isRunning = false;
            }
            draw();
        });

        // Playback Controls
        HBox controlBox = new HBox(15);
        controlBox.setAlignment(Pos.CENTER);
        
        Button startBtn = new Button("Start");
        startBtn.getStyleClass().add("back-button");
        Button pauseBtn = new Button("Pause");
        pauseBtn.getStyleClass().add("back-button");
        Button restartBtn = new Button("Restart");
        restartBtn.getStyleClass().add("back-button");

        startBtn.setOnAction(e -> {
            if (timer != null && !isRunning) {
                timer.start();
                isRunning = true;
            }
        });
        
        pauseBtn.setOnAction(e -> {
            if (timer != null && isRunning) {
                timer.stop();
                isRunning = false;
            }
        });

        restartBtn.setOnAction(e -> {
            ax = 100;
            ay = -150;
            draw();
            if (timer != null && !isRunning) {
                timer.start();
                isRunning = true;
            }
        });

        controlBox.getChildren().addAll(startBtn, pauseBtn, restartBtn);

        this.getChildren().addAll(canvas, controlBox);
        draw();
        initTimer();
    }

    private void initTimer() {
        timer = new AnimationTimer() {
            private long lastUpdate = 0;
            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }
                double dt = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;
                if (dt > 0.1) dt = 0.016;

                // Rotate Vector A continuously
                double currentAngle = Math.atan2(ay, ax);
                double currentMag = Math.sqrt(ax * ax + ay * ay);
                
                // Add 1 radian per second
                double nextAngle = currentAngle + dt * 1.5;
                
                ax = currentMag * Math.cos(nextAngle);
                ay = currentMag * Math.sin(nextAngle);
                draw();
            }
            
            @Override
            public void stop() {
                super.stop();
                lastUpdate = 0;
            }
        };
    }

    private void draw() {
        gc.setFill(Color.web("#14142a"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw Grid
        gc.setStroke(Color.web("#1c1c38"));
        gc.setLineWidth(1);
        for (int i = 0; i < WIDTH; i += 50) gc.strokeLine(i, 0, i, HEIGHT);
        for (int i = 0; i < HEIGHT; i += 50) gc.strokeLine(0, i, WIDTH, i);

        // Draw Axes
        gc.setStroke(Color.web("#32325a"));
        gc.setLineWidth(2);
        gc.strokeLine(0, ORIGIN_Y, WIDTH, ORIGIN_Y);
        gc.strokeLine(ORIGIN_X, 0, ORIGIN_X, HEIGHT);

        // Math Calculations
        // Projection of A onto B: proj_B(A) = (A dot B / |B|^2) * B
        double dotProduct = (ax * bx) + (ay * by);
        double bMagSq = (bx * bx) + (by * by);
        if (bMagSq < 1e-6) bMagSq = 1e-6; // Prevent division by zero
        double scalar = dotProduct / bMagSq;
        
        double projX = scalar * bx;
        double projY = scalar * by;

        boolean blueDominant = Math.abs(scalar) >= 0.999;

        if (blueDominant) {
            // Background: Projection (thicker)
            gc.setLineWidth(8);
            if (dotProduct < 0) {
                gc.setStroke(Color.web("#ef4444")); // Red for negative dot product
            } else {
                gc.setStroke(Color.web("#eab308")); // Yellow for positive projection
            }
            gc.strokeLine(ORIGIN_X, ORIGIN_Y, ORIGIN_X + projX, ORIGIN_Y + projY);

            // Foreground: Anchor Vector B (Blue, thinner)
            gc.setLineWidth(4);
            gc.setStroke(Color.web("#5ba8e0")); // Blue
            drawArrow(ORIGIN_X, ORIGIN_Y, ORIGIN_X + bx, ORIGIN_Y + by);
        } else {
            // Background: Anchor Vector B (Blue, thicker)
            gc.setLineWidth(8);
            gc.setStroke(Color.web("#5ba8e0")); // Blue
            drawArrow(ORIGIN_X, ORIGIN_Y, ORIGIN_X + bx, ORIGIN_Y + by);

            // Foreground: Projection (thinner)
            gc.setLineWidth(4);
            if (dotProduct < 0) {
                gc.setStroke(Color.web("#ef4444")); // Red for negative dot product
            } else {
                gc.setStroke(Color.web("#eab308")); // Yellow for positive projection
            }
            gc.strokeLine(ORIGIN_X, ORIGIN_Y, ORIGIN_X + projX, ORIGIN_Y + projY);
        }

        // Draw Draggable Handle on tip of B
        gc.setFill(Color.WHITE);
        gc.fillOval(ORIGIN_X + bx - 8, ORIGIN_Y + by - 8, 16, 16);

        // 3. Draw Dashed Drop Line (Shadow)
        gc.setLineWidth(2);
        gc.setStroke(Color.web("#6868a0")); // Light Grey
        gc.setLineDashes(10);
        gc.strokeLine(ORIGIN_X + ax, ORIGIN_Y + ay, ORIGIN_X + projX, ORIGIN_Y + projY);
        gc.setLineDashes(0); // Reset dashes

        // 4. Draw Draggable Vector A (Orange)
        gc.setLineWidth(4);
        gc.setStroke(Color.web("#f97316")); // Orange
        drawArrow(ORIGIN_X, ORIGIN_Y, ORIGIN_X + ax, ORIGIN_Y + ay);

        // Draw Draggable Handle on tip of A
        gc.setFill(Color.WHITE);
        gc.fillOval(ORIGIN_X + ax - 8, ORIGIN_Y + ay - 8, 16, 16);
    }

    private void drawArrow(double x1, double y1, double x2, double y2) {
        gc.strokeLine(x1, y1, x2, y2);
        // Simple arrowhead
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double len = 15;
        gc.strokeLine(x2, y2, x2 - len * Math.cos(angle - Math.PI/6), y2 - len * Math.sin(angle - Math.PI/6));
        gc.strokeLine(x2, y2, x2 - len * Math.cos(angle + Math.PI/6), y2 - len * Math.sin(angle + Math.PI/6));
    }
}
