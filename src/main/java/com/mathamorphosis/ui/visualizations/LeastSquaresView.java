package com.mathamorphosis.ui.visualizations;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
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

    // Live result labels
    private final Label livePointCount;
    private final Label liveSlopeLabel;
    private final Label liveInterceptLabel;
    private final Label liveEquationLabel;

    private Line userGuessLine;
    private int userGuessClicks = 0;
    private double guessX1, guessY1;

    private Line bestFitLine;
    private final List<Line> errorLines = new ArrayList<>();
    private final List<Rectangle> errorSquares = new ArrayList<>();

    private boolean bestFitActive = false;

    // Coordinate System
    private final double WIDTH  = 1000;
    private final double HEIGHT = 680;
    private final double MARGIN = 60;
    private final double GRAPH_W = WIDTH  - 2 * MARGIN;
    private final double GRAPH_H = HEIGHT - 2 * MARGIN;

    private final double X_MAX = 20;
    private final double Y_MAX = 20;

    public LeastSquaresView() {
        this.getStyleClass().add("root");

        // ── Grid / Chart pane ────────────────────────────────────────────
        gridPane = new Pane();
        gridPane.setStyle(
                "-fx-background-color: #1c1c38;" +
                "-fx-background-radius: 12px;" +
                "-fx-border-color: #32325a;" +
                "-fx-border-radius: 12px;" +
                "-fx-border-width: 2px;"
        );
        gridPane.setMinSize(750, 520);
        gridPane.setPrefSize(WIDTH, HEIGHT);
        gridPane.setMaxSize(WIDTH, HEIGHT);
        drawAxesAndGrid();
        gridPane.setOnMouseClicked(this::handleGridClick);

        // ── Title ─────────────────────────────────────────────────────────
        Label title = new Label("Least Squares Regression Sandbox");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #f0f0f8;");

        instructionLabel = new Label("Click anywhere on the grid to add data points. Drag them to adjust.");
        instructionLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6868a0;");

        // ── Live result labels (stored for updates) ───────────────────────
        livePointCount     = makeLiveLabel("Data Points: 0",    "#f0f0f8", 18, true);
        liveSlopeLabel     = makeLiveLabel("m (slope) = \u2014",   "#4cbf95", 18, true);
        liveInterceptLabel = makeLiveLabel("b (intercept) = \u2014", "#5ba8e0", 18, true);
        liveEquationLabel  = makeLiveLabel("ŷ = \u2014",           "#d4a84b", 17, true);

        // ── Controls ──────────────────────────────────────────────────────
        userGuessToggle = new ToggleButton("1. Draw Your Guess Line");
        userGuessToggle.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #b0b0d0;" +
                "-fx-background-color: #22224a; -fx-padding: 12px 24px;" +
                "-fx-background-radius: 8px; -fx-cursor: hand;"
        );
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
        calcBestFitBtn.setStyle(
                "-fx-font-size: 15px; -fx-text-fill: #5ba8e0; -fx-font-weight: bold;" +
                "-fx-background-color: #22224a; -fx-padding: 12px 24px;" +
                "-fx-border-color: #5ba8e0; -fx-border-radius: 8px;" +
                "-fx-border-width: 2px; -fx-cursor: hand;"
        );
        calcBestFitBtn.setOnAction(e -> calculateBestFit());
        calcBestFitBtn.setOnMouseEntered(e -> calcBestFitBtn.setStyle(
                "-fx-font-size: 15px; -fx-text-fill: #ffffff; -fx-font-weight: bold;" +
                "-fx-background-color: #5ba8e0; -fx-padding: 12px 24px;" +
                "-fx-border-color: #5ba8e0; -fx-border-radius: 8px;" +
                "-fx-border-width: 2px; -fx-cursor: hand;"
        ));
        calcBestFitBtn.setOnMouseExited(e -> calcBestFitBtn.setStyle(
                "-fx-font-size: 15px; -fx-text-fill: #5ba8e0; -fx-font-weight: bold;" +
                "-fx-background-color: #22224a; -fx-padding: 12px 24px;" +
                "-fx-border-color: #5ba8e0; -fx-border-radius: 8px;" +
                "-fx-border-width: 2px; -fx-cursor: hand;"
        ));

        showErrorsToggle = new ToggleButton("3. Show Error Squares");
        showErrorsToggle.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #b0b0d0;" +
                "-fx-background-color: #22224a; -fx-padding: 12px 24px;" +
                "-fx-background-radius: 8px; -fx-cursor: hand;"
        );
        showErrorsToggle.setOnAction(e -> updateVisuals());

        HBox controls = new HBox(24);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(14, 0, 10, 0));
        controls.getChildren().addAll(userGuessToggle, calcBestFitBtn, showErrorsToggle);

        // ── Right explanation panel (Statistics) ─────────────────────────
        VBox statsPanel = buildExplanationPanel();

        // ── Side-by-side content (Graph + Statistics) ─────────────────────
        HBox mainContent = new HBox(24);
        mainContent.setAlignment(Pos.CENTER);
        mainContent.getChildren().addAll(gridPane, statsPanel);

        // ── Center VBox ───────────────────────────────────────────────────
        VBox centerBox = new VBox(14);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(16, 24, 16, 24));
        centerBox.setStyle("-fx-background-color: #0c0c1e;");
        centerBox.getChildren().addAll(title, instructionLabel, mainContent, controls);

        // ── Wire up ───────────────────────────────────────────────────────
        this.setCenter(centerBox);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Explanation panel builder
    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildExplanationPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: #14142a;");

        // Section label
        Label sectionLabel = new Label("STATISTICS & CONTROLS");
        sectionLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold;" +
                "-fx-text-fill: #6868a0; -fx-letter-spacing: 2;"
        );

        // ── WHAT IS THIS card ──────────────────────────────────────────────
        VBox whatCard = new VBox(12);
        whatCard.setStyle(
                "-fx-background-color: #1c1c38;" +
                "-fx-border-color: #d4a84b;" +
                "-fx-border-width: 0 0 0 4;" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;" +
                "-fx-padding: 20;"
        );

        Label whatTitle = new Label("Least Squares Regression");
        whatTitle.setStyle(
                "-fx-font-size: 22px; -fx-font-weight: bold;" +
                "-fx-text-fill: #d4a84b;"
        );

        Label whatDesc = new Label(
                "Place data points and find the single straight line that minimises " +
                "the total squared vertical distance from every point to the line."
        );
        whatDesc.setStyle("-fx-font-size: 16px; -fx-text-fill: #d0d0e8; -fx-line-spacing: 3;");
        whatDesc.setWrapText(true);

        whatCard.getChildren().addAll(whatTitle, whatDesc);

        // ── THE FORMULA card ───────────────────────────────────────────────
        VBox formulaCard = new VBox(10);
        formulaCard.setStyle(
                "-fx-background-color: #1c1c38;" +
                "-fx-border-color: #32325a;" +
                "-fx-border-width: 1;" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;" +
                "-fx-padding: 18;"
        );

        Label formulaHeader = new Label("THE FORMULA");
        formulaHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #6868a0;");

        Label formulaMain = new Label("ŷ = mx + b");
        formulaMain.setStyle(
                "-fx-font-size: 26px; -fx-font-weight: bold;" +
                "-fx-text-fill: #d4a84b;" +
                "-fx-font-family: 'Courier New', monospace;"
        );

        Label mLabel = new Label("m = \u03A3(x\u1D62 \u2212 x\u0305)(y\u1D62 \u2212 y\u0305) / \u03A3(x\u1D62 \u2212 x\u0305)\u00B2");
        mLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #b0b0d0;");
        mLabel.setWrapText(true);

        Label bLabel = new Label("b = y\u0305 \u2212 m\u00B7x\u0305");
        bLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #b0b0d0;");

        Label noteLabel = new Label("Where x\u0305, y\u0305 are the means of x and y");
        noteLabel.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: #6868a0;" +
                "-fx-font-style: italic;"
        );

        formulaCard.getChildren().addAll(formulaHeader, formulaMain, mLabel, bLabel, noteLabel);

        // ── LIVE RESULTS card ──────────────────────────────────────────────
        VBox liveCard = new VBox(10);
        liveCard.setStyle(
                "-fx-background-color: #1c1c38;" +
                "-fx-border-color: #32325a;" +
                "-fx-border-width: 1;" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;" +
                "-fx-padding: 18;"
        );

        Label liveHeader = new Label("LIVE RESULTS");
        liveHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #6868a0;");

        liveCard.getChildren().addAll(
                liveHeader,
                livePointCount,
                liveSlopeLabel,
                liveInterceptLabel,
                liveEquationLabel
        );

        // ── HOW TO USE card ────────────────────────────────────────────────
        VBox howCard = new VBox(8);
        howCard.setStyle(
                "-fx-background-color: #1c1c38;" +
                "-fx-border-color: #32325a;" +
                "-fx-border-width: 1;" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;" +
                "-fx-padding: 18;"
        );

        Label howHeader = new Label("HOW TO USE");
        howHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #6868a0;");

        String[] steps = {
            "1. Click grid to place data points",
            "2. Draw your guess line first",
            "3. Hit Calculate Best Fit to see the math",
            "4. Toggle error squares to visualise residuals",
            "5. Drag points to watch line react live"
        };
        howCard.getChildren().add(howHeader);
        for (String step : steps) {
            Label stepLabel = new Label(step);
            stepLabel.setStyle(
                    "-fx-font-size: 15px; -fx-text-fill: #b0b0d0;" +
                    "-fx-line-spacing: 3;"
            );
            stepLabel.setWrapText(true);
            howCard.getChildren().add(stepLabel);
        }

        // ── WHY IT MATTERS card ────────────────────────────────────────────
        VBox whyCard = new VBox(8);
        whyCard.setStyle(
                "-fx-background-color: #1c1c38;" +
                "-fx-border-color: #32325a;" +
                "-fx-border-width: 1;" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;" +
                "-fx-padding: 18;"
        );

        Label whyHeader = new Label("REAL WORLD USES");
        whyHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #6868a0;");

        String[] uses = {
            "\uD83D\uDCC8 Predicting stock trends",
            "\uD83C\uDFE5 Medical dosage modelling",
            "\uD83C\uDF21 Climate change analysis",
            "\uD83D\uDE97 Fuel efficiency estimation"
        };
        whyCard.getChildren().add(whyHeader);
        for (String use : uses) {
            Label useLabel = new Label(use);
            useLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #b0b0d0;");
            whyCard.getChildren().add(useLabel);
        }

        panel.getChildren().addAll(
                sectionLabel,
                whatCard,
                formulaCard,
                liveCard,
                howCard,
                whyCard
        );

        // Wrap in a ScrollPane so it degrades gracefully at small heights
        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle(
                "-fx-background: #14142a;" +
                "-fx-background-color: #14142a;" +
                "-fx-border-color: #32325a;" +
                "-fx-border-radius: 12px;" +
                "-fx-background-radius: 12px;" +
                "-fx-border-width: 2px;"
        );
        scroll.setMinSize(360, 520);
        scroll.setPrefSize(420, HEIGHT);
        scroll.setMaxSize(420, HEIGHT);

        VBox wrapper = new VBox(scroll);
        return wrapper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private Label makeLiveLabel(String text, String color, int size, boolean bold) {
        Label lbl = new Label(text);
        lbl.setStyle(
                "-fx-font-size: " + size + "px;" +
                (bold ? " -fx-font-weight: bold;" : "") +
                " -fx-text-fill: " + color + ";"
        );
        return lbl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Coordinate helpers
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    // Grid / Axes
    // ─────────────────────────────────────────────────────────────────────────

    private void drawAxesAndGrid() {
        // Vertical grid lines
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
        // Horizontal grid lines
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

    // ─────────────────────────────────────────────────────────────────────────
    // Interaction
    // ─────────────────────────────────────────────────────────────────────────

    private void handleGridClick(MouseEvent event) {
        if (event.getTarget() instanceof Circle) return;

        double mx = toMathX(event.getX());
        double my = toMathY(event.getY());

        if (mx < 0 || mx > X_MAX || my < 0 || my > Y_MAX) return;

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
        point.setOnMouseExited(e  -> point.setRadius(8));

        point.setOnMouseDragged(e -> {
            double nx = e.getX();
            double ny = e.getY();
            if (nx < MARGIN)          nx = MARGIN;
            if (nx > WIDTH - MARGIN)  nx = WIDTH - MARGIN;
            if (ny < MARGIN)          ny = MARGIN;
            if (ny > HEIGHT - MARGIN) ny = HEIGHT - MARGIN;

            point.setCenterX(nx);
            point.setCenterY(ny);
            updateVisuals();
        });

        dataPoints.add(point);
        gridPane.getChildren().add(point);

        // Pop-in animation
        point.setRadius(0);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(300), new KeyValue(point.radiusProperty(), 8))
        );
        timeline.play();

        // Update live point count
        livePointCount.setText("Data Points: " + dataPoints.size());

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
        // Always keep point count fresh
        livePointCount.setText("Data Points: " + dataPoints.size());

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

        // Update live result labels
        liveSlopeLabel.setText(String.format("m (slope) = %.4f", m));
        liveInterceptLabel.setText(String.format("b (intercept) = %.4f", b));
        liveEquationLabel.setText(String.format("ŷ = %.4fx + %.4f", m, b));

        if (bestFitLine == null) {
            bestFitLine = new Line();
            bestFitLine.setStroke(Color.web("#5ba8e0"));
            bestFitLine.setStrokeWidth(5);
            bestFitLine.setEffect(new DropShadow(15, Color.web("#5ba8e0")));
            gridPane.getChildren().add(bestFitLine);
        }

        double startMathX = 0;
        double startMathY = m * startMathX + b;
        double endMathX   = X_MAX;
        double endMathY   = m * endMathX + b;

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

                double mx    = toMathX(cx);
                double mathLineY = m * mx + b;
                double lineY = toScreenY(mathLineY);

                Line errorLine = new Line(cx, cy, cx, lineY);
                errorLine.setStroke(Color.web("#f43f5e"));
                errorLine.setStrokeWidth(2);
                errorLine.getStrokeDashArray().addAll(5d, 5d);
                errorLines.add(errorLine);

                double screenDist = Math.abs(cy - lineY);
                if (screenDist > 0) {
                    Rectangle rect = new Rectangle();
                    rect.setWidth(screenDist);
                    rect.setHeight(screenDist);
                    rect.setFill(Color.web("#f43f5e", 0.2));
                    rect.setStroke(Color.web("#f43f5e"));
                    rect.setStrokeWidth(1.5);
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
