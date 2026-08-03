package com.mathamorphosis.ui.visualizations;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class LeastSquaresView extends BorderPane {

    private final Pane gridPane;
    private final List<Circle> dataPoints = new ArrayList<>();
    
    private final ToggleButton userGuessToggle;
    private final Button calcBestFitBtn;
    private final ToggleButton showErrorsToggle;
    
    private final Label instructionLabel;

    private Line userGuessLine;
    private int userGuessClicks = 0;
    private double guessX1, guessY1;

    private Line bestFitLine;
    private final List<Line> errorLines = new ArrayList<>();
    private final List<Rectangle> errorSquares = new ArrayList<>();

    private boolean bestFitActive = false;
    
    // Coordinate System
    private final double WIDTH = 1000;
    private final double HEIGHT = 600;
    private final double MARGIN = 50;
    private final double GRAPH_W = WIDTH - 2 * MARGIN;
    private final double GRAPH_H = HEIGHT - 2 * MARGIN;
    
    private final double X_MAX = 20;
    private final double Y_MAX = 20;

    public LeastSquaresView() {
        this.getStyleClass().add("root");

        gridPane = new Pane();
        gridPane.setStyle("-fx-background-color: #1c1c38; -fx-background-radius: 12px; -fx-border-color: #32325a; -fx-border-radius: 12px; -fx-border-width: 2px;");
        gridPane.setPrefSize(WIDTH, HEIGHT);
        
        drawAxesAndGrid();

        gridPane.setOnMouseClicked(this::handleGridClick);

        VBox topBox = new VBox(10);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(10, 0, 10, 0));
        
        Label title = new Label("Least Squares Regression Sandbox");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        
        instructionLabel = new Label("Click anywhere on the grid to add data points. Drag them to adjust.");
        instructionLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6868a0;");
        
        topBox.getChildren().addAll(title, instructionLabel);

        HBox controls = new HBox(20);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(20));

        userGuessToggle = new ToggleButton("1. Draw Your Guess Line");
        userGuessToggle.setStyle("-fx-font-size: 14px; -fx-text-fill: #b0b0d0; -fx-background-color: #22224a; -fx-padding: 10px 20px; -fx-background-radius: 8px; -fx-cursor: hand;");
        userGuessToggle.setOnAction(e -> {
            if (!userGuessToggle.isSelected()) {
                clearUserGuess();
                instructionLabel.setText("Guess line cancelled.");
            } else {
                userGuessClicks = 0;
                instructionLabel.setText("Click two points on the grid to draw your guess.");
            }
        });

        calcBestFitBtn = new Button("2. Calculate Best Fit");
        calcBestFitBtn.setStyle("-fx-font-size: 14px; -fx-text-fill: #5ba8e0; -fx-font-weight: bold; -fx-background-color: #22224a; -fx-padding: 10px 20px; -fx-border-color: #5ba8e0; -fx-border-radius: 8px; -fx-border-width: 2px; -fx-cursor: hand;");
        calcBestFitBtn.setOnAction(e -> calculateBestFit());
        
        calcBestFitBtn.setOnMouseEntered(e -> calcBestFitBtn.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-color: #5ba8e0; -fx-padding: 10px 20px; -fx-border-color: #5ba8e0; -fx-border-radius: 8px; -fx-border-width: 2px; -fx-cursor: hand;"));
        calcBestFitBtn.setOnMouseExited(e -> calcBestFitBtn.setStyle("-fx-font-size: 14px; -fx-text-fill: #5ba8e0; -fx-font-weight: bold; -fx-background-color: #22224a; -fx-padding: 10px 20px; -fx-border-color: #5ba8e0; -fx-border-radius: 8px; -fx-border-width: 2px; -fx-cursor: hand;"));


        showErrorsToggle = new ToggleButton("3. Show Error Squares");
        showErrorsToggle.setStyle("-fx-font-size: 14px; -fx-text-fill: #b0b0d0; -fx-background-color: #22224a; -fx-padding: 10px 20px; -fx-background-radius: 8px; -fx-cursor: hand;");
        showErrorsToggle.setOnAction(e -> updateVisuals());

        controls.getChildren().addAll(userGuessToggle, calcBestFitBtn, showErrorsToggle);

        this.setTop(topBox);
        this.setCenter(gridPane);
        this.setBottom(controls);
        BorderPane.setMargin(gridPane, new Insets(10, 40, 10, 40));
    }

    private double toScreenX(double mathX) {
        return MARGIN + (mathX / X_MAX) * GRAPH_W;
    }
    
    private double toScreenY(double mathY) {
        return HEIGHT - MARGIN - (mathY / Y_MAX) * GRAPH_H;
    }
    
    private double toMathX(double screenX) {
        return ((screenX - MARGIN) / GRAPH_W) * X_MAX;
    }
    
    private double toMathY(double screenY) {
        return ((HEIGHT - MARGIN - screenY) / GRAPH_H) * Y_MAX;
    }

    private void drawAxesAndGrid() {
        // Grid lines
        for (int i = 0; i <= X_MAX; i++) {
            double sx = toScreenX(i);
            Line vLine = new Line(sx, MARGIN, sx, HEIGHT - MARGIN);
            vLine.setStroke(Color.web("#32325a", 0.5));
            gridPane.getChildren().add(vLine);
            
            if (i % 5 == 0 && i > 0) {
                Text label = new Text(String.valueOf(i));
                label.setFill(Color.web("#6868a0"));
                label.setFont(Font.font("Segoe UI", 12));
                label.setX(sx - 5);
                label.setY(HEIGHT - MARGIN + 20);
                gridPane.getChildren().add(label);
            }
        }
        for (int i = 0; i <= Y_MAX; i++) {
            double sy = toScreenY(i);
            Line hLine = new Line(MARGIN, sy, WIDTH - MARGIN, sy);
            hLine.setStroke(Color.web("#32325a", 0.5));
            gridPane.getChildren().add(hLine);
            
            if (i % 5 == 0 && i > 0) {
                Text label = new Text(String.valueOf(i));
                label.setFill(Color.web("#6868a0"));
                label.setFont(Font.font("Segoe UI", 12));
                label.setX(MARGIN - 25);
                label.setY(sy + 5);
                gridPane.getChildren().add(label);
            }
        }
        
        // Axes
        Line xAxis = new Line(MARGIN, HEIGHT - MARGIN, WIDTH - MARGIN + 10, HEIGHT - MARGIN);
        xAxis.setStroke(Color.web("#6868a0"));
        xAxis.setStrokeWidth(2);
        
        Line yAxis = new Line(MARGIN, HEIGHT - MARGIN, MARGIN, MARGIN - 10);
        yAxis.setStroke(Color.web("#6868a0"));
        yAxis.setStrokeWidth(2);
        
        gridPane.getChildren().addAll(xAxis, yAxis);
    }

    private void handleGridClick(MouseEvent event) {
        if (event.getTarget() instanceof Circle) return;

        double mx = toMathX(event.getX());
        double my = toMathY(event.getY());
        
        if (mx < 0 || mx > X_MAX || my < 0 || my > Y_MAX) return; // Ignore clicks outside graph

        if (userGuessToggle.isSelected()) {
            if (userGuessClicks == 0) {
                guessX1 = event.getX();
                guessY1 = event.getY();
                userGuessClicks++;
                instructionLabel.setText("First point set. Click a second point.");
            } else if (userGuessClicks == 1) {
                clearUserGuess();
                userGuessLine = new Line(guessX1, guessY1, event.getX(), event.getY());
                userGuessLine.setStroke(Color.web("#b0b0d0"));
                userGuessLine.setStrokeWidth(3);
                userGuessLine.getStrokeDashArray().addAll(10d, 10d);
                userGuessLine.setEffect(new DropShadow(5, Color.BLACK));
                gridPane.getChildren().add(userGuessLine);
                extendLine(userGuessLine);
                userGuessClicks = 0;
                userGuessToggle.setSelected(false);
                instructionLabel.setText("Guess line drawn! Now calculate the true best fit.");
            }
            return;
        }

        addPoint(mx, my);
    }

    private void extendLine(Line line) {
        double x1 = line.getStartX();
        double y1 = line.getStartY();
        double x2 = line.getEndX();
        double y2 = line.getEndY();

        if (x1 == x2) return;

        double m = (y2 - y1) / (x2 - x1);
        double b = y1 - m * x1;

        double newX1 = MARGIN;
        double newY1 = m * MARGIN + b;
        double newX2 = WIDTH - MARGIN;
        double newY2 = m * (WIDTH - MARGIN) + b;

        line.setStartX(newX1);
        line.setStartY(newY1);
        line.setEndX(newX2);
        line.setEndY(newY2);
    }

    private void clearUserGuess() {
        if (userGuessLine != null) {
            gridPane.getChildren().remove(userGuessLine);
            userGuessLine = null;
        }
    }

    private void addPoint(double mathX, double mathY) {
        double sx = toScreenX(mathX);
        double sy = toScreenY(mathY);
        
        Circle point = new Circle(sx, sy, 8, Color.web("#facc15"));
        point.setStroke(Color.WHITE);
        point.setStrokeWidth(2);
        point.setCursor(javafx.scene.Cursor.HAND);
        
        DropShadow shadow = new DropShadow(8, Color.web("#facc15"));
        point.setEffect(shadow);
        
        point.setOnMouseEntered(e -> point.setRadius(10));
        point.setOnMouseExited(e -> point.setRadius(8));
        
        point.setOnMouseDragged(e -> {
            double nx = e.getX();
            double ny = e.getY();
            // clamp
            if (nx < MARGIN) nx = MARGIN;
            if (nx > WIDTH - MARGIN) nx = WIDTH - MARGIN;
            if (ny < MARGIN) ny = MARGIN;
            if (ny > HEIGHT - MARGIN) ny = HEIGHT - MARGIN;
            
            point.setCenterX(nx);
            point.setCenterY(ny);
            updateVisuals();
        });

        dataPoints.add(point);
        gridPane.getChildren().add(point);

        // Animation popup
        point.setRadius(0);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(300), new KeyValue(point.radiusProperty(), 8))
        );
        timeline.play();

        updateVisuals();
    }

    private void calculateBestFit() {
        if (dataPoints.size() < 2) {
            instructionLabel.setText("Please add at least 2 points first!");
            return;
        }
        bestFitActive = true;
        
        if (userGuessToggle.isSelected()) {
            userGuessToggle.setSelected(false);
            clearUserGuess();
        }
        
        instructionLabel.setText("Mathematical Best Fit calculated! Try dragging points to see it react.");
        updateVisuals();
    }

    private void updateVisuals() {
        if (!bestFitActive || dataPoints.size() < 2) return;

        double sumX = 0, sumY = 0;
        List<Double> mXs = new ArrayList<>();
        List<Double> mYs = new ArrayList<>();
        
        for (Circle c : dataPoints) {
            double mx = toMathX(c.getCenterX());
            double my = toMathY(c.getCenterY());
            mXs.add(mx);
            mYs.add(my);
            sumX += mx;
            sumY += my;
        }
        
        double meanX = sumX / dataPoints.size();
        double meanY = sumY / dataPoints.size();

        double num = 0, den = 0;
        for (int i = 0; i < dataPoints.size(); i++) {
            num += (mXs.get(i) - meanX) * (mYs.get(i) - meanY);
            den += Math.pow(mXs.get(i) - meanX, 2);
        }

        double m = den == 0 ? 0 : num / den;
        double b = meanY - m * meanX;

        if (bestFitLine == null) {
            bestFitLine = new Line();
            bestFitLine.setStroke(Color.web("#5ba8e0")); // Electric blue
            bestFitLine.setStrokeWidth(5);
            bestFitLine.setEffect(new DropShadow(15, Color.web("#5ba8e0")));
            gridPane.getChildren().add(bestFitLine);
        }

        // Calculate screen coordinates for the math line
        double startMathX = 0;
        double startMathY = m * startMathX + b;
        double endMathX = X_MAX;
        double endMathY = m * endMathX + b;

        bestFitLine.setStartX(toScreenX(startMathX));
        bestFitLine.setStartY(toScreenY(startMathY));
        bestFitLine.setEndX(toScreenX(endMathX));
        bestFitLine.setEndY(toScreenY(endMathY));

        gridPane.getChildren().removeAll(errorLines);
        gridPane.getChildren().removeAll(errorSquares);
        errorLines.clear();
        errorSquares.clear();

        if (showErrorsToggle.isSelected()) {
            for (Circle c : dataPoints) {
                double cx = c.getCenterX();
                double cy = c.getCenterY();
                
                double mx = toMathX(cx);
                double mathLineY = m * mx + b;
                double lineY = toScreenY(mathLineY);

                Line errorLine = new Line(cx, cy, cx, lineY);
                errorLine.setStroke(Color.web("#f43f5e"));
                errorLine.setStrokeWidth(2);
                errorLine.getStrokeDashArray().addAll(5d, 5d);
                errorLines.add(errorLine);

                // Draw physical math square. 
                // Since screen scale for X and Y are not equal, a "square" in math looks like a rectangle on screen.
                // But typically for variance visualization, we draw a square visually based on the vertical distance.
                double screenDist = Math.abs(cy - lineY);
                if (screenDist > 0) {
                    Rectangle rect = new Rectangle();
                    rect.setWidth(screenDist);
                    rect.setHeight(screenDist);
                    rect.setFill(Color.web("#f43f5e", 0.2));
                    rect.setStroke(Color.web("#f43f5e"));
                    rect.setStrokeWidth(1.5);
                    
                    // Draw to the right of the error line
                    rect.setX(cx);
                    rect.setY(Math.min(cy, lineY));
                    
                    errorSquares.add(rect);
                }
            }
            gridPane.getChildren().addAll(errorSquares);
            gridPane.getChildren().addAll(errorLines);
            bestFitLine.toFront();
            for (Circle c : dataPoints) {
                c.toFront();
            }
        }
    }
}
