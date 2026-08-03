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
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

public class SieveView extends VBox {

    private Canvas canvas;
    private GraphicsContext gc;
    private static final int CELL_SIZE = 65;
    private static final int GRID_SIZE = 10;
    private static final double CANVAS_SIZE = CELL_SIZE * GRID_SIZE;

    private Color[] primeColors = {
            Color.web("#39ff14"), // 2: Neon Green
            Color.web("#5ba8e0"), // 3: Neon Blue
            Color.web("#ffff00"), // 5: Yellow
            Color.web("#b026ff")  // 7: Purple
    };
    
    private final Color COLOR_BG = Color.web("#14142a");
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

    public SieveView() {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(20);
        this.setPadding(new Insets(20));

        canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
        gc = canvas.getGraphicsContext2D();

        HBox controlBox = new HBox(15);
        controlBox.setAlignment(Pos.CENTER);
        
        Button startBtn = new Button("Start");
        startBtn.getStyleClass().add("back-button");
        Button pauseBtn = new Button("Pause");
        pauseBtn.getStyleClass().add("back-button");
        Button restartBtn = new Button("Restart");
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

        this.getChildren().addAll(canvas, controlBox);

        initCells();
        draw();
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
                }
                break;

            case FIND_PRIME:
                while (currentPrime <= 10 && cells[currentPrime].state == -1) {
                    currentPrime++;
                }
                if (currentPrime > 7) {
                    phase = Phase.FLARE_PRIMES;
                    animProgress = 0;
                } else {
                    cells[currentPrime].state = 1; // Mark as prime
                    cells[currentPrime].color = primeColors[getPrimeIndex(currentPrime)];
                    phase = Phase.PULSE_PRIME;
                    animProgress = 0;
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
                }
                break;

            case SWEEP_MULTIPLE:
                jumpProgress += dt / jumpDuration;
                
                if (jumpProgress >= 1.0) {
                    if (cells[currentMultiple].state != -1) {
                        cells[currentMultiple].state = -1;
                        cells[currentMultiple].color = COLOR_TEXT_ELIMINATED;
                    }
                    
                    currentMultiple += currentPrime;
                    
                    if (currentMultiple > 100) { 
                        currentPrime++;
                        phase = Phase.FIND_PRIME;
                        animProgress = 0;
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
                        cells[i].color = interpolateColor(COLOR_TEXT_IDLE, Color.web("#f97316"), flareT); 
                    } else if (cells[i].state == -1) {
                        cells[i].alpha = 1.0 - flareT; // Fade out composites
                    }
                }

                if (animProgress > 1.0) {
                    phase = Phase.CONVERGE_PRIMES;
                    animProgress = 0;
                    
                    int primeCount = 0;
                    for (int i = 1; i <= 100; i++) {
                        if (cells[i].state == 1) {
                            cells[i].startX = cells[i].x;
                            cells[i].startY = cells[i].y;
                            
                            int row = primeCount / 5;
                            int col = primeCount % 5;
                            double targetSize = CANVAS_SIZE / 5.0; // 130
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
                }
                break;

            case DONE:
                timer.stop();
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
