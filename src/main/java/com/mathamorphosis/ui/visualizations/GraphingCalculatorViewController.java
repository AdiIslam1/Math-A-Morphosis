package com.mathamorphosis.ui.visualizations;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for graphing_calculator_view.fxml.
 * Owns all 2D Graphing Calculator logic, driven by FXML-injected nodes.
 */
public class GraphingCalculatorViewController {

    // ── FXML-injected nodes ──────────────────────────────────────────────────
    @FXML private VBox  functionsBox;
    @FXML private Button addFuncBtn;
    @FXML private Pane  graphPane;

    // ── Sub-panes (built in initialize) ─────────────────────────────────────
    private Pane gridPane;
    private Pane drawingPane;
    private Pane overlayPane;

    // ── Crosshair + coords overlay ───────────────────────────────────────────
    private Line crosshairX;
    private Line crosshairY;
    private Text hoverCoords;

    // ── Colour palette ───────────────────────────────────────────────────────
    private static final Color[] COLORS = {
        Color.web("#5ba8e0"), Color.web("#facc15"), Color.web("#a3e635"),
        Color.web("#f43f5e"), Color.web("#c084fc"), Color.web("#fb923c")
    };

    // ── State ────────────────────────────────────────────────────────────────
    private final List<FunctionRow> functionRows       = new ArrayList<>();
    private final List<Circle>      intersectionPoints = new ArrayList<>();
    private final List<Text>        intersectionLabels = new ArrayList<>();

    private double WIDTH   = 800;
    private double HEIGHT  = 600;
    private double offsetX = 400;
    private double offsetY = 300;
    private double scale   = 50;
    private double lastMouseX, lastMouseY;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // Build sub-panes inside graphPane
        gridPane    = new Pane();
        drawingPane = new Pane();
        overlayPane = new Pane();
        graphPane.getChildren().addAll(gridPane, drawingPane, overlayPane);

        // Clip to graph bounds
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(graphPane.widthProperty());
        clip.heightProperty().bind(graphPane.heightProperty());
        graphPane.setClip(clip);

        // Resize listeners
        graphPane.widthProperty().addListener((obs, o, n) -> {
            double oldW = WIDTH; WIDTH = n.doubleValue();
            offsetX += (WIDTH - oldW) / 2;
            drawGridAndAxes(); plotFunctions();
        });
        graphPane.heightProperty().addListener((obs, o, n) -> {
            double oldH = HEIGHT; HEIGHT = n.doubleValue();
            offsetY += (HEIGHT - oldH) / 2;
            drawGridAndAxes(); plotFunctions();
        });

        // Crosshair overlay
        crosshairX = new Line();
        crosshairX.setStroke(Color.web("#b0b0d0", 0.5));
        crosshairX.getStrokeDashArray().addAll(5d, 5d);
        crosshairX.setVisible(false);

        crosshairY = new Line();
        crosshairY.setStroke(Color.web("#b0b0d0", 0.5));
        crosshairY.getStrokeDashArray().addAll(5d, 5d);
        crosshairY.setVisible(false);

        hoverCoords = new Text();
        hoverCoords.setFill(Color.WHITE);
        hoverCoords.setFont(Font.font("Segoe UI", 12));
        hoverCoords.setVisible(false);

        overlayPane.getChildren().addAll(crosshairX, crosshairY, hoverCoords);

        setupInteractivity();

        addFuncBtn.setOnAction(e -> addFunction(""));

        addFunction("sin(x)");
        addFunction("x / 2");
    }

    // ── Function management ──────────────────────────────────────────────────

    private void addFunction(String expr) {
        Color c = COLORS[functionRows.size() % COLORS.length];
        FunctionRow row = new FunctionRow(expr, c);
        functionRows.add(row);

        HBox rowUI = new HBox(10);
        rowUI.setAlignment(Pos.CENTER_LEFT);

        Label fLabel = new Label("f" + functionRows.size() + "(x)=");
        fLabel.setStyle("-fx-text-fill: " + toHex(c) + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        Button removeBtn = new Button("X");
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-font-weight: bold;");
        removeBtn.setOnAction(e -> {
            functionRows.remove(row);
            functionsBox.getChildren().remove(rowUI);
            drawingPane.getChildren().remove(row.line);
            relabelFunctions();
            plotFunctions();
        });

        HBox.setHgrow(row.input, Priority.ALWAYS);
        rowUI.getChildren().addAll(fLabel, row.input, removeBtn);
        functionsBox.getChildren().add(rowUI);

        drawingPane.getChildren().add(row.line);
        row.line.toBack();
        plotFunctions();
    }

    private void relabelFunctions() {
        for (int i = 0; i < functionsBox.getChildren().size(); i++) {
            HBox rowUI = (HBox) functionsBox.getChildren().get(i);
            ((Label) rowUI.getChildren().get(0)).setText("f" + (i + 1) + "(x)=");
        }
    }

    // ── Interactivity ────────────────────────────────────────────────────────

    private void setupInteractivity() {
        graphPane.setOnMousePressed(e -> { lastMouseX = e.getX(); lastMouseY = e.getY(); });
        graphPane.setOnMouseDragged(e -> {
            offsetX += e.getX() - lastMouseX;
            offsetY += e.getY() - lastMouseY;
            lastMouseX = e.getX(); lastMouseY = e.getY();
            drawGridAndAxes(); plotFunctions();
        });
        graphPane.setOnScroll(e -> {
            double factor = e.getDeltaY() > 0 ? 1.1 : (1 / 1.1);
            double oldScale = scale;
            scale = Math.max(5, Math.min(1000, scale * factor));
            offsetX = e.getX() - (e.getX() - offsetX) * (scale / oldScale);
            offsetY = e.getY() - (e.getY() - offsetY) * (scale / oldScale);
            drawGridAndAxes(); plotFunctions();
        });
        graphPane.setOnMouseMoved(e -> {
            double sx = e.getX(), sy = e.getY();
            crosshairX.setVisible(true); crosshairY.setVisible(true); hoverCoords.setVisible(true);
            crosshairX.setStartX(0); crosshairX.setEndX(WIDTH); crosshairX.setStartY(sy); crosshairX.setEndY(sy);
            crosshairY.setStartX(sx); crosshairY.setEndX(sx); crosshairY.setStartY(0); crosshairY.setEndY(HEIGHT);
            hoverCoords.setText(String.format("(%.2f, %.2f)", (sx - offsetX) / scale, (offsetY - sy) / scale));
            hoverCoords.setX(sx + 10); hoverCoords.setY(sy - 15);
        });
        graphPane.setOnMouseExited(e -> {
            crosshairX.setVisible(false); crosshairY.setVisible(false); hoverCoords.setVisible(false);
        });
    }

    // ── Grid / Axes ──────────────────────────────────────────────────────────

    private void drawGridAndAxes() {
        if (WIDTH <= 0 || HEIGHT <= 0) return;
        gridPane.getChildren().clear();

        double stepMath = 1.0;
        if (scale > 150) stepMath = 0.5;
        if (scale > 300) stepMath = 0.1;
        if (scale < 30)  stepMath = 5.0;
        if (scale < 10)  stepMath = 10.0;
        if (scale < 5)   stepMath = 20.0;

        double stepScreen = stepMath * scale;
        String fmt = stepMath < 1 ? "%.1f" : "%.0f";

        double startX = offsetX % stepScreen;
        if (startX < 0) startX += stepScreen;
        for (double x = startX; x <= WIDTH; x += stepScreen) {
            Line vl = new Line(x, 0, x, HEIGHT);
            vl.setStroke(Color.web("#1c1c38")); vl.setStrokeWidth(1);
            gridPane.getChildren().add(vl);
            double mx = (x - offsetX) / scale;
            if (Math.abs(mx) > 0.001) {
                Text t = new Text(String.format(fmt, mx));
                t.setFill(Color.web("#6868a0")); t.setFont(Font.font("Segoe UI", 11));
                t.setX(x + 5);
                t.setY(Math.max(15, Math.min(HEIGHT - 5, offsetY + 15)));
                gridPane.getChildren().add(t);
            }
        }

        double startY = offsetY % stepScreen;
        if (startY < 0) startY += stepScreen;
        for (double y = startY; y <= HEIGHT; y += stepScreen) {
            Line hl = new Line(0, y, WIDTH, y);
            hl.setStroke(Color.web("#1c1c38")); hl.setStrokeWidth(1);
            gridPane.getChildren().add(hl);
            double my = (offsetY - y) / scale;
            if (Math.abs(my) > 0.001) {
                Text t = new Text(String.format(fmt, my));
                t.setFill(Color.web("#6868a0")); t.setFont(Font.font("Segoe UI", 11));
                t.setX(Math.max(5, Math.min(WIDTH - 30, offsetX - 25)));
                t.setY(y - 5);
                gridPane.getChildren().add(t);
            }
        }

        Line xAxis = new Line(0, offsetY, WIDTH, offsetY);
        xAxis.setStroke(Color.web("#b0b0d0")); xAxis.setStrokeWidth(2);
        Line yAxis = new Line(offsetX, 0, offsetX, HEIGHT);
        yAxis.setStroke(Color.web("#b0b0d0")); yAxis.setStrokeWidth(2);
        gridPane.getChildren().addAll(xAxis, yAxis);
    }

    // ── Plot ─────────────────────────────────────────────────────────────────

    private void plotFunctions() {
        if (WIDTH <= 0 || HEIGHT <= 0) return;

        for (FunctionRow row : functionRows) row.line.getElements().clear();
        drawingPane.getChildren().removeAll(intersectionPoints);
        drawingPane.getChildren().removeAll(intersectionLabels);
        intersectionPoints.clear(); intersectionLabels.clear();

        List<Expression> expressions = new ArrayList<>();
        for (FunctionRow row : functionRows) {
            String expr = row.input.getText().trim();
            Expression exp = null;
            try { if (!expr.isEmpty()) exp = new ExpressionBuilder(expr).variables("x").build(); }
            catch (Exception ignored) {}
            expressions.add(exp);
        }

        int n = expressions.size();
        boolean[] penDown   = new boolean[n];
        double[]  prevMathY = new double[n];
        Arrays.fill(prevMathY, Double.NaN);

        List<Double> prevYs = new ArrayList<>();
        for (int i = 0; i < n; i++) prevYs.add(null);

        double mathMinX = -offsetX / scale;
        double mathMaxX = (WIDTH - offsetX) / scale;
        double step     = 2.0 / scale;

        for (double mx = mathMinX; mx <= mathMaxX; mx += step) {
            double sx = offsetX + mx * scale;
            List<Double> currentYs = new ArrayList<>();

            for (int i = 0; i < n; i++) {
                Expression exp = expressions.get(i);
                Double my = null;
                if (exp != null) {
                    try {
                        exp.setVariable("x", mx);
                        double val = exp.evaluate();
                        if (!Double.isNaN(val) && !Double.isInfinite(val)) {
                            boolean bigJump = !Double.isNaN(prevMathY[i])
                                && Math.abs(val - prevMathY[i]) * scale > HEIGHT * 1.5;
                            if (bigJump) penDown[i] = false;
                            double sy = offsetY - val * scale;
                            if (!penDown[i]) { functionRows.get(i).line.getElements().add(new MoveTo(sx, sy)); penDown[i] = true; }
                            else              { functionRows.get(i).line.getElements().add(new LineTo(sx, sy)); }
                            prevMathY[i] = val; my = val;
                        } else { penDown[i] = false; prevMathY[i] = Double.NaN; }
                    } catch (Exception ignored) { penDown[i] = false; prevMathY[i] = Double.NaN; }
                } else { penDown[i] = false; prevMathY[i] = Double.NaN; }
                currentYs.add(my);
            }

            // Intersection detection for all pairs
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    Double my1 = currentYs.get(i), my2 = currentYs.get(j);
                    Double py1 = prevYs.get(i),    py2 = prevYs.get(j);
                    if (my1 != null && my2 != null && py1 != null && py2 != null) {
                        double dNow = my1 - my2, dPrev = py1 - py2;
                        if (dNow * dPrev <= 0 && Math.abs(dNow) < 50 && Math.abs(dPrev) < 50) {
                            double t = Double.isNaN(Math.abs(dPrev) / (Math.abs(dPrev) + Math.abs(dNow)))
                                ? 0.5 : Math.abs(dPrev) / (Math.abs(dPrev) + Math.abs(dNow));
                            double ix = (mx - step) + t * step;
                            double iy = 0;
                            try { expressions.get(i).setVariable("x", ix); iy = expressions.get(i).evaluate(); }
                            catch (Exception ignored) {}
                            double six = offsetX + ix * scale, siy = offsetY - iy * scale;
                            if (six >= -50 && six <= WIDTH + 50 && siy >= -50 && siy <= HEIGHT + 50) {
                                Circle dot = new Circle(six, siy, 6, Color.web("#ffffff"));
                                dot.setStroke(Color.web("#ef4444")); dot.setStrokeWidth(2);
                                dot.setEffect(new DropShadow(10, Color.web("#ef4444")));
                                intersectionPoints.add(dot);
                                Text coords = new Text(String.format("(%.2f, %.2f)", ix, iy));
                                coords.setFill(Color.web("#ffffff")); coords.setFont(Font.font("Segoe UI", 12));
                                coords.setX(six + 10); coords.setY(siy - 10);
                                coords.setEffect(new DropShadow(2, Color.BLACK));
                                intersectionLabels.add(coords);
                            }
                        }
                    }
                }
            }
            prevYs = currentYs;
        }
        drawingPane.getChildren().addAll(intersectionPoints);
        drawingPane.getChildren().addAll(intersectionLabels);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
            (int)(c.getRed() * 255), (int)(c.getGreen() * 255), (int)(c.getBlue() * 255));
    }

    // ── FunctionRow inner class ──────────────────────────────────────────────

    private class FunctionRow {
        TextField input;
        Path      line;
        Color     color;

        FunctionRow(String expr, Color c) {
            this.color = c;
            line = new Path();
            line.setStroke(c); line.setStrokeWidth(3);
            line.setFill(Color.TRANSPARENT);
            line.setEffect(new DropShadow(10, c));
            input = new TextField(expr);
            input.setStyle("-fx-background-color: #1c1c38; -fx-text-fill: #ffffff; -fx-font-size: 14px;" +
                           "-fx-border-color: " + toHex(c) + "; -fx-border-radius: 5px;");
            input.textProperty().addListener((obs, o, n) -> plotFunctions());
        }
    }
}
