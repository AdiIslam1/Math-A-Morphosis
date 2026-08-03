package com.mathamorphosis.ui;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StartupScreen extends StackPane {

    private Canvas canvas;
    private GraphicsContext gc;
    private AnimationTimer timer;
    private List<Particle> particles;
    private final int NUM_PARTICLES = 50;
    
    private final String[] SYMBOLS = {"∫", "Σ", "π", "∞", "√", "θ", "Δ"};
    private final Color[] PALETTE = {
        Color.web("#39ff14"), // Neon Green
        Color.web("#5ba8e0"), // Neon Blue
        Color.web("#ffff00"), // Neon Yellow
        Color.web("#b026ff"), // Neon Purple
        Color.web("#f97316"), // Neon Orange
        Color.web("#ff00ff")  // Magenta
    };
    private Random random = new Random();

    public StartupScreen(Runnable onStart, double width, double height) {
        this.getStyleClass().add("root");

        // 1. Background Animation Canvas
        canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();
        particles = new ArrayList<>();
        for (int i = 0; i < NUM_PARTICLES; i++) {
            particles.add(new Particle(width, height));
        }

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                draw(width, height);
            }
        };
        timer.start();

        // 2. Foreground UI
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setMouseTransparent(true); // Let clicks pass through to StackPane

        Label title = new Label("Math-A-Morphosis");
        title.getStyleClass().add("header-text");
        title.setStyle("-fx-font-size: 72px;");

        Label prompt = new Label("Click to start visualizing");
        prompt.getStyleClass().add("subheader-text");
        prompt.setStyle("-fx-font-size: 24px; -fx-text-fill: #5ba8e0;");

        // Pulse Animation for the prompt
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(prompt.opacityProperty(), 1.0)),
                new KeyFrame(Duration.seconds(1), new KeyValue(prompt.opacityProperty(), 0.3)),
                new KeyFrame(Duration.seconds(2), new KeyValue(prompt.opacityProperty(), 1.0))
        );
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();

        contentBox.getChildren().addAll(title, prompt);

        this.getChildren().addAll(canvas, contentBox);

        // 3. Transition interaction
        this.setOnMouseClicked(e -> {
            // Disable clicks immediately
            this.setDisable(true);
            timer.stop();
            
            // Fade out the entire StackPane
            FadeTransition ft = new FadeTransition(Duration.millis(800), this);
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setOnFinished(evt -> onStart.run());
            ft.play();
        });
    }

    private void draw(double width, double height) {
        gc.setFill(Color.web("#14142a"));
        gc.fillRect(0, 0, width, height);

        // Update particles
        for (Particle p : particles) {
            p.update(width, height);
        }

        // Draw connections
        gc.setLineWidth(1);
        for (int i = 0; i < particles.size(); i++) {
            for (int j = i + 1; j < particles.size(); j++) {
                Particle p1 = particles.get(i);
                Particle p2 = particles.get(j);
                double dx = p1.x - p2.x;
                double dy = p1.y - p2.y;
                double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < 150) {
                    double alpha = 1.0 - (dist / 150.0);
                    // Create a gradient-like effect using p1's color
                    gc.setStroke(new Color(p1.color.getRed(), p1.color.getGreen(), p1.color.getBlue(), alpha * 0.6));
                    gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }

        // Draw particles
        for (Particle p : particles) {
            if (p.symbol != null) {
                gc.setFill(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), 0.9));
                gc.setFont(Font.font("Inter", 24));
                gc.fillText(p.symbol, p.x - 12, p.y + 12);
            } else {
                gc.setFill(p.color);
                gc.fillOval(p.x - p.radius, p.y - p.radius, p.radius * 2, p.radius * 2);
            }
        }
    }

    private class Particle {
        double x, y;
        double vx, vy;
        double radius;
        String symbol;
        Color color;

        Particle(double w, double h) {
            x = random.nextDouble() * w;
            y = random.nextDouble() * h;
            vx = (random.nextDouble() - 0.5) * 1.5;
            vy = (random.nextDouble() - 0.5) * 1.5;
            radius = random.nextDouble() * 3 + 1.5;
            color = PALETTE[random.nextInt(PALETTE.length)];

            // 15% chance to be a math symbol instead of a dot
            if (random.nextDouble() < 0.15) {
                symbol = SYMBOLS[random.nextInt(SYMBOLS.length)];
            }
        }

        void update(double w, double h) {
            x += vx;
            y += vy;

            // Bounce off walls
            if (x < 0 || x > w) vx = -vx;
            if (y < 0 || y > h) vy = -vy;
        }
    }
}
