package com.mathamorphosis.ui.visualizations;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.control.Label;

/**
 * Controller for unit_circle_view.fxml.
 * Owns all Unit Circle Unroller logic, driven by FXML-injected nodes.
 */
public class UnitCircleViewController {

    // ── FXML-injected nodes ──────────────────────────────────────────────────
    @FXML private Pane         leftPane;
    @FXML private Pane         rightPane;
    @FXML private RadioButton  sineRadio;
    @FXML private RadioButton  cosineRadio;
    @FXML private RadioButton  tangentRadio;
    @FXML private ToggleButton refTriangleToggle;
    @FXML private ToggleButton autoSpinToggle;
    @FXML private Slider       speedSlider;
    @FXML private Label        speedValueLabel;

    // ── Constants ────────────────────────────────────────────────────────────
    private static final double CIRCLE_RADIUS = 150;
    private static final double LEFT_CX       = 250;
    private static final double LEFT_CY       = 250;
    private static final double RIGHT_CY      = 250;

    // ── Dynamic scene nodes (built in initialize) ────────────────────────────
    private Line     radiusLine;
    private Circle   handle;
    private Polyline waveLine;
    private Line     tracerLine;
    private Polygon  referenceTriangle;
    private Line     refVerticalLine;
    private Line     refHorizontalLine;

    // ── State ────────────────────────────────────────────────────────────────
    private double        currentAngle   = 0;
    private AnimationTimer autoSpinTimer;

    // ── Toggle button styles ─────────────────────────────────────────────────
    private static final String REF_OFF =
        "-fx-font-size: 14px; -fx-text-fill: #6868a0; -fx-font-weight: bold;" +
        "-fx-background-color: #1a1a38; -fx-border-color: #32325a; -fx-border-width: 2px;" +
        "-fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10px 20px; -fx-cursor: hand;" +
        "-fx-effect: dropshadow(gaussian, transparent, 0, 0, 0, 0);";
    private static final String REF_ON =
        "-fx-font-size: 14px; -fx-text-fill: #ffffff; -fx-font-weight: bold;" +
        "-fx-background-color: #10b981; -fx-border-color: #10b981; -fx-border-width: 2px;" +
        "-fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10px 20px; -fx-cursor: hand;" +
        "-fx-effect: dropshadow(gaussian, #10b981, 14, 0.65, 0, 0);";

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        buildSceneNodes();
        drawAxes();

        // Speed slider label
        speedSlider.valueProperty().addListener((obs, o, n) -> {
            speedValueLabel.setText(String.format("%.1f×", n.doubleValue() / 0.01));
        });

        // Reference triangle toggle
        refTriangleToggle.setStyle(REF_OFF);
        refTriangleToggle.setOnMouseEntered(e -> refTriangleToggle.setStyle(
            refTriangleToggle.isSelected() ? REF_ON :
            "-fx-font-size: 14px; -fx-text-fill: #b0b0d0; -fx-font-weight: bold;" +
            "-fx-background-color: #22224a; -fx-border-color: #5ba8e0; -fx-border-width: 2px;" +
            "-fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10px 20px; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, #5ba8e040, 8, 0.4, 0, 0);"
        ));
        refTriangleToggle.setOnMouseExited(e ->
            refTriangleToggle.setStyle(refTriangleToggle.isSelected() ? REF_ON : REF_OFF));
        refTriangleToggle.setOnAction(e -> {
            boolean show = refTriangleToggle.isSelected();
            refTriangleToggle.setStyle(show ? REF_ON : REF_OFF);
            referenceTriangle.setVisible(show);
            refVerticalLine.setVisible(show);
            refHorizontalLine.setVisible(show);
            updateVisuals();
        });

        // Auto-spin toggle
        autoSpinTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                currentAngle += speedSlider.getValue();
                if (currentAngle > Math.PI * 2) currentAngle -= Math.PI * 2;
                updateVisuals();
            }
        };
        autoSpinToggle.setOnAction(e -> {
            if (autoSpinToggle.isSelected()) {
                autoSpinToggle.setText("⏸ Stop Spin");
                autoSpinToggle.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-color: #5ba8e0; -fx-border-color: #5ba8e0; -fx-border-radius: 8px; -fx-border-width: 2px; -fx-padding: 10px 20px; -fx-cursor: hand;");
                handle.setDisable(true);
                autoSpinTimer.start();
            } else {
                autoSpinToggle.setText("▶ Auto-Spin");
                autoSpinToggle.setStyle("-fx-font-size: 14px; -fx-text-fill: #5ba8e0; -fx-font-weight: bold; -fx-background-color: #22224a; -fx-border-color: #5ba8e0; -fx-border-radius: 8px; -fx-border-width: 2px; -fx-padding: 10px 20px; -fx-cursor: hand;");
                handle.setDisable(false);
                autoSpinTimer.stop();
            }
        });

        // Handle drag
        handle.setOnMouseDragged(e -> {
            if (!autoSpinToggle.isSelected()) {
                double dx = e.getX() - LEFT_CX;
                double dy = LEFT_CY - e.getY();
                currentAngle = Math.atan2(dy, dx);
                if (currentAngle < 0) currentAngle += Math.PI * 2;
                updateVisuals();
            }
        });

        // Wire radio buttons into a shared ToggleGroup (done here since FXML
        // does not support ToggleGroup as a layout child)
        javafx.scene.control.ToggleGroup functionGroup = new javafx.scene.control.ToggleGroup();
        sineRadio.setToggleGroup(functionGroup);
        cosineRadio.setToggleGroup(functionGroup);
        tangentRadio.setToggleGroup(functionGroup);
        sineRadio.setSelected(true);

        // Function radio listener
        functionGroup.selectedToggleProperty().addListener((obs, o, n) -> updateVisuals());

        updateVisuals();
    }

    // ── Build dynamic nodes ──────────────────────────────────────────────────

    private void buildSceneNodes() {
        // Unit circle
        Circle unitCircle = new Circle(LEFT_CX, LEFT_CY, CIRCLE_RADIUS);
        unitCircle.setFill(Color.TRANSPARENT);
        unitCircle.setStroke(Color.web("#6868a0", 0.5));
        unitCircle.setStrokeWidth(2);

        radiusLine = new Line(LEFT_CX, LEFT_CY, LEFT_CX + CIRCLE_RADIUS, LEFT_CY);
        radiusLine.setStroke(Color.web("#5ba8e0"));
        radiusLine.setStrokeWidth(3);

        handle = new Circle(LEFT_CX + CIRCLE_RADIUS, LEFT_CY, 12, Color.web("#facc15"));
        handle.setCursor(javafx.scene.Cursor.HAND);
        handle.setEffect(new DropShadow(15, Color.web("#facc15")));
        handle.setOnMouseEntered(e -> handle.setRadius(15));
        handle.setOnMouseExited(e  -> handle.setRadius(12));

        referenceTriangle = new Polygon();
        referenceTriangle.setFill(Color.web("#5ba8e0", 0.3));
        referenceTriangle.setVisible(false);

        refVerticalLine = new Line();
        refVerticalLine.setStroke(Color.web("#10b981"));
        refVerticalLine.setStrokeWidth(3);
        refVerticalLine.setVisible(false);

        refHorizontalLine = new Line();
        refHorizontalLine.setStroke(Color.web("#f43f5e"));
        refHorizontalLine.setStrokeWidth(3);
        refHorizontalLine.setVisible(false);

        leftPane.getChildren().addAll(unitCircle, referenceTriangle, refVerticalLine, refHorizontalLine, radiusLine, handle);

        waveLine = new Polyline();
        waveLine.setStrokeWidth(4);
        tracerLine = new Line();
        tracerLine.setStroke(Color.web("#b0b0d0"));
        tracerLine.getStrokeDashArray().addAll(8d, 8d);
        tracerLine.setStrokeWidth(2);
        rightPane.getChildren().addAll(waveLine, tracerLine);
    }

    // ── Axes ─────────────────────────────────────────────────────────────────

    private void drawAxes() {
        // Left pane axes
        Line hL = new Line(LEFT_CX - 220, LEFT_CY, LEFT_CX + 220, LEFT_CY);
        hL.setStroke(Color.web("#32325a")); hL.setStrokeWidth(2);
        Line vL = new Line(LEFT_CX, LEFT_CY - 220, LEFT_CX, LEFT_CY + 220);
        vL.setStroke(Color.web("#32325a")); vL.setStrokeWidth(2);

        addLabel(leftPane, "1",  LEFT_CX + CIRCLE_RADIUS - 5, LEFT_CY + 20);
        addLabel(leftPane, "-1", LEFT_CX - CIRCLE_RADIUS - 10, LEFT_CY + 20);
        addLabel(leftPane, "1",  LEFT_CX - 20, LEFT_CY - CIRCLE_RADIUS + 5);
        addLabel(leftPane, "-1", LEFT_CX - 25, LEFT_CY + CIRCLE_RADIUS + 5);
        leftPane.getChildren().addAll(hL, vL);

        // Right pane axes
        Line hR = new Line(20, RIGHT_CY, 580, RIGHT_CY);
        hR.setStroke(Color.web("#32325a")); hR.setStrokeWidth(2);
        Line vR = new Line(40, 30, 40, 470);
        vR.setStroke(Color.web("#32325a")); vR.setStrokeWidth(2);

        addLabel(rightPane, "1",  20, RIGHT_CY - CIRCLE_RADIUS + 5);
        addLabel(rightPane, "-1", 15, RIGHT_CY + CIRCLE_RADIUS + 5);
        addLabel(rightPane, "π",  40 + 500 / 2.0 - 5, RIGHT_CY + 20);
        addLabel(rightPane, "2π", 40 + 500 - 10,       RIGHT_CY + 20);
        rightPane.getChildren().addAll(hR, vR);
    }

    private void addLabel(Pane pane, String text, double x, double y) {
        Text t = new Text(text);
        t.setFill(Color.web("#6868a0"));
        t.setFont(Font.font(12));
        t.setX(x); t.setY(y);
        pane.getChildren().add(t);
    }

    // ── Update visuals ───────────────────────────────────────────────────────

    private void updateVisuals() {
        boolean isSine    = sineRadio.isSelected();
        boolean isTangent = tangentRadio.isSelected();

        double mathX   = Math.cos(currentAngle);
        double mathY   = Math.sin(currentAngle);
        double mathTan = Math.tan(currentAngle);

        double px = LEFT_CX + mathX * CIRCLE_RADIUS;
        double py = LEFT_CY - mathY * CIRCLE_RADIUS;

        radiusLine.setEndX(px); radiusLine.setEndY(py);
        handle.setCenterX(px);  handle.setCenterY(py);

        if (refTriangleToggle.isSelected()) {
            referenceTriangle.getPoints().setAll(LEFT_CX, LEFT_CY, px, py, px, LEFT_CY);
            refVerticalLine.setStartX(px);   refVerticalLine.setStartY(py);
            refVerticalLine.setEndX(px);     refVerticalLine.setEndY(LEFT_CY);
            refHorizontalLine.setStartX(LEFT_CX); refHorizontalLine.setStartY(LEFT_CY);
            refHorizontalLine.setEndX(px);   refHorizontalLine.setEndY(LEFT_CY);

            if (isSine)         { refVerticalLine.setStrokeWidth(5); refHorizontalLine.setStrokeWidth(2); }
            else if (isTangent) { refVerticalLine.setStrokeWidth(4); refHorizontalLine.setStrokeWidth(4); }
            else                { refVerticalLine.setStrokeWidth(2); refHorizontalLine.setStrokeWidth(5); }
        }

        // Wave colour
        if (isSine)         { waveLine.setStroke(Color.web("#10b981")); waveLine.setEffect(new DropShadow(10, Color.web("#10b981"))); }
        else if (isTangent) { waveLine.setStroke(Color.web("#f97316")); waveLine.setEffect(new DropShadow(10, Color.web("#f97316"))); }
        else                { waveLine.setStroke(Color.web("#f43f5e")); waveLine.setEffect(new DropShadow(10, Color.web("#f43f5e"))); }

        // Build wave polyline
        waveLine.getPoints().clear();
        final double GRAPH_TOP = 30, GRAPH_BOTTOM = 470;
        for (double t = 0; t <= currentAngle; t += 0.03) {
            double sx = 40 + (t / (Math.PI * 2)) * 500;
            double sy;
            if (isTangent) {
                if (Math.abs(Math.cos(t)) < 0.08) { waveLine.getPoints().clear(); continue; }
                sy = Math.max(GRAPH_TOP, Math.min(GRAPH_BOTTOM, RIGHT_CY - Math.tan(t) * CIRCLE_RADIUS));
            } else {
                sy = RIGHT_CY - (isSine ? Math.sin(t) : Math.cos(t)) * CIRCLE_RADIUS;
            }
            waveLine.getPoints().addAll(sx, sy);
        }

        // Final point
        double finalX = 40 + (currentAngle / (Math.PI * 2)) * 500;
        double finalY;
        if (isTangent) {
            if (Math.abs(mathX) < 0.08) {
                finalY = -1;
            } else {
                finalY = Math.max(GRAPH_TOP, Math.min(GRAPH_BOTTOM, RIGHT_CY - mathTan * CIRCLE_RADIUS));
            }
        } else {
            finalY = RIGHT_CY - (isSine ? mathY : mathX) * CIRCLE_RADIUS;
        }
        if (!isTangent || Math.abs(mathX) >= 0.08) {
            waveLine.getPoints().addAll(finalX, finalY);
        }

        // Tracer line
        double tracerStartY = (isSine || isTangent) ? py : LEFT_CY;
        tracerLine.setStartX(-(500 - px + 30));
        tracerLine.setStartY(tracerStartY);
        tracerLine.setEndX(finalX);
        tracerLine.setEndY(finalY);
    }
}
