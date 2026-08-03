package com.mathamorphosis.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class Dashboard extends StackPane {

    private static final double CARD_W = 355;
    private static final double CARD_H = 175;

    public Dashboard(Consumer<String> onModuleSelected) {

        // ── Animated grid background for the whole dashboard ──────────────────
        Canvas bgCanvas = new Canvas();
        bgCanvas.widthProperty().bind(this.widthProperty());
        bgCanvas.heightProperty().bind(this.heightProperty());
        GraphicsContext bgGc = bgCanvas.getGraphicsContext2D();

        final double[] bgTime = {0};
        AnimationTimer bgTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                bgTime[0] += 0.008;
                drawDashboardBackground(bgGc,
                        bgCanvas.getWidth(), bgCanvas.getHeight(), bgTime[0]);
            }
        };
        bgTimer.start();

        // ── Content layer ─────────────────────────────────────────────────────
        VBox content = new VBox(40);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(50));

        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        Label title = new Label("Math-A-Morphosis");
        title.getStyleClass().add("header-text");
        Label subtitle = new Label("A Visual Mathematics Learning Studio");
        subtitle.getStyleClass().add("subheader-text");
        header.getChildren().addAll(title, subtitle);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(28);
        grid.setVgap(28);

        grid.add(makeCard("Number Theory",       "Sieve of Eratosthenes",               "#9b72d4", "NUMBER_THEORY",  "number_theory",  onModuleSelected), 0, 0);
        grid.add(makeCard("Calculus",            "Riemann Sum Convergence",              "#5ba8e0", "CALCULUS",        "calculus",       onModuleSelected), 1, 0);
        grid.add(makeCard("Linear Algebra",      "Interactive Vector Projections",       "#4ab8c4", "LINEAR_ALGEBRA",  "linear_algebra", onModuleSelected), 0, 1);
        grid.add(makeCard("Statistics",          "Least Squares Regression Sandbox",     "#d4a84b", "LEAST_SQUARES",   "statistics",     onModuleSelected), 1, 1);
        grid.add(makeCard("Trigonometry",        "Unit Circle Unroller",                 "#4cbf95", "UNIT_CIRCLE",     "trigonometry",   onModuleSelected), 0, 2);
        grid.add(makeCard("Algebra",             "2D Graphing Calculator",               "#d46b6b", "GRAPHING_CALC",   "algebra",        onModuleSelected), 1, 2);
        grid.add(makeCard("Signal Processing",   "Fourier Series Epicycles",             "#9b72d4", "FOURIER_SERIES",  "fourier",        onModuleSelected), 0, 3);
        grid.add(makeCard("Mathematical Marvels","The Chaos Game: Order from Randomness","#d4a84b", "CHAOS_GAME",      "chaos",          onModuleSelected), 1, 3);

        content.getChildren().addAll(header, grid);

        this.getChildren().addAll(bgCanvas, content);
        this.setStyle("-fx-background-color: #0c0c1e;");

        // Stop bg timer when removed from scene
        this.sceneProperty().addListener((obs, o, n) -> {
            if (n == null) bgTimer.stop();
        });
    }

    // ── Global background: subtle animated dot-grid ──────────────────────────

    private void drawDashboardBackground(GraphicsContext gc, double W, double H, double t) {
        gc.setFill(Color.web("#0c0c1e"));
        gc.fillRect(0, 0, W, H);

        // Fine grid lines
        gc.setStroke(Color.web("#1a1a3a", 0.8));
        gc.setLineWidth(0.5);
        double spacing = 40;
        for (double x = 0; x < W; x += spacing) gc.strokeLine(x, 0, x, H);
        for (double y = 0; y < H; y += spacing) gc.strokeLine(0, y, W, y);

        // Pulsing grid-intersection dots
        gc.setLineWidth(1);
        for (double x = 0; x < W; x += spacing) {
            for (double y = 0; y < H; y += spacing) {
                double pulse = 0.12 + 0.08 * Math.sin(t + x * 0.05 + y * 0.05);
                gc.setFill(Color.web("#5ba8e0", pulse));
                gc.fillOval(x - 1.5, y - 1.5, 3, 3);
            }
        }
    }

    // ── Card factory ─────────────────────────────────────────────────────────

    private StackPane makeCard(String titleText, String descText,
                               String accentHex, String moduleKey, String animType,
                               Consumer<String> onModuleSelected) {

        Color accent = Color.web(accentHex);

        // Animated canvas — same size as card, hidden when not hovered
        Canvas anim = new Canvas(CARD_W, CARD_H);
        anim.setOpacity(0);
        GraphicsContext gc = anim.getGraphicsContext2D();

        // State object for each card's animation
        AnimState state = new AnimState();

        AnimationTimer timer = new AnimationTimer() {
            @Override public void handle(long now) {
                state.t += 0.018;
                drawCardAnimation(gc, animType, accent, state);
            }
        };

        // Label content
        Label titleLbl = new Label(titleText);
        titleLbl.setStyle(
            "-fx-font-size:21px; -fx-font-weight:bold; -fx-text-fill:" + accentHex + ";"
        );
        Label descLbl = new Label(descText);
        descLbl.setStyle(
            "-fx-font-size:13px; -fx-text-fill:#8888aa; -fx-wrap-text:true;"
        );
        VBox text = new VBox(10, titleLbl, descLbl);
        text.setAlignment(Pos.CENTER_LEFT);
        text.setPadding(new Insets(28));
        text.setMaxWidth(CARD_W);
        text.setPickOnBounds(false);

        // Card shell
        StackPane card = new StackPane(anim, text);
        card.setPrefSize(CARD_W, CARD_H);
        card.setMinSize(CARD_W, CARD_H);
        card.setMaxSize(CARD_W, CARD_H);
        card.setStyle(
            "-fx-background-color:#14142a;" +
            "-fx-background-radius:12px;" +
            "-fx-border-color:" + accentHex + ";" +
            "-fx-border-radius:12px;" +
            "-fx-border-width:1.5px;" +
            "-fx-cursor:hand;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 4);"
        );
        card.setAlignment(Pos.CENTER_LEFT);

        // Hover: fade anim in/out
        card.setOnMouseEntered(e -> {
            state.reset();
            timer.start();
            card.setStyle(
                "-fx-background-color:#0c0c22;" +
                "-fx-background-radius:12px;" +
                "-fx-border-color:" + accentHex + ";" +
                "-fx-border-radius:12px;" +
                "-fx-border-width:2.5px;" +
                "-fx-cursor:hand;" +
                "-fx-effect: dropshadow(three-pass-box, " + toRgba(accent, 0.5) + ", 22, 0, 0, 0);"
            );
            fadeCanvas(anim, 0, 1, 250);
        });
        card.setOnMouseExited(e -> {
            timer.stop();
            card.setStyle(
                "-fx-background-color:#14142a;" +
                "-fx-background-radius:12px;" +
                "-fx-border-color:" + accentHex + ";" +
                "-fx-border-radius:12px;" +
                "-fx-border-width:1.5px;" +
                "-fx-cursor:hand;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 4);"
            );
            fadeCanvas(anim, 1, 0, 200);
        });

        card.setOnMouseClicked(e -> onModuleSelected.accept(moduleKey));

        return card;
    }

    // ── Per-card animation dispatcher ─────────────────────────────────────────

    private void drawCardAnimation(GraphicsContext gc, String type, Color accent, AnimState s) {
        gc.clearRect(0, 0, CARD_W, CARD_H);
        switch (type) {
            case "number_theory"  -> drawNumberTheory(gc, accent, s);
            case "calculus"       -> drawCalculus(gc, accent, s);
            case "linear_algebra" -> drawLinearAlgebra(gc, accent, s);
            case "statistics"     -> drawStatistics(gc, accent, s);
            case "trigonometry"   -> drawTrigonometry(gc, accent, s);
            case "algebra"        -> drawAlgebra(gc, accent, s);
            case "fourier"        -> drawFourier(gc, accent, s);
            case "chaos"          -> drawChaos(gc, accent, s);
        }
    }

    // ── 1. Number Theory: prime numbers drifting across the background ────────
    private void drawNumberTheory(GraphicsContext gc, Color accent, AnimState s) {
        if (s.primes == null) {
            s.primes = new ArrayList<>();
            int[] ps = {2,3,5,7,11,13,17,19,23,29,31,37,41,43,47,53,59,61,67,71,73,79,83,89,97,101,103,107,109,113};
            Random rng = new Random(42);
            for (int p : ps) {
                double[] d = { rng.nextDouble()*CARD_W, rng.nextDouble()*CARD_H,
                               (rng.nextDouble()-0.5)*0.4, (rng.nextDouble()-0.5)*0.4,
                               p, 9 + rng.nextDouble()*8 };
                s.primes.add(d);
            }
        }
        for (double[] p : s.primes) {
            p[0] += p[2]; p[1] += p[3];
            if (p[0] < -20) p[0] = CARD_W + 10;
            if (p[0] > CARD_W + 20) p[0] = -10;
            if (p[1] < -20) p[1] = CARD_H + 10;
            if (p[1] > CARD_H + 20) p[1] = -10;
            double alpha = 0.12 + 0.1 * Math.sin(s.t * 1.2 + p[4] * 0.3);
            gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
            gc.setFont(Font.font("Monospace", FontWeight.BOLD, p[5]));
            gc.fillText(String.valueOf((int)p[4]), p[0], p[1]);
        }
        // Sieve-style grid of dots that light up for primes
        double dotSpacing = 18;
        int col = 0, row = 0;
        double startX = CARD_W - 90, startY = 8;
        for (int n = 2; n <= 60; n++) {
            double dx = startX + col * dotSpacing;
            double dy = startY + row * dotSpacing;
            boolean isPrime = isPrime(n);
            double a = isPrime ? (0.35 + 0.2 * Math.sin(s.t * 2 + n * 0.5)) : 0.07;
            gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), a));
            gc.fillOval(dx - 3, dy - 3, isPrime ? 7 : 4, isPrime ? 7 : 4);
            col++;
            if (col > 4) { col = 0; row++; }
        }
    }

    private boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i = 2; i * i <= n; i++) if (n % i == 0) return false;
        return true;
    }

    // ── 2. Calculus: animated Riemann rectangles converging to curve ──────────
    private void drawCalculus(GraphicsContext gc, Color accent, AnimState s) {
        int n = Math.max(2, (int)(2 + 18 * ((Math.sin(s.t * 0.5) + 1) / 2.0)));
        double padL = 30, padR = 20, padT = 20, padB = 30;
        double pW = CARD_W - padL - padR, pH = CARD_H - padT - padB;
        // Axis
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.2));
        gc.setLineWidth(1);
        gc.strokeLine(padL, padT + pH, padL + pW, padT + pH);
        gc.strokeLine(padL, padT, padL, padT + pH);
        // Rectangles
        for (int i = 0; i < n; i++) {
            double xMid = (i + 0.5) / n;
            double y = 0.15 + 0.7 * Math.sin(xMid * Math.PI);
            double rx = padL + i * pW / n;
            double rw = pW / n - 1;
            double rh = y * pH;
            double alpha = 0.10 + 0.07 / n;
            gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
            gc.fillRect(rx, padT + pH - rh, rw, rh);
            gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.25));
            gc.setLineWidth(0.5);
            gc.strokeRect(rx, padT + pH - rh, rw, rh);
        }
        // Curve
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.55));
        gc.setLineWidth(2);
        gc.beginPath();
        for (int i = 0; i <= 100; i++) {
            double xr = i / 100.0;
            double yr = 0.15 + 0.7 * Math.sin(xr * Math.PI);
            double sx = padL + xr * pW;
            double sy = padT + pH - yr * pH;
            if (i == 0) gc.moveTo(sx, sy); else gc.lineTo(sx, sy);
        }
        gc.stroke();
    }

    // ── 3. Linear Algebra: two rotating vectors and their projection ──────────
    private void drawLinearAlgebra(GraphicsContext gc, Color accent, AnimState s) {
        double cx = CARD_W * 0.72, cy = CARD_H * 0.5, r = 55;
        // Rotating vector A
        double axA = cx + r * Math.cos(s.t * 0.7);
        double ayA = cy + r * Math.sin(s.t * 0.7);
        // Fixed vector B at a diagonal
        double axB = cx + r * Math.cos(0.8);
        double ayB = cy + r * Math.sin(0.8);
        // Draw projection dashed line
        double dot = (axA-cx)*(axB-cx) + (ayA-cy)*(ayB-cy);
        double bLen2 = (axB-cx)*(axB-cx) + (ayB-cy)*(ayB-cy);
        double projX = cx + dot / bLen2 * (axB - cx);
        double projY = cy + dot / bLen2 * (ayB - cy);
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.18));
        gc.setLineWidth(1); gc.setLineDashes(4, 4);
        gc.strokeLine(axA, ayA, projX, projY);
        gc.setLineDashes();
        // projection dot
        gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.5));
        gc.fillOval(projX - 4, projY - 4, 8, 8);
        // Vector A (rotating, bright)
        drawArrow(gc, cx, cy, axA, ayA, accent, 0.7, 2.5);
        // Vector B (fixed, dimmer)
        drawArrow(gc, cx, cy, axB, ayB, accent, 0.35, 1.5);
        // origin dot
        gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.5));
        gc.fillOval(cx - 4, cy - 4, 8, 8);
        // Grid lines behind
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.07));
        gc.setLineWidth(0.5);
        for (double gx = 10; gx < CARD_W; gx += 22) gc.strokeLine(gx, 0, gx, CARD_H);
        for (double gy = 5; gy < CARD_H; gy += 22) gc.strokeLine(0, gy, CARD_W, gy);
    }

    private void drawArrow(GraphicsContext gc, double x1, double y1,
                           double x2, double y2, Color c, double alpha, double lw) {
        gc.setStroke(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
        gc.setLineWidth(lw);
        gc.strokeLine(x1, y1, x2, y2);
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double al = 10;
        gc.setFill(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
        gc.fillPolygon(
            new double[]{x2, x2 - al*Math.cos(angle-0.4), x2 - al*Math.cos(angle+0.4)},
            new double[]{y2, y2 - al*Math.sin(angle-0.4), y2 - al*Math.sin(angle+0.4)}, 3);
    }

    // ── 4. Statistics: scatter dots forming a regression line ─────────────────
    private void drawStatistics(GraphicsContext gc, Color accent, AnimState s) {
        if (s.points == null) {
            s.points = new ArrayList<>();
            Random rng = new Random(7);
            for (int i = 0; i < 22; i++) {
                double xv = rng.nextDouble();
                double yv = 0.2 + 0.6 * xv + (rng.nextDouble() - 0.5) * 0.25;
                s.points.add(new double[]{xv, yv});
            }
        }
        double padL = 25, padT = 18, pW = CARD_W - padL - 15, pH = CARD_H - padT - 22;
        // Axes
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.2));
        gc.setLineWidth(1);
        gc.strokeLine(padL, padT + pH, padL + pW, padT + pH);
        gc.strokeLine(padL, padT, padL, padT + pH);
        // Regression line (animates in)
        double prog = Math.min(1.0, s.t / 3.0);
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.5 * prog));
        gc.setLineWidth(2);
        gc.strokeLine(padL, padT + pH * (1 - (0.2 + 0.0)*prog),
                padL + pW * prog, padT + pH * (1 - (0.2 + 0.6)*prog));
        // Dots
        for (int i = 0; i < s.points.size(); i++) {
            double[] pt = s.points.get(i);
            double appear = Math.min(1.0, Math.max(0, s.t - i * 0.12));
            double sx = padL + pt[0] * pW;
            double sy = padT + pH - pt[1] * pH;
            double alpha = 0.55 * appear;
            gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
            gc.fillOval(sx - 4, sy - 4, 8, 8);
            // residual line
            double predY = padT + pH - (0.2 + 0.6 * pt[0]) * pH;
            gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.15 * appear));
            gc.setLineWidth(1);
            gc.strokeLine(sx, sy, sx, predY);
        }
    }

    // ── 5. Trigonometry: spinning unit circle with radius arm ─────────────────
    private void drawTrigonometry(GraphicsContext gc, Color accent, AnimState s) {
        double cx = CARD_W * 0.68, cy = CARD_H * 0.5, r = 52;
        // Axes
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.2));
        gc.setLineWidth(1);
        gc.strokeLine(cx - r - 8, cy, cx + r + 8, cy);
        gc.strokeLine(cx, cy - r - 8, cx, cy + r + 8);
        // Circle
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.3));
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
        // Rotating radius
        double angle = s.t * 0.9;
        double hx = cx + r * Math.cos(angle);
        double hy = cy - r * Math.sin(angle);
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.75));
        gc.setLineWidth(2);
        gc.strokeLine(cx, cy, hx, hy);
        // Sine projection (vertical dashed line)
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.4));
        gc.setLineDashes(4, 4); gc.setLineWidth(1.2);
        gc.strokeLine(hx, hy, hx, cy);
        gc.setLineDashes();
        // Cosine projection (horizontal dashed line)
        gc.setStroke(new Color(accent.getRed()+0.1, accent.getGreen(), accent.getBlue(), 0.3));
        gc.setLineDashes(4, 4); gc.setLineWidth(1.2);
        gc.strokeLine(hx, cy, cx, cy);
        gc.setLineDashes();
        // Handle dot
        gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.85));
        gc.fillOval(hx - 5, hy - 5, 10, 10);
        // Trailing sine wave on the right
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.4));
        gc.setLineWidth(1.5);
        gc.beginPath();
        for (int i = 0; i <= 80; i++) {
            double t2 = angle - i * 0.08;
            double wx = (cx - r - 15) - i * 1.2;
            double wy = cy - Math.sin(t2) * r * 0.7;
            if (wx < 8) break;
            if (i == 0) gc.moveTo(wx, wy); else gc.lineTo(wx, wy);
        }
        gc.stroke();
        // theta label
        gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.45));
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        gc.fillText("θ", cx + 12, cy - 5);
    }

    // ── 6. Algebra: animated parabola + function labels scrolling ─────────────
    private void drawAlgebra(GraphicsContext gc, Color accent, AnimState s) {
        double padL = 30, padT = 18, pW = CARD_W - padL - 15, pH = CARD_H - padT - 25;
        // Axes
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.2));
        gc.setLineWidth(1);
        gc.strokeLine(padL, padT + pH, padL + pW, padT + pH);
        gc.strokeLine(padL, padT, padL, padT + pH);
        gc.strokeLine(padL + pW / 2, padT, padL + pW / 2, padT + pH);
        // Animated parabola (shift parameter oscillates)
        double shift = Math.sin(s.t * 0.6) * 0.3;
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.6));
        gc.setLineWidth(2);
        gc.beginPath();
        for (int i = 0; i <= 100; i++) {
            double xr = i / 100.0;
            double xm = (xr - 0.5 - shift);
            double yr = Math.min(1, 4 * xm * xm);
            double sx = padL + xr * pW;
            double sy = padT + yr * pH;
            if (i == 0) gc.moveTo(sx, sy); else gc.lineTo(sx, sy);
        }
        gc.stroke();
        // Second curve: sine
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.3));
        gc.setLineWidth(1.5);
        gc.beginPath();
        for (int i = 0; i <= 100; i++) {
            double xr = i / 100.0;
            double yr = 0.5 - 0.4 * Math.sin((xr * 2 * Math.PI) + s.t);
            double sx = padL + xr * pW;
            double sy = padT + yr * pH;
            if (i == 0) gc.moveTo(sx, sy); else gc.lineTo(sx, sy);
        }
        gc.stroke();
        // Floating equation labels
        String[] eqs = {"y=x²","f(x)","ax+b","y=|x|"};
        for (int i = 0; i < eqs.length; i++) {
            double ey = (padT + 12) + i * 26 + 8 * Math.sin(s.t * 0.5 + i * 1.3);
            double alpha = 0.12 + 0.07 * Math.sin(s.t + i);
            gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
            gc.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
            gc.fillText(eqs[i], 5, ey);
        }
    }

    // ── 7. Fourier / Signal Processing: sine waves combining ──────────────────
    private void drawFourier(GraphicsContext gc, Color accent, AnimState s) {
        double midY = CARD_H * 0.5;
        double ampScale = CARD_H * 0.35;
        // Individual harmonics (faint)
        int harmonics = 5;
        for (int k = 1; k <= harmonics; k++) {
            double alpha = 0.08 + 0.04 / k;
            gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
            gc.setLineWidth(1);
            gc.beginPath();
            for (int i = 0; i <= (int)CARD_W; i++) {
                double x = i;
                double y = midY - (ampScale / k) * Math.sin(k * (x / CARD_W) * Math.PI * 4 + s.t * k * 0.6);
                if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
            }
            gc.stroke();
        }
        // Combined wave (bright)
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.65));
        gc.setLineWidth(2.2);
        gc.beginPath();
        for (int i = 0; i <= (int)CARD_W; i++) {
            double x = i;
            double y = midY;
            for (int k = 1; k <= harmonics; k += 2) {
                y -= (ampScale / k) * Math.sin(k * (x / CARD_W) * Math.PI * 4 + s.t * 0.5);
            }
            if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
        }
        gc.stroke();
        // Spinning epicycle on the right
        double ecx = CARD_W * 0.85, ecy = CARD_H * 0.5, er = 22;
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.25));
        gc.setLineWidth(1);
        gc.strokeOval(ecx - er, ecy - er, er * 2, er * 2);
        double ehx = ecx + er * Math.cos(s.t);
        double ehy = ecy + er * Math.sin(s.t);
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.55));
        gc.setLineWidth(1.8);
        gc.strokeLine(ecx, ecy, ehx, ehy);
        gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.7));
        gc.fillOval(ehx - 4, ehy - 4, 8, 8);
    }

    // ── 8. Chaos Game / Mathematical Marvels: Sierpiński dots appearing ────────
    private void drawChaos(GraphicsContext gc, Color accent, AnimState s) {
        if (s.chaosPoints == null) {
            s.chaosPoints = new ArrayList<>();
            // pre-generate Sierpiński chaos-game points
            double px = CARD_W * 0.5, py = CARD_H * 0.1;
            double[] vx = {CARD_W*0.15, CARD_W*0.85, CARD_W*0.5};
            double[] vy = {CARD_H*0.92, CARD_H*0.92, CARD_H*0.08};
            Random rng = new Random(17);
            for (int i = 0; i < 1800; i++) {
                int v = rng.nextInt(3);
                px = (px + vx[v]) / 2;
                py = (py + vy[v]) / 2;
                s.chaosPoints.add(new double[]{px, py});
            }
        }
        int visible = Math.min(s.chaosPoints.size(), (int)(s.t * 60));
        for (int i = 0; i < visible; i++) {
            double[] pt = s.chaosPoints.get(i);
            double alpha = 0.25 + 0.1 * Math.sin(s.t * 1.5 + i * 0.02);
            gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
            gc.fillRect(pt[0], pt[1], 1.8, 1.8);
        }
        // Loop: reset when all shown
        if (visible >= s.chaosPoints.size()) s.t = 0;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static class AnimState {
        double t = 0;
        List<double[]> primes;
        List<double[]> points;
        List<double[]> chaosPoints;

        void reset() {
            t = 0;
            // keep pre-generated lists for chaos/stats to avoid GC jitter
        }
    }

    private String toRgba(Color c, double alpha) {
        return String.format("rgba(%d,%d,%d,%.2f)",
            (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255), alpha);
    }

    /** Simple opacity animation via AnimationTimer tick. */
    private void fadeCanvas(Canvas c, double from, double to, long durationMs) {
        final long[] start = {-1};
        c.setOpacity(from);
        AnimationTimer ft = new AnimationTimer() {
            @Override public void handle(long now) {
                if (start[0] < 0) start[0] = now;
                double prog = Math.min(1.0, (now - start[0]) / (durationMs * 1_000_000.0));
                c.setOpacity(from + (to - from) * prog);
                if (prog >= 1.0) this.stop();
            }
        };
        ft.start();
    }
}
