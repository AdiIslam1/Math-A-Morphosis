package com.mathamorphosis.ui.visualizations;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class UnitCircleView extends BorderPane {

    private final Pane leftPane;
    private final Pane rightPane;
    
    private final double CIRCLE_RADIUS = 150;
    private final double LEFT_CX = 250;
    private final double LEFT_CY = 250;
    private final double RIGHT_CY = 250;
    
    private double currentAngle = 0; // In radians
    
    private final Line radiusLine;
    private final Circle handle;
    private final Polyline waveLine;
    private final Line tracerLine;
    
    private final Polygon referenceTriangle;
    private final Line refVerticalLine;
    private final Line refHorizontalLine;
    
    private final ToggleButton autoSpinToggle;
    private final ToggleButton refTriangleToggle;
    private final RadioButton sineRadio;
    private final RadioButton cosineRadio;
    
    private final AnimationTimer autoSpinTimer;

    public UnitCircleView() {
        this.getStyleClass().add("root");
        
        VBox topBox = new VBox(10);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(10, 0, 10, 0));
        
        Label title = new Label("Unit Circle Unroller");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        
        Label instructionLabel = new Label("Drag the yellow handle or click Auto-Spin to see how trigonometric waves are generated.");
        instructionLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #94a3b8;");
        
        topBox.getChildren().addAll(title, instructionLabel);
        
        HBox mainBox = new HBox(30);
        mainBox.setAlignment(Pos.CENTER);
        
        leftPane = new Pane();
        leftPane.setPrefSize(500, 500);
        leftPane.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12px; -fx-border-color: #334155; -fx-border-radius: 12px; -fx-border-width: 2px;");
        
        rightPane = new Pane();
        rightPane.setPrefSize(600, 500);
        rightPane.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12px; -fx-border-color: #334155; -fx-border-radius: 12px; -fx-border-width: 2px;");
        
        drawAxes(leftPane, rightPane);
        
        // Draw Unit Circle
        Circle unitCircle = new Circle(LEFT_CX, LEFT_CY, CIRCLE_RADIUS);
        unitCircle.setFill(Color.TRANSPARENT);
        unitCircle.setStroke(Color.web("#94a3b8", 0.5));
        unitCircle.setStrokeWidth(2);
        
        // Dynamic elements
        radiusLine = new Line(LEFT_CX, LEFT_CY, LEFT_CX + CIRCLE_RADIUS, LEFT_CY);
        radiusLine.setStroke(Color.web("#0ea5e9"));
        radiusLine.setStrokeWidth(3);
        
        handle = new Circle(LEFT_CX + CIRCLE_RADIUS, LEFT_CY, 12, Color.web("#facc15"));
        handle.setCursor(javafx.scene.Cursor.HAND);
        handle.setEffect(new DropShadow(15, Color.web("#facc15")));
        handle.setOnMouseEntered(e -> handle.setRadius(15));
        handle.setOnMouseExited(e -> handle.setRadius(12));
        
        referenceTriangle = new Polygon();
        referenceTriangle.setFill(Color.web("#0ea5e9", 0.3));
        referenceTriangle.setVisible(false);
        
        refVerticalLine = new Line();
        refVerticalLine.setStroke(Color.web("#10b981")); // sine color
        refVerticalLine.setStrokeWidth(3);
        refVerticalLine.setVisible(false);
        
        refHorizontalLine = new Line();
        refHorizontalLine.setStroke(Color.web("#f43f5e")); // cosine color
        refHorizontalLine.setStrokeWidth(3);
        refHorizontalLine.setVisible(false);
        
        leftPane.getChildren().addAll(unitCircle, referenceTriangle, refVerticalLine, refHorizontalLine, radiusLine, handle);
        
        waveLine = new Polyline();
        waveLine.setStrokeWidth(4);
        rightPane.getChildren().add(waveLine);
        
        tracerLine = new Line();
        tracerLine.setStroke(Color.web("#cbd5e1"));
        tracerLine.getStrokeDashArray().addAll(8d, 8d);
        tracerLine.setStrokeWidth(2);
        
        rightPane.getChildren().add(tracerLine);
        
        mainBox.getChildren().addAll(leftPane, rightPane);
        
        // Controls
        HBox controls = new HBox(30);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(20));
        
        ToggleGroup functionGroup = new ToggleGroup();
        sineRadio = new RadioButton("Sine (Height)");
        sineRadio.setToggleGroup(functionGroup);
        sineRadio.setSelected(true);
        sineRadio.setStyle("-fx-text-fill: #10b981; -fx-font-size: 16px; -fx-font-weight: bold; -fx-cursor: hand;");
        
        cosineRadio = new RadioButton("Cosine (Width)");
        cosineRadio.setToggleGroup(functionGroup);
        cosineRadio.setStyle("-fx-text-fill: #f43f5e; -fx-font-size: 16px; -fx-font-weight: bold; -fx-cursor: hand;");
        
        refTriangleToggle = new ToggleButton("Show Reference Triangle");
        refTriangleToggle.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1; -fx-background-color: #27354f; -fx-padding: 10px 20px; -fx-background-radius: 8px; -fx-cursor: hand;");
        refTriangleToggle.setOnAction(e -> {
            boolean show = refTriangleToggle.isSelected();
            referenceTriangle.setVisible(show);
            refVerticalLine.setVisible(show);
            refHorizontalLine.setVisible(show);
            updateVisuals();
        });
        
        autoSpinToggle = new ToggleButton("▶ Auto-Spin");
        autoSpinToggle.setStyle("-fx-font-size: 14px; -fx-text-fill: #0ea5e9; -fx-font-weight: bold; -fx-background-color: #27354f; -fx-border-color: #0ea5e9; -fx-border-radius: 8px; -fx-border-width: 2px; -fx-padding: 10px 20px; -fx-cursor: hand;");
        
        autoSpinTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                currentAngle += 0.02; // counter-clockwise in math (but angle increases)
                if (currentAngle > Math.PI * 2) {
                    currentAngle -= Math.PI * 2;
                }
                updateVisuals();
            }
        };
        
        autoSpinToggle.setOnAction(e -> {
            if (autoSpinToggle.isSelected()) {
                autoSpinToggle.setText("⏸ Stop Spin");
                autoSpinToggle.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-color: #0ea5e9; -fx-border-color: #0ea5e9; -fx-border-radius: 8px; -fx-border-width: 2px; -fx-padding: 10px 20px; -fx-cursor: hand;");
                handle.setDisable(true);
                autoSpinTimer.start();
            } else {
                autoSpinToggle.setText("▶ Auto-Spin");
                autoSpinToggle.setStyle("-fx-font-size: 14px; -fx-text-fill: #0ea5e9; -fx-font-weight: bold; -fx-background-color: #27354f; -fx-border-color: #0ea5e9; -fx-border-radius: 8px; -fx-border-width: 2px; -fx-padding: 10px 20px; -fx-cursor: hand;");
                handle.setDisable(false);
                autoSpinTimer.stop();
            }
        });
        
        controls.getChildren().addAll(sineRadio, cosineRadio, refTriangleToggle, autoSpinToggle);
        
        this.setTop(topBox);
        this.setCenter(mainBox);
        this.setBottom(controls);
        
        handle.setOnMouseDragged(e -> {
            if (!autoSpinToggle.isSelected()) {
                double dx = e.getX() - LEFT_CX;
                double dy = LEFT_CY - e.getY(); // Math Y is inverted screen Y
                currentAngle = Math.atan2(dy, dx);
                if (currentAngle < 0) currentAngle += Math.PI * 2;
                updateVisuals();
            }
        });
        
        functionGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> updateVisuals());
        
        updateVisuals();
    }
    
    private void drawAxes(Pane left, Pane right) {
        // Left Axes
        Line hAxisLeft = new Line(LEFT_CX - 220, LEFT_CY, LEFT_CX + 220, LEFT_CY);
        hAxisLeft.setStroke(Color.web("#475569"));
        hAxisLeft.setStrokeWidth(2);
        
        Line vAxisLeft = new Line(LEFT_CX, LEFT_CY - 220, LEFT_CX, LEFT_CY + 220);
        vAxisLeft.setStroke(Color.web("#475569"));
        vAxisLeft.setStrokeWidth(2);
        
        // Labels for Unit Circle
        Text l1 = new Text("1"); l1.setFill(Color.web("#94a3b8")); l1.setX(LEFT_CX + CIRCLE_RADIUS - 5); l1.setY(LEFT_CY + 20);
        Text l2 = new Text("-1"); l2.setFill(Color.web("#94a3b8")); l2.setX(LEFT_CX - CIRCLE_RADIUS - 10); l2.setY(LEFT_CY + 20);
        Text l3 = new Text("1"); l3.setFill(Color.web("#94a3b8")); l3.setX(LEFT_CX - 20); l3.setY(LEFT_CY - CIRCLE_RADIUS + 5);
        Text l4 = new Text("-1"); l4.setFill(Color.web("#94a3b8")); l4.setX(LEFT_CX - 25); l4.setY(LEFT_CY + CIRCLE_RADIUS + 5);
        
        left.getChildren().addAll(hAxisLeft, vAxisLeft, l1, l2, l3, l4);
        
        // Right Axes
        Line hAxisRight = new Line(20, RIGHT_CY, 580, RIGHT_CY);
        hAxisRight.setStroke(Color.web("#475569"));
        hAxisRight.setStrokeWidth(2);
        
        Line vAxisRight = new Line(40, 30, 40, 470);
        vAxisRight.setStroke(Color.web("#475569"));
        vAxisRight.setStrokeWidth(2);
        
        // Labels for Wave
        Text w1 = new Text("1"); w1.setFill(Color.web("#94a3b8")); w1.setX(20); w1.setY(RIGHT_CY - CIRCLE_RADIUS + 5);
        Text w2 = new Text("-1"); w2.setFill(Color.web("#94a3b8")); w2.setX(15); w2.setY(RIGHT_CY + CIRCLE_RADIUS + 5);
        Text w3 = new Text("π"); w3.setFill(Color.web("#94a3b8")); w3.setX(40 + (500/2.0) - 5); w3.setY(RIGHT_CY + 20);
        Text w4 = new Text("2π"); w4.setFill(Color.web("#94a3b8")); w4.setX(40 + 500 - 10); w4.setY(RIGHT_CY + 20);
        
        right.getChildren().addAll(hAxisRight, vAxisRight, w1, w2, w3, w4);
    }
    
    private void updateVisuals() {
        boolean isSine = sineRadio.isSelected();
        
        // Math coordinates
        double mathX = Math.cos(currentAngle);
        double mathY = Math.sin(currentAngle);
        
        // Screen coordinates
        double px = LEFT_CX + mathX * CIRCLE_RADIUS;
        double py = LEFT_CY - mathY * CIRCLE_RADIUS; // Screen Y goes down
        
        radiusLine.setEndX(px);
        radiusLine.setEndY(py);
        handle.setCenterX(px);
        handle.setCenterY(py);
        
        if (refTriangleToggle.isSelected()) {
            referenceTriangle.getPoints().setAll(
                LEFT_CX, LEFT_CY,
                px, py,
                px, LEFT_CY
            );
            refVerticalLine.setStartX(px);
            refVerticalLine.setStartY(py);
            refVerticalLine.setEndX(px);
            refVerticalLine.setEndY(LEFT_CY);
            
            refHorizontalLine.setStartX(LEFT_CX);
            refHorizontalLine.setStartY(LEFT_CY);
            refHorizontalLine.setEndX(px);
            refHorizontalLine.setEndY(LEFT_CY);
            
            // Highlight the active function
            if (isSine) {
                refVerticalLine.setStrokeWidth(5);
                refHorizontalLine.setStrokeWidth(2);
            } else {
                refVerticalLine.setStrokeWidth(2);
                refHorizontalLine.setStrokeWidth(5);
            }
        }
        
        // Update wave color and glow
        if (isSine) {
            waveLine.setStroke(Color.web("#10b981")); // Emerald
            waveLine.setEffect(new DropShadow(10, Color.web("#10b981")));
        } else {
            waveLine.setStroke(Color.web("#f43f5e")); // Rose
            waveLine.setEffect(new DropShadow(10, Color.web("#f43f5e")));
        }
        
        // Generate wave points (40 is start X of graph, 500 is width = 2 PI)
        waveLine.getPoints().clear();
        for (double t = 0; t <= currentAngle; t += 0.05) {
            double screenX = 40 + (t / (Math.PI * 2)) * 500;
            double val = isSine ? Math.sin(t) : Math.cos(t);
            double screenY = RIGHT_CY - val * CIRCLE_RADIUS;
            waveLine.getPoints().addAll(screenX, screenY);
        }
        // Add final point
        double finalScreenX = 40 + (currentAngle / (Math.PI * 2)) * 500;
        double finalScreenY = RIGHT_CY - (isSine ? mathY : mathX) * CIRCLE_RADIUS;
        waveLine.getPoints().addAll(finalScreenX, finalScreenY);
        
        // Tracer line bridging left pane to right pane
        // Tracer line is inside right pane. 0 is left edge of right pane.
        // HBox spacing is 30. Left pane width 500. So handle is at (px). Distance to right pane = 500 - px + 30.
        // So tracer starts at -(500 - px + 30).
        
        if (isSine) {
            tracerLine.setStartX(-(500 - px + 30));
            tracerLine.setStartY(py);
            tracerLine.setEndX(finalScreenX);
            tracerLine.setEndY(finalScreenY); // which is py
        } else {
            // For cosine, trace from horizontal width (px) to the top of the wave.
            tracerLine.setStartX(-(500 - px + 30));
            tracerLine.setStartY(LEFT_CY); // starts from the axis projection
            tracerLine.setEndX(finalScreenX);
            tracerLine.setEndY(finalScreenY);
        }
    }
}
