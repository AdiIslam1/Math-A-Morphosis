package com.mathamorphosis.ui.visualizations;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.geometry.Point2D;
import javafx.util.Duration;

import java.util.Random;

public class ChaosGameView extends BorderPane {

    // ── Canvas ──────────────────────────────────────────────────────────────
    private Canvas canvas;
    private GraphicsContext gc;
    private Pane sandbox;

    // ── Game state ───────────────────────────────────────────────────────────
    private Circle[]  vertices;
    private Label[]   vertexLabels;   // A, B, C labels on sandbox
    private Color[]   colors;
    private Point2D   currentPoint;
    private Random    random = new Random();

    private AnimationTimer timer;
    private boolean isPlaying        = false;
    private boolean isManualMode     = false;
    private boolean isAnimatingLaser = false;
    private Timeline laserTimeline;
    private Timeline diceTimeline;

    private enum SetupState {
        PLACE_VERTEX_1, PLACE_VERTEX_2, PLACE_VERTEX_3, PLACE_INITIAL_POINT, READY
    }
    private SetupState currentState = SetupState.PLACE_VERTEX_1;

    // ── UI refs ──────────────────────────────────────────────────────────────
    private Label  instructionLabel;
    private Button modeBtn;
    private Button diceBtn;
    private Button simulateRestBtn;
    private Button playPauseBtn;
    private Slider speedSlider;
    private Slider distSlider;

    // Dice display area
    private Pane   dicePane;          // fixed 140×140 area
    private Canvas diceCanvas;        // drawn face
    private Label  diceResultLabel;   // "Rolled 4 → Vertex B"

    // ────────────────────────────────────────────────────────────────────────
    public ChaosGameView() {
        this.getStyleClass().add("root");

        colors       = new Color[]{Color.CYAN, Color.MAGENTA, Color.YELLOW};
        vertices     = new Circle[3];
        vertexLabels = new Label[3];

        buildTopBar();
        buildCenter();
        buildRightPanel();

        initEngine();
        setupCanvasClicks();
        fullReset();
    }

    // ── Layout builders ──────────────────────────────────────────────────────

    private void buildTopBar() {
        instructionLabel = new Label();
        instructionLabel.setStyle(
            "-fx-font-size:18px; -fx-text-fill:#eab308; -fx-font-weight:bold;" +
            " -fx-padding:12px; -fx-background-color:#1c1c38;");
        instructionLabel.setAlignment(Pos.CENTER);
        instructionLabel.setMaxWidth(Double.MAX_VALUE);
        this.setTop(instructionLabel);
    }

    private void buildCenter() {
        sandbox = new Pane();
        sandbox.setStyle("-fx-background-color:#14142a;");

        canvas = new Canvas();
        canvas.widthProperty().bind(sandbox.widthProperty());
        canvas.heightProperty().bind(sandbox.heightProperty());

        // Redraw/clear whenever the canvas resizes
        canvas.widthProperty().addListener(o -> {
            if (currentState == SetupState.READY) softReset();
        });
        canvas.heightProperty().addListener(o -> {
            if (currentState == SetupState.READY) softReset();
        });

        gc = canvas.getGraphicsContext2D();
        sandbox.getChildren().add(canvas);

        // Thin result strip below canvas
        diceResultLabel = new Label("");
        diceResultLabel.setStyle(
            "-fx-font-size:15px; -fx-font-weight:bold; -fx-padding:6 14;" +
            " -fx-background-color:#1a1a36; -fx-background-radius:0;");
        diceResultLabel.setAlignment(Pos.CENTER);
        diceResultLabel.setMaxWidth(Double.MAX_VALUE);
        diceResultLabel.setPrefHeight(36);
        diceResultLabel.setMinHeight(36);

        // sandbox goes directly in center so it fills all available space;
        // diceResultLabel goes at the bottom of the BorderPane
        this.setCenter(sandbox);
        this.setBottom(diceResultLabel);
    }

    private void buildRightPanel() {
        VBox panel = new VBox(18);
        panel.setPadding(new Insets(20, 20, 20, 20));
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPrefWidth(320);
        panel.setStyle("-fx-background-color:#1c1c38;");

        // ── Header
        Label titleLabel = new Label("CONTROLS");
        titleLabel.setStyle("-fx-font-size:18px; -fx-text-fill:white; -fx-font-weight:bold;");

        // ── Mode toggle (always enabled)
        modeBtn = new Button("Switch to Manual Mode");
        modeBtn.getStyleClass().add("back-button");
        modeBtn.setMaxWidth(Double.MAX_VALUE);
        modeBtn.setOnAction(e -> toggleMode());

        // ── Dice display area ───────────────────────────────────────────────
        dicePane   = new Pane();
        diceCanvas = new Canvas(120, 120);
        dicePane.setMinSize(120, 120);
        dicePane.setPrefSize(120, 120);
        dicePane.setMaxSize(120, 120);
        dicePane.setStyle("-fx-background-color:transparent;");
        dicePane.getChildren().add(diceCanvas);
        drawDiceFace(6); // default

        StackPane diceCenterer = new StackPane(dicePane);
        diceCenterer.setMinSize(120, 120);
        diceCenterer.setAlignment(Pos.CENTER);
        VBox.setMargin(diceCenterer, new Insets(10, 0, 10, 0));

        // diceResultLabel is built in buildCenter() as a canvas bottom strip

        // ── Dice roll button
        diceBtn = new Button("🎲  Roll Dice");
        diceBtn.getStyleClass().add("back-button");
        diceBtn.setMaxWidth(Double.MAX_VALUE);
        diceBtn.setStyle("-fx-background-color:#7f1d1d; -fx-text-fill:white;" +
                         " -fx-font-size:15px; -fx-font-weight:bold;");
        diceBtn.setOnAction(e -> rollDiceAndAnimate());

        // ── Simulate rest
        simulateRestBtn = new Button("▶  Simulate the Rest");
        simulateRestBtn.getStyleClass().add("back-button");
        simulateRestBtn.setMaxWidth(Double.MAX_VALUE);
        simulateRestBtn.setOnAction(e -> {
            if (!isManualMode) return;
            if (currentState != SetupState.READY) return;
            isManualMode = false;
            modeBtn.setText("Switch to Manual Mode");
            diceBtn.setDisable(true);
            simulateRestBtn.setDisable(true);
            playPauseBtn.setDisable(false);
            speedSlider.setDisable(false);
            instructionLabel.setText("Auto Mode — The fractal is forming. Drag vertices to morph it live.");
            if (!isPlaying) { timer.start(); isPlaying = true; }
        });

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#333;");

        // ── Auto-mode controls
        Label speedLabel = new Label("Auto Speed (dots / frame)");
        speedLabel.setStyle("-fx-text-fill:#aaa; -fx-font-size:12px;");
        speedSlider = new Slider(1, 5000, 500);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);

        Label distLabel = new Label("Jump Distance");
        distLabel.setStyle("-fx-text-fill:#aaa; -fx-font-size:12px;");
        distSlider = new Slider(0.1, 0.9, 0.5);
        distSlider.setShowTickLabels(true);
        distSlider.setShowTickMarks(true);
        distSlider.setMajorTickUnit(0.1);
        distSlider.setBlockIncrement(0.01);
        distSlider.valueProperty().addListener((obs, o, n) -> {
            if (currentState == SetupState.READY) softReset();
        });

        playPauseBtn = new Button("⏯  Play / Pause");
        playPauseBtn.getStyleClass().add("back-button");
        playPauseBtn.setMaxWidth(Double.MAX_VALUE);
        playPauseBtn.setOnAction(e -> {
            if (currentState != SetupState.READY || isManualMode) return;
            if (isPlaying) { timer.stop(); } else { timer.start(); }
            isPlaying = !isPlaying;
        });

        Button resetBtn = new Button("⟳  Reset Setup");
        resetBtn.getStyleClass().add("back-button");
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.setStyle("-fx-background-color:#ef4444; -fx-text-fill:white; -fx-font-weight:bold;");
        resetBtn.setOnAction(e -> fullReset());

        panel.getChildren().addAll(
            titleLabel, modeBtn,
            new Separator(),
            diceCenterer, diceBtn, simulateRestBtn,
            sep,
            speedLabel, speedSlider, distLabel, distSlider,
            playPauseBtn, resetBtn
        );
        this.setRight(panel);
    }

    // ── Mode toggle ──────────────────────────────────────────────────────────

    private void toggleMode() {
        isManualMode = !isManualMode;

        if (isManualMode) {
            // Stop auto engine
            if (timer != null) timer.stop();
            isPlaying = false;
            modeBtn.setText("Switch to Auto Mode");
            diceBtn.setDisable(currentState != SetupState.READY);
            simulateRestBtn.setDisable(currentState != SetupState.READY);
            playPauseBtn.setDisable(true);
            speedSlider.setDisable(true);
            if (currentState == SetupState.READY)
                instructionLabel.setText("Manual Mode — Roll the dice to plot the next point!");
            else
                instructionLabel.setText("Manual Mode — Finish placing your points on the canvas first.");
        } else {
            modeBtn.setText("Switch to Manual Mode");
            diceBtn.setDisable(true);
            simulateRestBtn.setDisable(true);
            diceResultLabel.setText("");
            playPauseBtn.setDisable(currentState != SetupState.READY);
            speedSlider.setDisable(false);
            if (currentState == SetupState.READY)
                instructionLabel.setText("Auto Mode — The fractal is forming. Drag vertices to morph it live.");
        }
    }

    // ── Dice roll + laser ────────────────────────────────────────────────────

    private void rollDiceAndAnimate() {
        if (!isManualMode || isAnimatingLaser || currentState != SetupState.READY) return;

        isAnimatingLaser = true;
        diceBtn.setDisable(true);
        diceResultLabel.setText("");

        int finalRoll       = random.nextInt(6) + 1;          // 1-6
        int targetIndex     = (finalRoll - 1) / 2;            // 0,1,2
        Color targetColor   = colors[targetIndex];
        String vertexName   = new String[]{"A","B","C"}[targetIndex];
        String colorName    = new String[]{"Cyan","Magenta","Yellow"}[targetIndex];

        // ── Spinning dice animation: cycle through faces quickly then settle
        int[] spinFrames = new int[16];
        for (int i = 0; i < 15; i++) spinFrames[i] = (i % 6) + 1;
        spinFrames[15] = finalRoll;

        diceTimeline = new Timeline();
        for (int i = 0; i < spinFrames.length; i++) {
            final int face    = spinFrames[i];
            final boolean last = (i == spinFrames.length - 1);
            double delayMs = 40 + i * 18.0; // gets slower toward the end
            diceTimeline.getKeyFrames().add(new KeyFrame(
                Duration.millis(delayMs),
                ev -> {
                    drawDiceFace(face);
                    if (last) {
                        // Show colored result text
                        String txt = "Rolled  " + finalRoll + "  →  Vertex " + vertexName;
                        diceResultLabel.setText(txt);
                        diceResultLabel.setTextFill(targetColor);

                        // Now fire laser
                        fireLaser(targetIndex);
                    }
                }
            ));
        }
        diceTimeline.play();
    }

    /** Draws a clean dice face (1-6) onto diceCanvas. */
    private void drawDiceFace(int value) {
        javafx.scene.canvas.GraphicsContext dg = diceCanvas.getGraphicsContext2D();
        double W = diceCanvas.getWidth();
        double H = diceCanvas.getHeight();
        double r = 14;

        // Body – red rounded rect
        dg.clearRect(0, 0, W, H);
        dg.setFill(Color.web("#b91c1c"));
        dg.fillRoundRect(4, 4, W - 8, H - 8, r, r);
        dg.setStroke(Color.web("#fca5a5"));
        dg.setLineWidth(2);
        dg.strokeRoundRect(4, 4, W - 8, H - 8, r, r);

        // Pips
        dg.setFill(Color.WHITE);
        double[][] pips = pipPositions(value, W, H);
        for (double[] p : pips) {
            dg.fillOval(p[0] - 7, p[1] - 7, 14, 14);
        }
    }

    /** Returns (cx, cy) for every pip of a face value inside W×H. */
    private double[][] pipPositions(int value, double W, double H) {
        double l = W * 0.27, c = W * 0.50, r2 = W * 0.73;
        double t = H * 0.27, m = H * 0.50, b2 = H * 0.73;
        switch (value) {
            case 1: return new double[][]{{c, m}};
            case 2: return new double[][]{{l, t}, {r2, b2}};
            case 3: return new double[][]{{l, t}, {c, m}, {r2, b2}};
            case 4: return new double[][]{{l, t}, {r2, t}, {l, b2}, {r2, b2}};
            case 5: return new double[][]{{l, t}, {r2, t}, {c, m}, {l, b2}, {r2, b2}};
            case 6: return new double[][]{{l, t}, {r2, t}, {l, m}, {r2, m}, {l, b2}, {r2, b2}};
            default: return new double[][]{};
        }
    }

    private void fireLaser(int targetIndex) {
        Circle target = vertices[targetIndex];
        Color  col    = colors[targetIndex];
        double fraction = distSlider.getValue();

        double newX = currentPoint.getX() + (target.getCenterX() - currentPoint.getX()) * fraction;
        double newY = currentPoint.getY() + (target.getCenterY() - currentPoint.getY()) * fraction;

        Line laser = new Line(currentPoint.getX(), currentPoint.getY(),
                              currentPoint.getX(), currentPoint.getY());
        laser.setStroke(col);
        laser.setStrokeWidth(3);
        DropShadow glow = new DropShadow(14, col);
        glow.setInput(new Glow(0.8));
        laser.setEffect(glow);
        sandbox.getChildren().add(laser);

        laserTimeline = new Timeline(
            new KeyFrame(Duration.millis(380),
                new KeyValue(laser.endXProperty(), newX, javafx.animation.Interpolator.EASE_OUT),
                new KeyValue(laser.endYProperty(), newY, javafx.animation.Interpolator.EASE_OUT)
            )
        );
        laserTimeline.setOnFinished(ev -> {
            sandbox.getChildren().remove(laser);
            currentPoint = new Point2D(newX, newY);
            gc.setFill(col);
            gc.fillRect(newX, newY, 2, 2);
            isAnimatingLaser = false;
            if (isManualMode && currentState == SetupState.READY) diceBtn.setDisable(false);
        });
        laserTimeline.play();
    }

    // ── Canvas click setup ──────────────────────────────────────────────────

    private void setupCanvasClicks() {
        canvas.setOnMouseClicked(e -> {
            switch (currentState) {
                case PLACE_VERTEX_1:
                    vertices[0] = createVertex(e.getX(), e.getY(), colors[0], "A");
                    sandbox.getChildren().addAll(vertices[0], vertexLabels[0]);
                    currentState = SetupState.PLACE_VERTEX_2;
                    instructionLabel.setText("Step 2: Click to place Vertex B (Magenta)");
                    break;

                case PLACE_VERTEX_2:
                    vertices[1] = createVertex(e.getX(), e.getY(), colors[1], "B");
                    sandbox.getChildren().addAll(vertices[1], vertexLabels[1]);
                    currentState = SetupState.PLACE_VERTEX_3;
                    instructionLabel.setText("Step 3: Click to place Vertex C (Yellow)");
                    break;

                case PLACE_VERTEX_3:
                    vertices[2] = createVertex(e.getX(), e.getY(), colors[2], "C");
                    sandbox.getChildren().addAll(vertices[2], vertexLabels[2]);
                    currentState = SetupState.PLACE_INITIAL_POINT;
                    instructionLabel.setText("Step 4: Click anywhere to drop the starting point");
                    break;

                case PLACE_INITIAL_POINT:
                    currentPoint = new Point2D(e.getX(), e.getY());
                    gc.setFill(Color.WHITE);
                    gc.fillOval(e.getX() - 4, e.getY() - 4, 8, 8);
                    currentState = SetupState.READY;

                    // If already in manual mode just unlock the dice
                    if (isManualMode) {
                        instructionLabel.setText("Manual Mode — Roll the dice to plot the next point!");
                        diceBtn.setDisable(false);
                        simulateRestBtn.setDisable(false);
                    } else {
                        instructionLabel.setText("Ready! The fractal is forming. Drag vertices to morph it live.");
                        playPauseBtn.setDisable(false);
                        timer.start();
                        isPlaying = true;
                    }
                    break;

                default:
                    break;
            }
        });
    }

    // ── Vertex circle creation ───────────────────────────────────────────────

    private Circle createVertex(double x, double y, Color color, String name) {
        Circle c = new Circle(x, y, 10, color);
        c.setStroke(Color.WHITE);
        c.setStrokeWidth(2);
        DropShadow glow = new DropShadow(16, color);
        c.setEffect(glow);

        // Floating label
        int idx = "A".equals(name) ? 0 : "B".equals(name) ? 1 : 2;
        Label lbl = new Label(name);
        lbl.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:" + toHex(color) + ";");
        lbl.setLayoutX(x + 13);
        lbl.setLayoutY(y - 10);
        vertexLabels[idx] = lbl;

        c.setOnMouseDragged(e -> {
            if (currentState != SetupState.READY) return;
            double nx = Math.max(0, Math.min(canvas.getWidth(),  e.getX()));
            double ny = Math.max(0, Math.min(canvas.getHeight(), e.getY()));
            c.setCenterX(nx);
            c.setCenterY(ny);
            lbl.setLayoutX(nx + 13);
            lbl.setLayoutY(ny - 10);
            softReset();
        });
        return c;
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x",
            (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }

    // ── Reset helpers ────────────────────────────────────────────────────────

    private void fullReset() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (timer        != null) timer.stop();
        if (laserTimeline != null) laserTimeline.stop();
        if (diceTimeline  != null) diceTimeline.stop();
        isPlaying        = false;
        isAnimatingLaser = false;

        sandbox.getChildren().removeIf(n -> n instanceof Circle || n instanceof Line || n instanceof Label);

        vertices     = new Circle[3];
        vertexLabels = new Label[3];
        currentState = SetupState.PLACE_VERTEX_1;

        isManualMode = false;
        modeBtn.setText("Switch to Manual Mode");

        diceBtn.setDisable(true);
        simulateRestBtn.setDisable(true);
        playPauseBtn.setDisable(true);
        speedSlider.setDisable(false);
        diceResultLabel.setText("");
        drawDiceFace(6);

        instructionLabel.setText("Step 1: Click the dark canvas to place Vertex A (Cyan)");
    }

    private void softReset() {
        if (laserTimeline != null &&
            laserTimeline.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            laserTimeline.stop();
            isAnimatingLaser = false;
            if (isManualMode) diceBtn.setDisable(false);
        }
        sandbox.getChildren().removeIf(n -> n instanceof Line);
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        double w = canvas.getWidth() == 0 ? 800 : canvas.getWidth();
        double h = canvas.getHeight() == 0 ? 800 : canvas.getHeight();
        currentPoint = new Point2D(w / 2, h / 2);
    }

    // ── Auto engine ──────────────────────────────────────────────────────────

    private void initEngine() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (currentState != SetupState.READY || isManualMode) return;
                int    iters    = (int) speedSlider.getValue();
                double fraction = distSlider.getValue();
                for (int i = 0; i < iters; i++) {
                    int    ti  = random.nextInt(3);
                    Circle tgt = vertices[ti];
                    double nx  = currentPoint.getX() + (tgt.getCenterX() - currentPoint.getX()) * fraction;
                    double ny  = currentPoint.getY() + (tgt.getCenterY() - currentPoint.getY()) * fraction;
                    currentPoint = new Point2D(nx, ny);
                    gc.setFill(colors[ti]);
                    gc.fillRect(nx, ny, 1, 1);
                }
            }
        };
    }
}
