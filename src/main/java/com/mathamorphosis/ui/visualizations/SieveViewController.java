package com.mathamorphosis.ui.visualizations;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Controller for sieve_view.fxml.
 * Owns all Sieve of Eratosthenes logic, driven by FXML-injected nodes.
 */
public class SieveViewController {

    // ── FXML-injected nodes ──────────────────────────────────────────────────
    @FXML private Canvas canvas;
    @FXML private Button startBtn;
    @FXML private Button pauseBtn;
    @FXML private Button restartBtn;

    @FXML private Label statusBadgeLabel;
    @FXML private Label stepTitleLabel;
    @FXML private Label stepDescLabel;
    @FXML private Label primesCountLabel;
    @FXML private Label eliminatedCountLabel;

    // ── Constants ────────────────────────────────────────────────────────────
    private static final int    CELL_SIZE   = 60;
    private static final int    GRID_SIZE   = 10;
    private static final double CANVAS_SIZE = CELL_SIZE * GRID_SIZE;

    private final Color[] primeColors = {
        Color.web("#4cbf95"),  // 2: Emerald Green
        Color.web("#5ba8e0"),  // 3: Sky Blue
        Color.web("#d4a84b"),  // 5: Warm Gold
        Color.web("#9b72d4")   // 7: Purple
    };

    private final Color COLOR_BG             = Color.web("#0c0c1e");
    private final Color COLOR_TEXT_IDLE      = Color.web("#5ba8e0");
    private final Color COLOR_TEXT_ELIMINATED = Color.web("#32325a");

    // ── Animation state ──────────────────────────────────────────────────────
    private GraphicsContext gc;
    private AnimationTimer  timer;
    private Cell[]  cells;
    private boolean isRunning = false;

    private enum Phase { INIT, ELIMINATE_ONE, FIND_PRIME, PULSE_PRIME, SWEEP_MULTIPLE, FLARE_PRIMES, CONVERGE_PRIMES, DONE }
    private Phase  phase         = Phase.INIT;
    private double animProgress  = 0;
    private int    currentPrime  = 2;
    private int    currentMultiple = 4;
    private double jumpProgress  = 0.0;
    private final double jumpDuration = 0.4;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        gc = canvas.getGraphicsContext2D();

        startBtn.setOnAction(e -> {
            if (timer != null) { timer.start(); isRunning = true; }
            else startAnimation();
        });
        pauseBtn.setOnAction(e -> {
            if (timer != null) { timer.stop(); isRunning = false; }
        });
        restartBtn.setOnAction(e -> startAnimation());

        initCells();
        draw();
        updateExplanationText();
    }

    // ── Init / Animation control ─────────────────────────────────────────────

    private void initCells() {
        cells = new Cell[101];
        for (int i = 1; i <= 100; i++) {
            int row = (i - 1) / GRID_SIZE;
            int col = (i - 1) % GRID_SIZE;
            double x = col * CELL_SIZE + CELL_SIZE / 2.0;
            double y = row * CELL_SIZE + CELL_SIZE / 2.0;
            cells[i] = new Cell(i, x, y);
        }
        phase        = Phase.INIT;
        animProgress = 0;
        updateExplanationText();
    }

    private void startAnimation() {
        if (timer != null) timer.stop();
        initCells();

        timer = new AnimationTimer() {
            private long lastUpdate = 0;
            @Override public void handle(long now) {
                if (lastUpdate == 0) { lastUpdate = now; return; }
                double dt = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;
                if (dt > 0.1) dt = 0.016;
                update(dt);
                draw();
            }
            @Override public void stop() { super.stop(); lastUpdate = 0; }
        };
        timer.start();
        isRunning = true;
    }

    // ── Update ───────────────────────────────────────────────────────────────

    private void update(double dt) {
        switch (phase) {
            case INIT:
                animProgress += dt;
                if (animProgress > 1.0) {
                    phase = Phase.ELIMINATE_ONE;
                    animProgress = 0;
                    updateExplanationText();
                }
                break;

            case ELIMINATE_ONE:
                animProgress += dt * 2;
                if (animProgress < 1.0) {
                    cells[1].scale = 1.0 - (0.3 * animProgress);
                    cells[1].color = interpolateColor(COLOR_TEXT_IDLE, COLOR_TEXT_ELIMINATED, animProgress);
                } else {
                    cells[1].scale = 0.7;
                    cells[1].color = COLOR_TEXT_ELIMINATED;
                    cells[1].state = -1;
                    phase = Phase.FIND_PRIME;
                    animProgress = 0;
                    currentPrime = 2;
                    updateExplanationText();
                }
                break;

            case FIND_PRIME:
                while (currentPrime <= 10 && cells[currentPrime].state == -1) currentPrime++;
                if (currentPrime > 7) {
                    phase = Phase.FLARE_PRIMES;
                    animProgress = 0;
                    updateExplanationText();
                } else {
                    cells[currentPrime].state = 1;
                    cells[currentPrime].color = primeColors[getPrimeIndex(currentPrime)];
                    phase = Phase.PULSE_PRIME;
                    animProgress = 0;
                    updateExplanationText();
                }
                break;

            case PULSE_PRIME:
                animProgress += dt * 2;
                if (animProgress <= 0.5) {
                    cells[currentPrime].scale = 1.0 + (0.5 * (animProgress * 2));
                } else if (animProgress <= 1.0) {
                    cells[currentPrime].scale = 1.5 - (0.5 * ((animProgress - 0.5) * 2));
                } else {
                    cells[currentPrime].scale = 1.0;
                    phase = Phase.SWEEP_MULTIPLE;
                    currentMultiple = currentPrime * 2;
                    jumpProgress = 0;
                    updateExplanationText();
                }
                break;

            case SWEEP_MULTIPLE:
                jumpProgress += dt / jumpDuration;
                if (jumpProgress >= 1.0) {
                    if (cells[currentMultiple].state != -1) {
                        cells[currentMultiple].state = -1;
                        cells[currentMultiple].color = COLOR_TEXT_ELIMINATED;
                        updateExplanationText();
                    }
                    currentMultiple += currentPrime;
                    if (currentMultiple > 100) {
                        currentPrime++;
                        phase = Phase.FIND_PRIME;
                        animProgress = 0;
                        updateExplanationText();
                    } else {
                        jumpProgress -= 1.0;
                    }
                }
                break;

            case FLARE_PRIMES:
                animProgress += dt;
                double flareT = Math.min(1.0, animProgress);
                for (int i = 1; i <= 100; i++) {
                    if (cells[i].state == 0)  cells[i].state = 1;
                    if (cells[i].state == 1)  cells[i].color = interpolateColor(COLOR_TEXT_IDLE, Color.web("#d4a84b"), flareT);
                    else if (cells[i].state == -1) cells[i].alpha = 1.0 - flareT;
                }
                if (animProgress > 1.0) {
                    phase = Phase.CONVERGE_PRIMES;
                    animProgress = 0;
                    updateExplanationText();
                    int primeCount = 0;
                    for (int i = 1; i <= 100; i++) {
                        if (cells[i].state == 1) {
                            cells[i].startX = cells[i].x;
                            cells[i].startY = cells[i].y;
                            int row = primeCount / 5, col = primeCount % 5;
                            double targetSize = CANVAS_SIZE / 5.0;
                            cells[i].targetX     = col * targetSize + targetSize / 2.0;
                            cells[i].targetY     = row * targetSize + targetSize / 2.0;
                            cells[i].targetScale = (targetSize - 20) / (CELL_SIZE - 16);
                            primeCount++;
                        }
                    }
                }
                break;

            case CONVERGE_PRIMES:
                animProgress += dt / 2.0;
                double t    = Math.min(1.0, animProgress);
                double ease = t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
                for (int i = 1; i <= 100; i++) {
                    if (cells[i].state == 1) {
                        cells[i].x     = cells[i].startX + (cells[i].targetX - cells[i].startX) * ease;
                        cells[i].y     = cells[i].startY + (cells[i].targetY - cells[i].startY) * ease;
                        cells[i].scale = 1.0 + (cells[i].targetScale - 1.0) * ease;
                    }
                }
                if (animProgress >= 1.0) { phase = Phase.DONE; updateExplanationText(); }
                break;

            case DONE:
                timer.stop();
                updateExplanationText();
                break;
        }

        // Smooth elevation for all cells
        for (int i = 1; i <= 100; i++) {
            Cell c = cells[i];
            double target = (c.state == -1) ? 0.0 : 6.0;
            if      (c.elevation > target) c.elevation = Math.max(target, c.elevation - dt * 25.0);
            else if (c.elevation < target) c.elevation = Math.min(target, c.elevation + dt * 25.0);
        }
    }

    // ── Draw ─────────────────────────────────────────────────────────────────

    private void draw() {
        gc.setFill(COLOR_BG);
        gc.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Inter", FontWeight.BOLD, 22));

        for (int i = 1; i <= 100; i++) {
            Cell c = cells[i];
            if (c.alpha <= 0.001) continue;
            gc.setGlobalAlpha(c.alpha);

            double size = (CELL_SIZE - 16) * c.scale;
            double cx   = c.x - size / 2.0;
            double cy   = c.y - size / 2.0;
            double arc  = 20 * c.scale;

            // Drop shadow
            double shadowOffset = (c.elevation / 6.0) * 8.0 * c.scale;
            if (shadowOffset > 0) {
                gc.setFill(Color.web("#000000", 0.7));
                gc.fillRoundRect(cx + shadowOffset, cy + shadowOffset, size, size, arc, arc);
            }

            // Base (3D side)
            gc.setFill(Color.web("#15151a"));
            gc.fillRoundRect(cx, cy, size, size, arc, arc);

            // Top face
            double topY      = cy - c.elevation;
            Color  topStart  = Color.web("#3f3f4e");
            Color  topEnd    = Color.web("#282833");
            Color  pressed   = Color.web("#1a1a24");
            double fraction  = (6.0 - c.elevation) / 6.0;
            Color  curStart  = interpolateColor(topStart, pressed, fraction);
            Color  curEnd    = interpolateColor(topEnd,   pressed, fraction);

            if (c.state == 1) {
                curStart = interpolateColor(curStart, c.color, 0.2);
                curEnd   = interpolateColor(curEnd,   c.color, 0.2);
            }

            gc.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, curStart), new Stop(1, curEnd)));
            gc.fillRoundRect(cx, topY, size, size, arc, arc);

            // Gloss highlight
            if (c.elevation > 1.0) {
                gc.setStroke(Color.web("#ffffff", 0.15));
                gc.setLineWidth(1.5);
                gc.strokeRoundRect(cx + 1.5, topY + 1.5, size - 3, size - 3, arc - 2, arc - 2);
            }

            // Glow border
            if (c.state == 1 && i == currentPrime && phase != Phase.DONE) {
                gc.setStroke(c.color); gc.setLineWidth(2 * c.scale);
                gc.strokeRoundRect(cx, topY, size, size, arc, arc);
            } else if (c.state == 1 && (phase == Phase.FLARE_PRIMES || phase == Phase.CONVERGE_PRIMES || phase == Phase.DONE)) {
                gc.setStroke(c.color); gc.setLineWidth(2.5 * c.scale);
                gc.strokeRoundRect(cx, topY, size, size, arc, arc);
            } else {
                gc.setStroke(Color.web("#000000", 0.6)); gc.setLineWidth(1 * c.scale);
                gc.strokeRoundRect(cx, topY, size, size, arc, arc);
            }

            // Number text
            gc.save();
            gc.translate(c.x, c.y - c.elevation);
            gc.scale(c.scale, c.scale);
            gc.setFill(c.color);
            if ((phase == Phase.FLARE_PRIMES || phase == Phase.CONVERGE_PRIMES || phase == Phase.DONE) && c.state == 1) {
                gc.setEffect(new javafx.scene.effect.DropShadow(15, c.color));
            } else if (c.state == 1 && i == currentPrime) {
                gc.setEffect(new javafx.scene.effect.DropShadow(15, c.color));
            }
            gc.fillText(String.valueOf(i), 0, 7);
            gc.restore();
        }

        // Bouncing ball
        if (phase == Phase.SWEEP_MULTIPLE) {
            int prevMultiple = currentMultiple - currentPrime;
            double startX, startY, endX, endY;

            if (prevMultiple > 0 && prevMultiple <= 100) {
                startX = cells[prevMultiple].x;
                startY = cells[prevMultiple].y - cells[prevMultiple].elevation;
            } else {
                startX = ((prevMultiple - 1) % GRID_SIZE) * CELL_SIZE + CELL_SIZE / 2.0;
                startY = ((prevMultiple - 1) / GRID_SIZE) * CELL_SIZE + CELL_SIZE / 2.0;
            }
            if (currentMultiple <= 100) {
                endX = cells[currentMultiple].x;
                endY = cells[currentMultiple].y - cells[currentMultiple].elevation;
            } else {
                endX = ((currentMultiple - 1) % GRID_SIZE) * CELL_SIZE + CELL_SIZE / 2.0;
                endY = ((currentMultiple - 1) / GRID_SIZE) * CELL_SIZE + CELL_SIZE / 2.0;
            }

            double bx  = startX + (endX - startX) * jumpProgress;
            double arc = Math.max(10.0, Math.min(80.0, startY - 10.0));
            double by  = startY + (endY - startY) * jumpProgress - arc * Math.sin(Math.PI * jumpProgress);

            int pIdx = getPrimeIndex(currentPrime);
            gc.setFill(primeColors[pIdx]);
            gc.setEffect(new javafx.scene.effect.DropShadow(20, primeColors[pIdx]));
            gc.fillOval(bx - 10, by - 10, 20, 20);
            gc.setEffect(null);
        }

        gc.setGlobalAlpha(1.0);
    }

    // ── Explanation text ─────────────────────────────────────────────────────

    private void updateExplanationText() {
        int primeCount = 0, eliminatedCount = 0;
        if (cells != null) {
            for (int i = 1; i <= 100; i++) {
                if (cells[i].state ==  1) primeCount++;
                if (cells[i].state == -1) eliminatedCount++;
            }
        }
        primesCountLabel.setText(String.valueOf(primeCount));
        eliminatedCountLabel.setText(eliminatedCount + " / 100");

        stepTitleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #5ba8e0;");

        switch (phase) {
            case INIT:
                statusBadgeLabel.setText("READY");
                statusBadgeLabel.setStyle("-fx-background-color: #22224a; -fx-text-fill: #5ba8e0; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 6;");
                stepTitleLabel.setText("Grid Loaded (1 to 100)");
                stepDescLabel.setText("Press 'Start' to begin the algorithm. We will systematically find primes and eliminate composites.");
                break;
            case ELIMINATE_ONE:
                statusBadgeLabel.setText("STEP 1");
                statusBadgeLabel.setStyle("-fx-background-color: #4a222a; -fx-text-fill: #d46b6b; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 6;");
                stepTitleLabel.setText("Eliminating 1");
                stepDescLabel.setText("Number 1 is excluded because prime numbers must have exactly two distinct positive divisors: 1 and itself.");
                break;
            case FIND_PRIME:
                statusBadgeLabel.setText("SCANNING");
                statusBadgeLabel.setStyle("-fx-background-color: #223c32; -fx-text-fill: #4cbf95; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 6;");
                stepTitleLabel.setText("Finding Next Prime...");
                stepDescLabel.setText("Scanning the grid for the smallest remaining number that has not been eliminated.");
                break;
            case PULSE_PRIME:
                statusBadgeLabel.setText("PRIME FOUND");
                statusBadgeLabel.setStyle("-fx-background-color: #223c32; -fx-text-fill: #4cbf95; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 6;");
                stepTitleLabel.setText("Prime Discovered: " + currentPrime);
                stepTitleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + toHex(primeColors[getPrimeIndex(currentPrime)]) + ";");
                stepDescLabel.setText(currentPrime + " is prime! Next, we will jump through the grid to eliminate all multiples of " + currentPrime + ".");
                break;
            case SWEEP_MULTIPLE:
                statusBadgeLabel.setText("SWEEPING MULTIPLES");
                statusBadgeLabel.setStyle("-fx-background-color: #4a3c22; -fx-text-fill: #d4a84b; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 6;");
                stepTitleLabel.setText("Eliminating Multiples of " + currentPrime);
                stepTitleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + toHex(primeColors[getPrimeIndex(currentPrime)]) + ";");
                stepDescLabel.setText("Bouncing to " + currentMultiple + " (" + currentPrime + " × " + (currentMultiple / currentPrime) + "). Since it's divisible by " + currentPrime + ", it is marked composite.");
                break;
            case FLARE_PRIMES:
                statusBadgeLabel.setText("COMPLETE");
                statusBadgeLabel.setStyle("-fx-background-color: #38224a; -fx-text-fill: #9b72d4; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 6;");
                stepTitleLabel.setText("Sieve Finished! (up to √100 = 10)");
                stepDescLabel.setText("We've checked all prime factors up to √100 = 10. Every remaining unmarked number in the grid is guaranteed to be PRIME!");
                break;
            case CONVERGE_PRIMES:
            case DONE:
                statusBadgeLabel.setText("ALL PRIMES");
                statusBadgeLabel.setStyle("-fx-background-color: #223c32; -fx-text-fill: #4cbf95; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 6;");
                stepTitleLabel.setText("25 Primes Discovered");
                stepDescLabel.setText("The 25 prime numbers between 1 and 100 are:\n2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97.");
                break;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Color interpolateColor(Color c1, Color c2, double t) {
        return new Color(
            c1.getRed()   + (c2.getRed()   - c1.getRed())   * t,
            c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t,
            c1.getBlue()  + (c2.getBlue()  - c1.getBlue())  * t,
            1.0
        );
    }

    private int getPrimeIndex(int p) {
        return switch (p) { case 2 -> 0; case 3 -> 1; case 5 -> 2; case 7 -> 3; default -> 0; };
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x",
            (int)(c.getRed() * 255), (int)(c.getGreen() * 255), (int)(c.getBlue() * 255));
    }

    // ── Cell inner class ─────────────────────────────────────────────────────

    private class Cell {
        int    number;
        double x, y;
        int    state     = 0;
        double scale     = 1.0;
        double elevation = 6.0;
        double alpha     = 1.0;
        double startX, startY;
        double targetX, targetY, targetScale;
        Color  color     = COLOR_TEXT_IDLE;

        Cell(int number, double x, double y) {
            this.number = number;
            this.x      = x;
            this.y      = y;
        }
    }
}
