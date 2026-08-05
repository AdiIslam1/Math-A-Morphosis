package com.mathamorphosis.ui.visualizations;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.animation.AnimationTimer;
import javafx.util.Duration;

import java.util.Random;

/**
 * Controller for chaos_game_view.fxml.
 * Owns all Chaos Game (Sierpiński Triangle) logic, driven by FXML-injected nodes.
 */
public class ChaosGameViewController {

    // ── FXML-injected nodes ──────────────────────────────────────────────────
    @FXML private Label  instructionLabel;
    @FXML private Pane   sandbox;
    @FXML private Label  diceResultLabel;
    @FXML private Button modeBtn;
    @FXML private Canvas diceCanvas;
    @FXML private Button diceBtn;
    @FXML private Button simulateRestBtn;
    @FXML private Slider speedSlider;
    @FXML private Slider distSlider;
    @FXML private Button playPauseBtn;
    @FXML private Button resetBtn;

    // ── Canvas (bound to sandbox, built in initialize) ───────────────────────
    private Canvas          canvas;
    private GraphicsContext gc;

    // ── Game state ───────────────────────────────────────────────────────────
    private final Color[] colors = {Color.CYAN, Color.MAGENTA, Color.YELLOW};
    private Circle[]  vertices     = new Circle[3];
    private Label[]   vertexLabels = new Label[3];
    private Point2D   currentPoint;
    private final Random random    = new Random();

    private AnimationTimer timer;
    private boolean isPlaying        = false;
    private boolean isManualMode     = false;
    private boolean isAnimatingLaser = false;
    private Timeline laserTimeline;
    private Timeline diceTimeline;

    private enum SetupState { PLACE_VERTEX_1, PLACE_VERTEX_2, PLACE_VERTEX_3, PLACE_INITIAL_POINT, READY }
    private SetupState currentState = SetupState.PLACE_VERTEX_1;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // Build the main drawing canvas and bind it to the sandbox pane
        canvas = new Canvas();
        canvas.widthProperty().bind(sandbox.widthProperty());
        canvas.heightProperty().bind(sandbox.heightProperty());
        canvas.widthProperty().addListener(o  -> { if (currentState == SetupState.READY) softReset(); });
        canvas.heightProperty().addListener(o -> { if (currentState == SetupState.READY) softReset(); });
        gc = canvas.getGraphicsContext2D();
        sandbox.getChildren().add(0, canvas);

        // Wire up buttons
        modeBtn.setOnAction(e        -> toggleMode());
        diceBtn.setOnAction(e        -> rollDiceAndAnimate());
        simulateRestBtn.setOnAction(e -> simulateRest());
        playPauseBtn.setOnAction(e   -> togglePlayPause());
        resetBtn.setOnAction(e       -> fullReset());

        distSlider.valueProperty().addListener((obs, o, n) -> { if (currentState == SetupState.READY) softReset(); });

        setupCanvasClicks();
        initEngine();
        fullReset();
    }

    // ── Mode ─────────────────────────────────────────────────────────────────

    private void toggleMode() {
        isManualMode = !isManualMode;
        if (isManualMode) {
            if (timer != null) timer.stop();
            isPlaying = false;
            modeBtn.setText("Switch to Auto Mode");
            diceBtn.setDisable(currentState != SetupState.READY);
            simulateRestBtn.setDisable(currentState != SetupState.READY);
            playPauseBtn.setDisable(true);
            speedSlider.setDisable(true);
            instructionLabel.setText(currentState == SetupState.READY
                ? "Manual Mode — Roll the dice to plot the next point!"
                : "Manual Mode — Finish placing your points on the canvas first.");
        } else {
            modeBtn.setText("Switch to Manual Mode");
            diceBtn.setDisable(true);
            simulateRestBtn.setDisable(true);
            diceResultLabel.setText("");
            playPauseBtn.setDisable(currentState != SetupState.READY);
            speedSlider.setDisable(false);
            instructionLabel.setText(currentState == SetupState.READY
                ? "Auto Mode — The fractal is forming. Drag vertices to morph it live."
                : instructionLabel.getText());
        }
    }

    private void simulateRest() {
        if (!isManualMode || currentState != SetupState.READY) return;
        isManualMode = false;
        modeBtn.setText("Switch to Manual Mode");
        diceBtn.setDisable(true);
        simulateRestBtn.setDisable(true);
        playPauseBtn.setDisable(false);
        speedSlider.setDisable(false);
        instructionLabel.setText("Auto Mode — The fractal is forming. Drag vertices to morph it live.");
        if (!isPlaying) { timer.start(); isPlaying = true; }
    }

    private void togglePlayPause() {
        if (currentState != SetupState.READY || isManualMode) return;
        if (isPlaying) timer.stop(); else timer.start();
        isPlaying = !isPlaying;
    }

    // ── Dice roll + laser ────────────────────────────────────────────────────

    private void rollDiceAndAnimate() {
        if (!isManualMode || isAnimatingLaser || currentState != SetupState.READY) return;
        isAnimatingLaser = true;
        diceBtn.setDisable(true);
        diceResultLabel.setText("");

        int finalRoll   = random.nextInt(6) + 1;
        int targetIndex = (finalRoll - 1) / 2;
        Color targetColor = colors[targetIndex];
        String vertexName = new String[]{"A", "B", "C"}[targetIndex];

        int[] spinFrames = new int[16];
        for (int i = 0; i < 15; i++) spinFrames[i] = (i % 6) + 1;
        spinFrames[15] = finalRoll;

        diceTimeline = new Timeline();
        for (int i = 0; i < spinFrames.length; i++) {
            final int face = spinFrames[i];
            final boolean last = (i == spinFrames.length - 1);
            double delayMs = 40 + i * 18.0;
            diceTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(delayMs), ev -> {
                drawDiceFace(face);
                if (last) {
                    diceResultLabel.setText("Rolled  " + finalRoll + "  →  Vertex " + vertexName);
                    diceResultLabel.setTextFill(targetColor);
                    fireLaser(targetIndex);
                }
            }));
        }
        diceTimeline.play();
    }

    private void drawDiceFace(int value) {
        GraphicsContext dg = diceCanvas.getGraphicsContext2D();
        double W = diceCanvas.getWidth(), H = diceCanvas.getHeight(), r = 14;
        dg.clearRect(0, 0, W, H);
        dg.setFill(Color.web("#b91c1c"));
        dg.fillRoundRect(4, 4, W - 8, H - 8, r, r);
        dg.setStroke(Color.web("#fca5a5")); dg.setLineWidth(2);
        dg.strokeRoundRect(4, 4, W - 8, H - 8, r, r);
        dg.setFill(Color.WHITE);
        for (double[] p : pipPositions(value, W, H)) dg.fillOval(p[0] - 7, p[1] - 7, 14, 14);
    }

    private double[][] pipPositions(int value, double W, double H) {
        double l = W * 0.27, c = W * 0.50, r2 = W * 0.73;
        double t = H * 0.27, m = H * 0.50, b2 = H * 0.73;
        return switch (value) {
            case 1 -> new double[][]{{c, m}};
            case 2 -> new double[][]{{l, t}, {r2, b2}};
            case 3 -> new double[][]{{l, t}, {c, m}, {r2, b2}};
            case 4 -> new double[][]{{l, t}, {r2, t}, {l, b2}, {r2, b2}};
            case 5 -> new double[][]{{l, t}, {r2, t}, {c, m}, {l, b2}, {r2, b2}};
            case 6 -> new double[][]{{l, t}, {r2, t}, {l, m}, {r2, m}, {l, b2}, {r2, b2}};
            default -> new double[][]{};
        };
    }

    private void fireLaser(int targetIndex) {
        Circle target   = vertices[targetIndex];
        Color  col      = colors[targetIndex];
        double fraction = distSlider.getValue();
        double newX = currentPoint.getX() + (target.getCenterX() - currentPoint.getX()) * fraction;
        double newY = currentPoint.getY() + (target.getCenterY() - currentPoint.getY()) * fraction;

        Line laser = new Line(currentPoint.getX(), currentPoint.getY(),
                              currentPoint.getX(), currentPoint.getY());
        laser.setStroke(col); laser.setStrokeWidth(3);
        DropShadow glow = new DropShadow(14, col);
        glow.setInput(new Glow(0.8));
        laser.setEffect(glow);
        sandbox.getChildren().add(laser);

        laserTimeline = new Timeline(new KeyFrame(Duration.millis(380),
            new KeyValue(laser.endXProperty(), newX, javafx.animation.Interpolator.EASE_OUT),
            new KeyValue(laser.endYProperty(), newY, javafx.animation.Interpolator.EASE_OUT)
        ));
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

    // ── Canvas click setup ───────────────────────────────────────────────────

    private void setupCanvasClicks() {
        canvas.setOnMouseClicked(e -> {
            switch (currentState) {
                case PLACE_VERTEX_1 -> {
                    vertices[0] = createVertex(e.getX(), e.getY(), colors[0], "A");
                    sandbox.getChildren().addAll(vertices[0], vertexLabels[0]);
                    currentState = SetupState.PLACE_VERTEX_2;
                    instructionLabel.setText("Step 2: Click to place Vertex B (Magenta)");
                }
                case PLACE_VERTEX_2 -> {
                    vertices[1] = createVertex(e.getX(), e.getY(), colors[1], "B");
                    sandbox.getChildren().addAll(vertices[1], vertexLabels[1]);
                    currentState = SetupState.PLACE_VERTEX_3;
                    instructionLabel.setText("Step 3: Click to place Vertex C (Yellow)");
                }
                case PLACE_VERTEX_3 -> {
                    vertices[2] = createVertex(e.getX(), e.getY(), colors[2], "C");
                    sandbox.getChildren().addAll(vertices[2], vertexLabels[2]);
                    currentState = SetupState.PLACE_INITIAL_POINT;
                    instructionLabel.setText("Step 4: Click anywhere to drop the starting point");
                }
                case PLACE_INITIAL_POINT -> {
                    currentPoint = new Point2D(e.getX(), e.getY());
                    gc.setFill(Color.WHITE);
                    gc.fillOval(e.getX() - 4, e.getY() - 4, 8, 8);
                    currentState = SetupState.READY;
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
                }
                default -> {}
            }
        });
    }

    // ── Vertex circle creation ───────────────────────────────────────────────

    private Circle createVertex(double x, double y, Color color, String name) {
        Circle c = new Circle(x, y, 10, color);
        c.setStroke(Color.WHITE); c.setStrokeWidth(2);
        c.setEffect(new DropShadow(16, color));

        int idx = "A".equals(name) ? 0 : "B".equals(name) ? 1 : 2;
        Label lbl = new Label(name);
        lbl.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:" + toHex(color) + ";");
        lbl.setLayoutX(x + 13); lbl.setLayoutY(y - 10);
        vertexLabels[idx] = lbl;

        c.setOnMouseDragged(e -> {
            if (currentState != SetupState.READY) return;
            double nx = Math.max(0, Math.min(canvas.getWidth(),  e.getX()));
            double ny = Math.max(0, Math.min(canvas.getHeight(), e.getY()));
            c.setCenterX(nx); c.setCenterY(ny);
            lbl.setLayoutX(nx + 13); lbl.setLayoutY(ny - 10);
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
        if (timer         != null) timer.stop();
        if (laserTimeline != null) laserTimeline.stop();
        if (diceTimeline  != null) diceTimeline.stop();
        isPlaying = false; isAnimatingLaser = false;

        // Remove only vertex circles, laser lines, and vertex labels — canvas stays in place
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
        if (laserTimeline != null && laserTimeline.getStatus() == Animation.Status.RUNNING) {
            laserTimeline.stop();
            isAnimatingLaser = false;
            if (isManualMode) diceBtn.setDisable(false);
        }
        sandbox.getChildren().removeIf(n -> n instanceof Line);
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        double w = canvas.getWidth()  == 0 ? 800 : canvas.getWidth();
        double h = canvas.getHeight() == 0 ? 800 : canvas.getHeight();
        currentPoint = new Point2D(w / 2, h / 2);
    }

    // ── Auto engine ──────────────────────────────────────────────────────────

    private void initEngine() {
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
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
