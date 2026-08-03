package com.mathamorphosis.ui.visualizations;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class SieveView extends BorderPane {

    private Canvas canvas;
    private GraphicsContext gc;
    private static final int CELL_SIZE = 60;
    private static final int GRID_SIZE = 10;
    private static final double CANVAS_SIZE = CELL_SIZE * GRID_SIZE;

    private Color[] primeColors = {
            Color.web("#4cbf95"), // 2: Emerald Green
            Color.web("#5ba8e0"), // 3: Sky Blue
            Color.web("#d4a84b"), // 5: Warm Gold
            Color.web("#9b72d4")  // 7: Purple
    };
    
    private final Color COLOR_BG = Color.web("#0c0c1e");
    private final Color COLOR_TEXT_IDLE = Color.web("#5ba8e0"); // Light blue numbers
    private final Color COLOR_TEXT_ELIMINATED = Color.web("#32325a"); // Dim gray-blue when pressed

    private AnimationTimer timer;
    private Cell[] cells;
    
    private enum Phase { INIT, ELIMINATE_ONE, FIND_PRIME, PULSE_PRIME, SWEEP_MULTIPLE, FLARE_PRIMES, CONVERGE_PRIMES, DONE }
    private Phase phase = Phase.INIT;
    
    private double animProgress = 0;
    private int currentPrime = 2;
    private int currentMultiple = 4;
    private double jumpProgress = 0.0;
    private double jumpDuration = 0.4; // 0.4 seconds per jump
    private double gridAlpha = 1.0;
    private boolean isRunning = false;

    // UI explanation panel elements
    private Label stepTitleLabel;
    private Label stepDescLabel;
    private Label primesCountLabel;
    private Label eliminatedCountLabel;
    private Label statusBadgeLabel;

    public SieveView() {
        this.setStyle("-fx-background-color: #0c0c1e;");
        this.setPadding(new Insets(20));

        // Center Visualization Layout
        VBox centerBox = new VBox(15);
        centerBox.setAlignment(Pos.CENTER);

        // Header
        Label headerTitle = new Label("Sieve of Eratosthenes");
        headerTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #f0f0f8;");
        
        canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
        gc = canvas.getGraphicsContext2D();

        HBox controlBox = new HBox(15);
        controlBox.setAlignment(Pos.CENTER);
        
        Button startBtn = new Button("▶  Start");
        startBtn.getStyleClass().add("back-button");
        Button pauseBtn = new Button("⏸  Pause");
        pauseBtn.getStyleClass().add("back-button");
        Button restartBtn = new Button("⟳  Restart");
        restartBtn.getStyleClass().add("back-button");

        startBtn.setOnAction(e -> {
            if (timer != null) {
                timer.start();
                isRunning = true;
            } else {
                startAnimation();
            }
        });
        
        pauseBtn.setOnAction(e -> {
            if (timer != null) {
                timer.stop();
                isRunning = false;
            }
        });

        restartBtn.setOnAction(e -> startAnimation());
        
        controlBox.getChildren().addAll(startBtn, pauseBtn, restartBtn);

        centerBox.getChildren().addAll(headerTitle, canvas, controlBox);
        this.setCenter(centerBox);

        // Right Info & Explanation Side Panel
        VBox rightPanel = buildExplanationPanel();
        this.setRight(rightPanel);

        initCells();
        draw();
        updateExplanationText();
    }

    private VBox buildExplanationPanel() {
        VBox panel = new VBox(16);
        panel.setPrefWidth(320);
        panel.setPadding(new Insets(10, 16, 20, 20));
        panel.setStyle(
            "-fx-background-color: #14142a; " +
            "-fx-border-color: #32325a; -fx-border-width: 0 0 0 1;"
        );

        Label panelHeader = new Label("📌 INSTRUCTIONS & CONCEPT");
        panelHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #6868a0; -fx-letter-spacing: 1px;");

        // Live Step Tracker Card
        statusBadgeLabel = new Label("READY");
        statusBadgeLabel.setStyle(
            "-fx-background-color: #22224a; -fx-text-fill: #5ba8e0; " +
            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4;"
        );

        stepTitleLabel = new Label("Initialization");
        stepTitleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #5ba8e0;");

        stepDescLabel = new Label("Press Start to begin the algorithm. Watch how prime numbers are discovered and composite numbers are sieved out.");
        stepDescLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #b0b0d0; -fx-wrap-text: true;");

        VBox stepCard = new VBox(8, statusBadgeLabel, stepTitleLabel, stepDescLabel);
        stepCard.setPadding(new Insets(14));
        stepCard.setStyle(
            "-fx-background-color: #1c1c38; -fx-border-color: #5ba8e0; " +
            "-fx-border-width: 0 0 0 3; -fx-border-radius: 0 6 6 0; -fx-background-radius: 6;"
        );

        // Stats Card
        Label statsHeader = new Label("📊 LIVE STATS");
        statsHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6868a0;");

        primesCountLabel = new Label("Primes Found: 0");
        primesCountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #4cbf95;");

        eliminatedCountLabel = new Label("Eliminated: 0 / 100");
        eliminatedCountLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #d46b6b;");

        VBox statsCard = new VBox(6, statsHeader, primesCountLabel, eliminatedCountLabel);
        statsCard.setPadding(new Insets(12, 14, 12, 14));
        statsCard.setStyle(
            "-fx-background-color: #1c1c38; -fx-border-color: #32325a; " +
            "-fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;"
        );

        // How it works / Algorithm Rules Card
        Label rulesHeader = new Label("💡 HOW THE SIEVE WORKS");
        rulesHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6868a0;");

        Label rule1 = new Label("1. Start with grid of 1–100.");
        rule1.setStyle("-fx-font-size: 12px; -fx-text-fill: #b0b0d0;");
        Label rule2 = new Label("2. 1 is neither prime nor composite.");
        rule2.setStyle("-fx-font-size: 12px; -fx-text-fill: #b0b0d0;");
        Label rule3 = new Label("3. Pick smallest un-eliminated (e.g. 2). It's PRIME!");
        rule3.setStyle("-fx-font-size: 12px; -fx-text-fill: #4cbf95; -fx-font-weight: bold;");
        Label rule4 = new Label("4. Eliminate all multiples (4, 6, 8, 10...).");
        rule4.setStyle("-fx-font-size: 12px; -fx-text-fill: #b0b0d0;");
        Label rule5 = new Label("5. Repeat for 3, 5, 7. Stop at √100 = 10!");
        rule5.setStyle("-fx-font-size: 12px; -fx-text-fill: #d4a84b;");

        VBox rulesCard = new VBox(6, rulesHeader, rule1, rule2, rule3, rule4, rule5);
        rulesCard.setPadding(new Insets(12, 14, 12, 14));
        rulesCard.setStyle(
            "-fx-background-color: #1c1c38; -fx-border-color: #32325a; " +
            "-fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;"
        );

        panel.getChildren().addAll(panelHeader, stepCard, statsCard, rulesCard);
        return panel;
    }

    private void updateExplanationText() {
        int primeCount = 0;
        int eliminatedCount = 0;

        if (cells != null) {
            for (int i = 1; i <= 100; i++) {
                if (cells[i].state == 1) primeCount++;
                if (cells[i].state == -1) eliminatedCount++;
            }
        }

        primesCountLabel.setText("Primes Found: " + primeCount);
        eliminatedCountLabel.setText("Eliminated: " + eliminatedCount + " / 100");

        switch (phase) {
            case INIT:
                statusBadgeLabel.setText("READY");
                statusBadgeLabel.setStyle("-fx-background-color: #22224a; -fx-text-fill: #5ba8e0; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4;");
                stepTitleLabel.setText("Grid Loaded (1 to 100)");
                stepDescLabel.setText("Press 'Start' to begin the sieve process. We will systematically find primes and eliminate composites.");
                break;

            case ELIMINATE_ONE:
                statusBadgeLabel.setText("STEP 1");
                statusBadgeLabel.setStyle("-fx-background-color: #4a222a; -fx-text-fill: #d46b6b; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4;");
                stepTitleLabel.setText("Eliminating 1");
                stepDescLabel.setText("Number 1 is excluded because prime numbers must have exactly two distinct positive divisors: 1 and itself.");
                break;

            case FIND_PRIME:
                statusBadgeLabel.setText("SCANNING");
                statusBadgeLabel.setStyle("-fx-background-color: #223c32; -fx-text-fill: #4cbf95; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4;");
                stepTitleLabel.setText("Finding Next Prime...");
                stepDescLabel.setText("Scanning the grid for the smallest remaining number that has not been eliminated.");
                break;

            case PULSE_PRIME:
                statusBadgeLabel.setText("PRIME FOUND");
                statusBadgeLabel.setStyle("-fx-background-color: #223c32; -fx-text-fill: #4cbf95; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4;");
                stepTitleLabel.setText("Prime Discovered: " + currentPrime);
                stepDescLabel.setText(currentPrime + " is prime! Next, we will jump through the grid to eliminate all multiples of " + currentPrime + ".");
                break;

            case SWEEP_MULTIPLE:
                statusBadgeLabel.setText("SWEEPING");
                statusBadgeLabel.setStyle("-fx-background-color: #4a3c22; -fx-text-fill: #d4a84b; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4;");
                stepTitleLabel.setText("Eliminating Multiples of " + currentPrime);
                stepDescLabel.setText("Bouncing to " + currentMultiple + " (" + currentPrime + " × " + (currentMultiple / currentPrime) + "). Since it's divisible by " + currentPrime + ", it is marked composite.");
                break;

            case FLARE_PRIMES:
                statusBadgeLabel.setText("COMPLETE");
                statusBadgeLabel.setStyle("-fx-background-color: #38224a; -fx-text-fill: #9b72d4; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4;");
                stepTitleLabel.setText("Sieve Finished! (up to √100 = 10)");
                stepDescLabel.setText("We've checked all prime factors up to √100 = 10. Every remaining unmarked number in the grid is guaranteed to be PRIME!");
                break;

            case CONVERGE_PRIMES:
            case DONE:
                statusBadgeLabel.setText("ALL PRIMES");
                statusBadgeLabel.setStyle("-fx-background-color: #223c32; -fx-text-fill: #4cbf95; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4;");
                stepTitleLabel.setText("25 Primes Discovered");
                stepDescLabel.setText("The 25 prime numbers between 1 and 100 are:\n2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97.");
                break;
        }
    }

    private void initCells() {
        cells = new Cell[101];
        for (int i = 1; i <= 100; i++) {
            int row = (i - 1) / GRID_SIZE;
            int col = (i - 1) % GRID_SIZE;
            double x = col * CELL_SIZE + CELL_SIZE / 2.0;
            double y = row * CELL_SIZE + CELL_SIZE / 2.0;
            cells[i] = new Cell(i, x, y);
        }
        phase = Phase.INIT;
        gridAlpha = 1.0;
        animProgress = 0;
        updateExplanationText();
    }

    private void startAnimation() {
        if (timer != null) timer.stop();
        initCells();
        
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
                if (dt > 0.1) dt = 0.016; // Cap dt to avoid huge jumps after pausing
                update(dt);
                draw();
            }
            
            @Override
            public void stop() {
                super.stop();
                lastUpdate = 0; 
            }
        };
        timer.start();
        isRunning = true;
    }

    private void update(double dt) {
        switch (phase) {
            case INIT:
                animProgress += dt;
                if (animProgress > 1.0) { // Wait 1 sec
                    phase = Phase.ELIMINATE_ONE;
                    animProgress = 0;
                    updateExplanationText();
                }
                break;

            case ELIMINATE_ONE:
                animProgress += dt * 2; // 0.5s duration
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
                while (currentPrime <= 10 && cells[currentPrime].state == -1) {
                    currentPrime++;
                }
                if (currentPrime > 7) {
                    phase = Phase.FLARE_PRIMES;
                    animProgress = 0;
                    updateExplanationText();
                } else {
                    cells[currentPrime].state = 1; // Mark as prime
                    cells[currentPrime].color = primeColors[getPrimeIndex(currentPrime)];
                    phase = Phase.PULSE_PRIME;
                    animProgress = 0;
                    updateExplanationText();
                }
                break;

            case PULSE_PRIME:
                animProgress += dt * 2; // 0.5s pulse
                if (animProgress <= 0.5) {
                    // scale up
                    cells[currentPrime].scale = 1.0 + (0.5 * (animProgress * 2));
                } else if (animProgress <= 1.0) {
                    // scale down
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
                animProgress += dt; // 1 sec duration
                double flareT = Math.min(1.0, animProgress);
                
                for (int i = 1; i <= 100; i++) {
                    if (cells[i].state == 0) cells[i].state = 1;
                    if (cells[i].state == 1) {
                        cells[i].color = interpolateColor(COLOR_TEXT_IDLE, Color.web("#d4a84b"), flareT); 
                    } else if (cells[i].state == -1) {
                        cells[i].alpha = 1.0 - flareT; // Fade out composites
                    }
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
                            
                            int row = primeCount / 5;
                            int col = primeCount % 5;
                            double targetSize = CANVAS_SIZE / 5.0; // 120
                            cells[i].targetX = col * targetSize + targetSize / 2.0;
                            cells[i].targetY = row * targetSize + targetSize / 2.0;
                            cells[i].targetScale = (targetSize - 20) / (CELL_SIZE - 16); 
                            primeCount++;
                        }
                    }
                }
                break;

            case CONVERGE_PRIMES:
                double duration = 2.0; // 2 seconds to slide into 5x5 grid
                animProgress += dt / duration;
                double t = Math.min(1.0, animProgress);
                
                // Smooth ease-in-out cubic interpolation
                double ease = t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
                
                for (int i = 1; i <= 100; i++) {
                    if (cells[i].state == 1) {
                        cells[i].x = cells[i].startX + (cells[i].targetX - cells[i].startX) * ease;
                        cells[i].y = cells[i].startY + (cells[i].targetY - cells[i].startY) * ease;
                        cells[i].scale = 1.0 + (cells[i].targetScale - 1.0) * ease;
                    }
                }
                
                if (animProgress >= 1.0) {
                    phase = Phase.DONE;
                    updateExplanationText();
                }
                break;

            case DONE:
                timer.stop();
                updateExplanationText();
                break;
        }
        // Smoothly update elevation for all cells
        for (int i = 1; i <= 100; i++) {
            Cell c = cells[i];
            double target = (c.state == -1) ? 0.0 : 6.0;
            if (c.elevation > target) {
                c.elevation = Math.max(target, c.elevation - dt * 25.0);
            } else if (c.elevation < target) {
                c.elevation = Math.min(target, c.elevation + dt * 25.0);
            }
        }
    }

    private void draw() {
        gc.setFill(COLOR_BG);
        gc.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Inter", FontWeight.BOLD, 22));

        for (int i = 1; i <= 100; i++) {
            Cell c = cells[i];
            
            if (c.alpha <= 0.001) continue; // Skip rendering if fully faded
            gc.setGlobalAlpha(c.alpha);
            
            double size = (CELL_SIZE - 16) * c.scale; 
            double cx = c.x - size / 2.0;
            double cy = c.y - size / 2.0;
            double arc = 20 * c.scale; // Very smooth, pill-like corners

            // Dynamic Drop Shadow (shrinks as button is pressed)
            double shadowOffset = (c.elevation / 6.0) * 8.0 * c.scale; 
            if (shadowOffset > 0) {
                gc.setFill(Color.web("#000000", 0.7));
                gc.fillRoundRect(cx + shadowOffset, cy + shadowOffset, size, size, arc, arc);
            }

            // Draw Base (3D side)
            gc.setFill(Color.web("#15151a")); // Deep gray base
            gc.fillRoundRect(cx, cy, size, size, arc, arc);

            // Draw Top
            double topY = cy - c.elevation;
            
            // Top background color transitions to flat dark when pressed
            Color topStart = Color.web("#3f3f4e"); // Smooth metallic grayish
            Color topEnd = Color.web("#282833"); 
            Color pressedColor = Color.web("#1a1a24"); 
            
            double fraction = (6.0 - c.elevation) / 6.0; // 0 when floating, 1 when pressed
            Color currentStart = interpolateColor(topStart, pressedColor, fraction);
            Color currentEnd = interpolateColor(topEnd, pressedColor, fraction);

            // Tint the top slightly if it's a prime
            if (c.state == 1) {
                currentStart = interpolateColor(currentStart, c.color, 0.2); 
                currentEnd = interpolateColor(currentEnd, c.color, 0.2); 
            }

            // Create shiny glossy gradient for elegant keys
            LinearGradient gradient = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, currentStart),
                new Stop(1, currentEnd)
            );

            gc.setFill(gradient);
            gc.fillRoundRect(cx, topY, size, size, arc, arc);
            
            // Draw shiny highlight inner ring for 3D glassy bevel
            if (c.elevation > 1.0) {
                gc.setStroke(Color.web("#ffffff", 0.15));
                gc.setLineWidth(1.5);
                gc.strokeRoundRect(cx + 1.5, topY + 1.5, size - 3, size - 3, arc - 2, arc - 2);
            }

            // Draw glowing border if it's the current prime or climax
            if (c.state == 1 && i == currentPrime && phase != Phase.DONE) {
                gc.setStroke(c.color);
                gc.setLineWidth(2 * c.scale);
                gc.strokeRoundRect(cx, topY, size, size, arc, arc);
            } else if (c.state == 1 && (phase == Phase.FLARE_PRIMES || phase == Phase.CONVERGE_PRIMES || phase == Phase.DONE)) {
                gc.setStroke(c.color);
                gc.setLineWidth(2.5 * c.scale);
                gc.strokeRoundRect(cx, topY, size, size, arc, arc);
            } else {
                gc.setStroke(Color.web("#000000", 0.6)); // Sharp dark outline
                gc.setLineWidth(1 * c.scale);
                gc.strokeRoundRect(cx, topY, size, size, arc, arc);
            }

            // Draw Text
            gc.save();
            gc.translate(c.x, c.y - c.elevation);
            gc.scale(c.scale, c.scale);
            
            gc.setFill(c.color);
            // In climax, make primes glow (shadow)
            if ((phase == Phase.FLARE_PRIMES || phase == Phase.CONVERGE_PRIMES || phase == Phase.DONE) && c.state == 1) {
                gc.setEffect(new javafx.scene.effect.DropShadow(15, c.color));
            } else if (c.state == 1 && i == currentPrime) {
                gc.setEffect(new javafx.scene.effect.DropShadow(15, c.color));
            }
            
            gc.fillText(String.valueOf(i), 0, 7); // vertical center approximation
            gc.restore();
        }

        // Draw Bouncing Ball
        if (phase == Phase.SWEEP_MULTIPLE) {
            int prevMultiple = currentMultiple - currentPrime;
            
            double startX, startY;
            if (prevMultiple > 0 && prevMultiple <= 100) {
                startX = cells[prevMultiple].x;
                startY = cells[prevMultiple].y - cells[prevMultiple].elevation;
            } else {
                int row = (prevMultiple - 1) / GRID_SIZE;
                int col = (prevMultiple - 1) % GRID_SIZE;
                startX = col * CELL_SIZE + CELL_SIZE / 2.0;
                startY = row * CELL_SIZE + CELL_SIZE / 2.0;
            }
            
            double endX, endY;
            if (currentMultiple <= 100) {
                endX = cells[currentMultiple].x;
                endY = cells[currentMultiple].y - cells[currentMultiple].elevation;
            } else {
                int row = (currentMultiple - 1) / GRID_SIZE;
                int col = (currentMultiple - 1) % GRID_SIZE;
                endX = col * CELL_SIZE + CELL_SIZE / 2.0;
                endY = row * CELL_SIZE + CELL_SIZE / 2.0;
            }
            
            double t = jumpProgress;
            double bx = startX + (endX - startX) * t;
            
            // Limit arc height to prevent jumping off the top screen boundary
            double maxArc = Math.min(80.0, startY - 10.0);
            maxArc = Math.max(10.0, maxArc);
            double by = startY + (endY - startY) * t - maxArc * Math.sin(Math.PI * t); 
            
            int pIdx = getPrimeIndex(currentPrime);
            gc.setFill(primeColors[pIdx]);
            gc.setEffect(new javafx.scene.effect.DropShadow(20, primeColors[pIdx]));
            gc.fillOval(bx - 10, by - 10, 20, 20);
            gc.setEffect(null); // reset effect
        }
        
        gc.setGlobalAlpha(1.0); // Reset alpha for next frame
    }

    private Color interpolateColor(Color c1, Color c2, double fraction) {
        double r = c1.getRed() + (c2.getRed() - c1.getRed()) * fraction;
        double g = c1.getGreen() + (c2.getGreen() - c1.getGreen()) * fraction;
        double b = c1.getBlue() + (c2.getBlue() - c1.getBlue()) * fraction;
        return new Color(r, g, b, 1.0);
    }

    private int getPrimeIndex(int p) {
        if (p == 2) return 0;
        if (p == 3) return 1;
        if (p == 5) return 2;
        if (p == 7) return 3;
        return 0; // fallback
    }

    private class Cell {
        int number;
        double x, y;
        int state = 0; 
        double scale = 1.0;
        double elevation = 6.0; 
        double alpha = 1.0;
        double startX, startY;
        double targetX, targetY, targetScale;
        Color color = COLOR_TEXT_IDLE;

        Cell(int number, double x, double y) {
            this.number = number;
            this.x = x;
            this.y = y;
        }
    }
}
