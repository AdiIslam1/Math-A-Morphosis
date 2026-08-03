package com.mathamorphosis.ui.visualizations;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class VectorView extends BorderPane {

    private Canvas canvas;
    private GraphicsContext gc;

    private final double WIDTH = 800;
    private final double HEIGHT = 600;
    private final double ORIGIN_X = WIDTH / 2;
    private final double ORIGIN_Y = HEIGHT / 2;

    // Vectors
    private double bx = 200, by = 0;   // Vector B (Anchor)
    private double ax = 100, ay = -150; // Vector A (Draggable)

    private boolean draggingA = false;
    private boolean draggingB = false;

    private AnimationTimer timer;
    private boolean isRunning = false;

    // Live-updating labels
    private Label vectorALabel;
    private Label vectorBLabel;
    private Label dotProductLabel;
    private Label projLabel;

    public VectorView() {
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();

        // ── Mouse Interaction ──────────────────────────────────────────────
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

        // ── Playback Controls ──────────────────────────────────────────────
        HBox controlBox = new HBox(15);
        controlBox.setAlignment(Pos.CENTER);
        controlBox.setPadding(new Insets(10, 0, 10, 0));

        Button startBtn = new Button("▶ Start");
        startBtn.getStyleClass().add("back-button");
        Button pauseBtn = new Button("⏸ Pause");
        pauseBtn.getStyleClass().add("back-button");
        Button restartBtn = new Button("↺ Restart");
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

        // ── Centre: Canvas + Controls ──────────────────────────────────────
        VBox centreBox = new VBox(0, canvas, controlBox);
        centreBox.setAlignment(Pos.CENTER);
        centreBox.setStyle("-fx-background-color: #0c0c1e;");

        // ── Right: Info / Explanation Panel ────────────────────────────────
        VBox panel = buildInfoPanel();

        this.setCenter(centreBox);
        this.setRight(panel);

        draw();
        initTimer();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Info Panel Builder
    // ──────────────────────────────────────────────────────────────────────────

    private VBox buildInfoPanel() {
        VBox panel = new VBox(16);
        panel.setPrefWidth(400);
        panel.setMinWidth(400);
        panel.setMaxWidth(400);
        panel.setStyle(
            "-fx-background-color: #14142a;" +
            "-fx-border-color: #32325a;" +
            "-fx-border-width: 0 0 0 1;"
        );
        panel.setPadding(new Insets(20, 24, 20, 24));

        // ── Section label 'LINEAR ALGEBRA' ────────────────────────────────
        Label sectionLabel = new Label("LINEAR ALGEBRA");
        sectionLabel.setStyle(
            "-fx-text-fill: #6868a0;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: 'Inter', 'Segoe UI', sans-serif;" +
            "-fx-letter-spacing: 1.5;"
        );

        // ── CONCEPT card ──────────────────────────────────────────────────
        VBox conceptCard = new VBox(8);
        conceptCard.setStyle(
            "-fx-background-color: #1c1c38;" +
            "-fx-border-color: #5ba8e0;" +
            "-fx-border-width: 0 0 0 4;" +
            "-fx-background-radius: 8;" +
            "-fx-border-radius: 8;"
        );
        conceptCard.setPadding(new Insets(18));

        Label conceptTitle = new Label("Vector Projection");
        conceptTitle.setStyle(
            "-fx-text-fill: #5ba8e0;" +
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: 'Inter', 'Segoe UI', sans-serif;"
        );

        Label conceptDesc = new Label(
            "Projection shows how much of vector A lies in the direction of " +
            "vector B. Drag the arrow tips to explore."
        );
        conceptDesc.setWrapText(true);
        conceptDesc.setStyle(
            "-fx-text-fill: #d0d0e8;" +
            "-fx-font-size: 15px;" +
            "-fx-font-family: 'Inter', 'Segoe UI', sans-serif;"
        );

        conceptCard.getChildren().addAll(conceptTitle, conceptDesc);

        // ── FORMULAE card ─────────────────────────────────────────────────
        VBox formulaeCard = new VBox(8);
        formulaeCard.setStyle(
            "-fx-background-color: #1c1c38;" +
            "-fx-border-color: #32325a;" +
            "-fx-border-width: 1;" +
            "-fx-background-radius: 8;" +
            "-fx-border-radius: 8;"
        );
        formulaeCard.setPadding(new Insets(16));

        Label formulaeHeader = new Label("FORMULAE");
        formulaeHeader.setStyle(
            "-fx-text-fill: #6868a0;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: 'Inter', 'Segoe UI', sans-serif;"
        );

        Label formula1 = new Label("proj_B(A) = (A·B / |B|²) × B");
        formula1.setStyle(
            "-fx-text-fill: #d4a84b;" +
            "-fx-font-size: 17px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace;"
        );

        Label formula2 = new Label("A·B = Ax·Bx + Ay·By");
        formula2.setStyle(
            "-fx-text-fill: #b0b0d0;" +
            "-fx-font-size: 14px;" +
            "-fx-font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace;"
        );

        Label formula3 = new Label("|B|² = Bx² + By²");
        formula3.setStyle(
            "-fx-text-fill: #b0b0d0;" +
            "-fx-font-size: 14px;" +
            "-fx-font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace;"
        );

        formulaeCard.getChildren().addAll(formulaeHeader, formula1, formula2, formula3);

        // ── LIVE VECTORS card ─────────────────────────────────────────────
        VBox liveCard = new VBox(8);
        liveCard.setStyle(
            "-fx-background-color: #1c1c38;" +
            "-fx-border-color: #32325a;" +
            "-fx-border-width: 1;" +
            "-fx-background-radius: 8;" +
            "-fx-border-radius: 8;"
        );
        liveCard.setPadding(new Insets(16));

        Label liveHeader = new Label("LIVE READINGS");
        liveHeader.setStyle(
            "-fx-text-fill: #6868a0;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: 'Inter', 'Segoe UI', sans-serif;"
        );

        vectorALabel = new Label("A = (100.0, -150.0)");
        vectorALabel.setStyle(
            "-fx-text-fill: #f97316;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace;"
        );

        vectorBLabel = new Label("B = (200.0, 0.0)");
        vectorBLabel.setStyle(
            "-fx-text-fill: #5ba8e0;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace;"
        );

        dotProductLabel = new Label("A · B = ...");
        dotProductLabel.setStyle(
            "-fx-text-fill: #4cbf95;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace;"
        );

        projLabel = new Label("proj = (...)");
        projLabel.setStyle(
            "-fx-text-fill: #d4a84b;" +
            "-fx-font-size: 15px;" +
            "-fx-font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace;"
        );

        liveCard.getChildren().addAll(liveHeader, vectorALabel, vectorBLabel, dotProductLabel, projLabel);

        // ── COLOUR LEGEND card ────────────────────────────────────────────
        VBox legendCard = new VBox(8);
        legendCard.setStyle(
            "-fx-background-color: #1c1c38;" +
            "-fx-border-color: #32325a;" +
            "-fx-border-width: 1;" +
            "-fx-background-radius: 8;" +
            "-fx-border-radius: 8;"
        );
        legendCard.setPadding(new Insets(14));

        Label legendHeader = new Label("COLOUR GUIDE");
        legendHeader.setStyle(
            "-fx-text-fill: #6868a0;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: 'Inter', 'Segoe UI', sans-serif;"
        );

        legendCard.getChildren().addAll(
            legendHeader,
            legendRow("#f97316", "Vector A (draggable, rotates)"),
            legendRow("#5ba8e0", "Vector B (draggable anchor)"),
            legendRow("#d4a84b", "Projection of A onto B"),
            legendRow("#6868a0", "Perpendicular drop line")
        );

        // ── HOW TO INTERACT card ──────────────────────────────────────────
        VBox howtoCard = new VBox(8);
        howtoCard.setStyle(
            "-fx-background-color: #1c1c38;" +
            "-fx-border-color: #32325a;" +
            "-fx-border-width: 1;" +
            "-fx-background-radius: 8;" +
            "-fx-border-radius: 8;"
        );
        howtoCard.setPadding(new Insets(14));

        Label howtoHeader = new Label("INTERACTION");
        howtoHeader.setStyle(
            "-fx-text-fill: #6868a0;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: 'Inter', 'Segoe UI', sans-serif;"
        );

        String[] tips = {
            "• Drag the white tip of Vector A (orange) to rotate it",
            "• Drag the white tip of Vector B (blue) to reorient",
            "• Use ▶ Start to auto-rotate Vector A continuously",
            "• Watch the projection update in real time"
        };

        howtoCard.getChildren().add(howtoHeader);
        for (String tip : tips) {
            Label tipLabel = new Label(tip);
            tipLabel.setWrapText(true);
            tipLabel.setStyle(
                "-fx-text-fill: #b0b0d0;" +
                "-fx-font-size: 13px;" +
                "-fx-font-family: 'Inter', 'Segoe UI', sans-serif;"
            );
            howtoCard.getChildren().add(tipLabel);
        }

        // ── Assemble Panel ────────────────────────────────────────────────
        panel.getChildren().addAll(
            sectionLabel,
            conceptCard,
            formulaeCard,
            liveCard,
            legendCard,
            howtoCard
        );

        return panel;
    }

    /** Builds a colour-legend row: coloured dot + description text. */
    private HBox legendRow(String hexColour, String text) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label dot = new Label("●");
        dot.setStyle(
            "-fx-text-fill: " + hexColour + ";" +
            "-fx-font-size: 16px;"
        );
        dot.setMinWidth(18);

        Label desc = new Label(text);
        desc.setWrapText(true);
        desc.setStyle(
            "-fx-text-fill: #8080b0;" +
            "-fx-font-size: 13px;" +
            "-fx-font-family: 'Inter', 'Segoe UI', sans-serif;"
        );

        row.getChildren().addAll(dot, desc);
        return row;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Animation Timer
    // ──────────────────────────────────────────────────────────────────────────

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
                double currentMag   = Math.sqrt(ax * ax + ay * ay);

                // Add 1.5 radians per second
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

    // ──────────────────────────────────────────────────────────────────────────
    //  Draw
    // ──────────────────────────────────────────────────────────────────────────

    private void draw() {
        gc.setFill(Color.web("#14142a"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw Grid
        gc.setStroke(Color.web("#1c1c38"));
        gc.setLineWidth(1);
        for (int i = 0; i < WIDTH; i += 50)  gc.strokeLine(i, 0, i, HEIGHT);
        for (int i = 0; i < HEIGHT; i += 50) gc.strokeLine(0, i, WIDTH, i);

        // Draw Axes
        gc.setStroke(Color.web("#32325a"));
        gc.setLineWidth(2);
        gc.strokeLine(0, ORIGIN_Y, WIDTH, ORIGIN_Y);
        gc.strokeLine(ORIGIN_X, 0, ORIGIN_X, HEIGHT);

        // ── Math Calculations ──────────────────────────────────────────────
        double dotProduct = (ax * bx) + (ay * by);
        double bMagSq     = (bx * bx) + (by * by);
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

        // Draw Dashed Drop Line (Shadow)
        gc.setLineWidth(2);
        gc.setStroke(Color.web("#6868a0")); // Light Grey
        gc.setLineDashes(10);
        gc.strokeLine(ORIGIN_X + ax, ORIGIN_Y + ay, ORIGIN_X + projX, ORIGIN_Y + projY);
        gc.setLineDashes(0); // Reset dashes

        // Draw Draggable Vector A (Orange)
        gc.setLineWidth(4);
        gc.setStroke(Color.web("#f97316")); // Orange
        drawArrow(ORIGIN_X, ORIGIN_Y, ORIGIN_X + ax, ORIGIN_Y + ay);

        // Draw Draggable Handle on tip of A
        gc.setFill(Color.WHITE);
        gc.fillOval(ORIGIN_X + ax - 8, ORIGIN_Y + ay - 8, 16, 16);

        // ── Update Live Labels ─────────────────────────────────────────────
        if (vectorALabel != null) {
            vectorALabel.setText(String.format("A = (%.1f, %.1f)", ax, ay));
            vectorBLabel.setText(String.format("B = (%.1f, %.1f)", bx, by));
            dotProductLabel.setText(String.format("A · B = %.1f", dotProduct));
            projLabel.setText(String.format("proj = (%.1f, %.1f)", projX, projY));
        }
    }

    private void drawArrow(double x1, double y1, double x2, double y2) {
        gc.strokeLine(x1, y1, x2, y2);
        // Simple arrowhead
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double len   = 15;
        gc.strokeLine(x2, y2,
                      x2 - len * Math.cos(angle - Math.PI / 6),
                      y2 - len * Math.sin(angle - Math.PI / 6));
        gc.strokeLine(x2, y2,
                      x2 - len * Math.cos(angle + Math.PI / 6),
                      y2 - len * Math.sin(angle + Math.PI / 6));
    }
}
