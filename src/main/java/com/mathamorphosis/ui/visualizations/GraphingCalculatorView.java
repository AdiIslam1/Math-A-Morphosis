package com.mathamorphosis.ui.visualizations;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GraphingCalculatorView extends BorderPane {

    private final Pane graphPane;
    private final Pane gridPane; 
    private final Pane drawingPane; 
    private final Pane overlayPane; 
    
    private final VBox functionsBox;
    
    private final List<Circle> intersectionPoints = new ArrayList<>();
    private final List<Text> intersectionLabels = new ArrayList<>();
    
    private double WIDTH = 800;
    private double HEIGHT = 600;
    
    private double offsetX = 400;
    private double offsetY = 300;
    private double scale = 50; 
    
    private double lastMouseX;
    private double lastMouseY;
    
    private final Line crosshairX;
    private final Line crosshairY;
    private final Text hoverCoords;

    private final Color[] colors = new Color[] {
        Color.web("#5ba8e0"), // light blue
        Color.web("#facc15"), // yellow
        Color.web("#a3e635"), // lime
        Color.web("#f43f5e"), // rose
        Color.web("#c084fc"), // purple
        Color.web("#fb923c")  // orange
    };
    
    private class FunctionRow {
        TextField input;
        Path line;
        Color color;
        
        FunctionRow(String expr, Color c) {
            this.color = c;
            
            line = new Path();
            line.setStroke(color);
            line.setStrokeWidth(3);
            line.setFill(Color.TRANSPARENT);
            line.setEffect(new DropShadow(10, color));
            
            input = new TextField(expr);
            input.setStyle("-fx-background-color: #1c1c38; -fx-text-fill: #ffffff; -fx-font-size: 14px; -fx-border-color: " + toHex(color) + "; -fx-border-radius: 5px;");
            input.textProperty().addListener((obs, oldV, newV) -> plotFunctions());
            
            HBox.setHgrow(input, Priority.ALWAYS);
        }
    }
    
    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }
    
    private final List<FunctionRow> functionRows = new ArrayList<>();

    public GraphingCalculatorView() {
        this.getStyleClass().add("root");
        
        // Side Panel for functions
        VBox sidePanel = new VBox(15);
        sidePanel.setPrefWidth(300);
        sidePanel.setStyle("-fx-background-color: #100f28; -fx-border-color: #32325a; -fx-border-width: 0 2px 0 0;");
        sidePanel.setPadding(new Insets(20));
        
        Label title = new Label("Functions");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        
        functionsBox = new VBox(10);
        
        ScrollPane scrollPane = new ScrollPane(functionsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #100f28; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        Button addFuncBtn = new Button("+ Add Function");
        addFuncBtn.setStyle("-fx-font-size: 14px; -fx-text-fill: #5ba8e0; -fx-font-weight: bold; -fx-background-color: #1c1c38; -fx-border-color: #5ba8e0; -fx-border-radius: 5px; -fx-cursor: hand;");
        addFuncBtn.setMaxWidth(Double.MAX_VALUE);
        addFuncBtn.setOnAction(e -> addFunction(""));
        
        sidePanel.getChildren().addAll(title, scrollPane, addFuncBtn);
        
        // Graph Pane (Full window rest area)
        graphPane = new Pane();
        graphPane.setStyle("-fx-background-color: #14142a;"); // Very dark background
        
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(graphPane.widthProperty());
        clip.heightProperty().bind(graphPane.heightProperty());
        graphPane.setClip(clip);
        
        graphPane.widthProperty().addListener((obs, oldV, newV) -> {
            double oldW = WIDTH;
            WIDTH = newV.doubleValue();
            offsetX += (WIDTH - oldW) / 2;
            drawGridAndAxes();
            plotFunctions();
        });
        
        graphPane.heightProperty().addListener((obs, oldV, newV) -> {
            double oldH = HEIGHT;
            HEIGHT = newV.doubleValue();
            offsetY += (HEIGHT - oldH) / 2;
            drawGridAndAxes();
            plotFunctions();
        });
        
        gridPane = new Pane();
        drawingPane = new Pane();
        overlayPane = new Pane();
        graphPane.getChildren().addAll(gridPane, drawingPane, overlayPane);
        
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
        
        this.setLeft(sidePanel);
        this.setCenter(graphPane);
        
        // Initial setup
        addFunction("sin(x)");
        addFunction("x / 2");
    }
    
    private void addFunction(String expr) {
        Color c = colors[functionRows.size() % colors.length];
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
        
        rowUI.getChildren().addAll(fLabel, row.input, removeBtn);
        functionsBox.getChildren().add(rowUI);
        
        drawingPane.getChildren().add(row.line);
        row.line.toBack();
        
        plotFunctions();
    }
    
    private void relabelFunctions() {
        for (int i = 0; i < functionsBox.getChildren().size(); i++) {
            HBox rowUI = (HBox) functionsBox.getChildren().get(i);
            Label fLabel = (Label) rowUI.getChildren().get(0);
            fLabel.setText("f" + (i + 1) + "(x)=");
        }
    }
    
    private void setupInteractivity() {
        graphPane.setOnMousePressed(e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
        });
        
        graphPane.setOnMouseDragged(e -> {
            double dx = e.getX() - lastMouseX;
            double dy = e.getY() - lastMouseY;
            offsetX += dx;
            offsetY += dy;
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            
            drawGridAndAxes();
            plotFunctions();
        });
        
        graphPane.setOnScroll(e -> {
            double zoomFactor = 1.1;
            double oldScale = scale;
            if (e.getDeltaY() > 0) {
                scale *= zoomFactor;
            } else if (e.getDeltaY() < 0) {
                scale /= zoomFactor;
            }
            
            if (scale < 5) scale = 5;
            if (scale > 1000) scale = 1000;
            
            double mouseX = e.getX();
            double mouseY = e.getY();
            
            offsetX = mouseX - (mouseX - offsetX) * (scale / oldScale);
            offsetY = mouseY - (mouseY - offsetY) * (scale / oldScale);
            
            drawGridAndAxes();
            plotFunctions();
        });
        
        graphPane.setOnMouseMoved(e -> {
            crosshairX.setVisible(true);
            crosshairY.setVisible(true);
            hoverCoords.setVisible(true);
            
            double sx = e.getX();
            double sy = e.getY();
            
            crosshairX.setStartX(0); crosshairX.setEndX(WIDTH);
            crosshairX.setStartY(sy); crosshairX.setEndY(sy);
            
            crosshairY.setStartX(sx); crosshairY.setEndX(sx);
            crosshairY.setStartY(0); crosshairY.setEndY(HEIGHT);
            
            double mx = (sx - offsetX) / scale;
            double my = (offsetY - sy) / scale;
            
            hoverCoords.setText(String.format("(%.2f, %.2f)", mx, my));
            hoverCoords.setX(sx + 10);
            hoverCoords.setY(sy - 15);
        });
        
        graphPane.setOnMouseExited(e -> {
            crosshairX.setVisible(false);
            crosshairY.setVisible(false);
            hoverCoords.setVisible(false);
        });
    }
    
    private void drawGridAndAxes() {
        if (WIDTH <= 0 || HEIGHT <= 0) return;
        gridPane.getChildren().clear();
        
        double stepMath = 1.0;
        if (scale > 150) stepMath = 0.5;
        if (scale > 300) stepMath = 0.1;
        if (scale < 30) stepMath = 5.0;
        if (scale < 10) stepMath = 10.0;
        if (scale < 5) stepMath = 20.0;
        
        double stepScreen = stepMath * scale;
        
        double startX = offsetX % stepScreen;
        if (startX < 0) startX += stepScreen;
        
        for (double x = startX; x <= WIDTH; x += stepScreen) {
            Line vLine = new Line(x, 0, x, HEIGHT);
            vLine.setStroke(Color.web("#1c1c38"));
            vLine.setStrokeWidth(1);
            gridPane.getChildren().add(vLine);
            
            double mx = (x - offsetX) / scale;
            if (Math.abs(mx) > 0.001) {
                Text t = new Text(String.format(stepMath < 1 ? "%.1f" : "%.0f", mx));
                t.setFill(Color.web("#6868a0"));
                t.setFont(Font.font("Segoe UI", 11));
                t.setX(x + 5);
                t.setY(offsetY + 15);
                if (offsetY + 15 < 15) t.setY(15);
                if (offsetY + 15 > HEIGHT - 5) t.setY(HEIGHT - 5);
                gridPane.getChildren().add(t);
            }
        }
        
        double startY = offsetY % stepScreen;
        if (startY < 0) startY += stepScreen;
        
        for (double y = startY; y <= HEIGHT; y += stepScreen) {
            Line hLine = new Line(0, y, WIDTH, y);
            hLine.setStroke(Color.web("#1c1c38"));
            hLine.setStrokeWidth(1);
            gridPane.getChildren().add(hLine);
            
            double my = (offsetY - y) / scale;
            if (Math.abs(my) > 0.001) {
                Text t = new Text(String.format(stepMath < 1 ? "%.1f" : "%.0f", my));
                t.setFill(Color.web("#6868a0"));
                t.setFont(Font.font("Segoe UI", 11));
                t.setX(offsetX - 25);
                t.setY(y - 5);
                if (offsetX - 25 < 5) t.setX(5);
                if (offsetX - 25 > WIDTH - 30) t.setX(WIDTH - 30);
                gridPane.getChildren().add(t);
            }
        }
        
        Line xAxis = new Line(0, offsetY, WIDTH, offsetY);
        xAxis.setStroke(Color.web("#b0b0d0"));
        xAxis.setStrokeWidth(2);
        gridPane.getChildren().add(xAxis);
        
        Line yAxis = new Line(offsetX, 0, offsetX, HEIGHT);
        yAxis.setStroke(Color.web("#b0b0d0"));
        yAxis.setStrokeWidth(2);
        gridPane.getChildren().add(yAxis);
    }
    
    private void plotFunctions() {
        if (WIDTH <= 0 || HEIGHT <= 0) return;

        for (FunctionRow row : functionRows) {
            row.line.getElements().clear();
        }

        drawingPane.getChildren().removeAll(intersectionPoints);
        drawingPane.getChildren().removeAll(intersectionLabels);
        intersectionPoints.clear();
        intersectionLabels.clear();

        List<Expression> expressions = new ArrayList<>();
        for (FunctionRow row : functionRows) {
            String expr = row.input.getText().trim();
            Expression exp = null;
            try {
                if (!expr.isEmpty()) {
                    exp = new ExpressionBuilder(expr).variables("x").build();
                }
            } catch (Exception ignored) {}
            expressions.add(exp);
        }

        double mathMinX = -offsetX / scale;
        double mathMaxX = (WIDTH - offsetX) / scale;
        double step = 2.0 / scale;

        int n = expressions.size();
        // penDown[i]: whether the last point for function i was valid (pen is on paper)
        boolean[] penDown = new boolean[n];
        // prevMathY[i]: last valid math-y for function i (NaN if no valid point yet)
        double[] prevMathY = new double[n];
        Arrays.fill(prevMathY, Double.NaN);

        // For intersection detection we still track per-step y values
        List<Double> prevYs = new ArrayList<>();
        for (int i = 0; i < n; i++) prevYs.add(null);

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
                            // Asymptote / discontinuity detection:
                            // If the jump from the previous point is larger than ~1.5x the
                            // viewport height in screen pixels, lift the pen rather than
                            // drawing a near-vertical spike across the asymptote.
                            boolean bigJump = !Double.isNaN(prevMathY[i])
                                    && Math.abs(val - prevMathY[i]) * scale > HEIGHT * 1.5;

                            if (bigJump) {
                                penDown[i] = false; // lift pen at the jump
                            }

                            double sy = offsetY - val * scale;
                            if (!penDown[i]) {
                                // Start a new sub-path segment
                                functionRows.get(i).line.getElements().add(new MoveTo(sx, sy));
                                penDown[i] = true;
                            } else {
                                functionRows.get(i).line.getElements().add(new LineTo(sx, sy));
                            }
                            prevMathY[i] = val;
                            my = val;
                        } else {
                            // NaN or Infinity — lift pen
                            penDown[i] = false;
                            prevMathY[i] = Double.NaN;
                        }
                    } catch (Exception ignored) {
                        penDown[i] = false;
                        prevMathY[i] = Double.NaN;
                    }
                } else {
                    penDown[i] = false;
                    prevMathY[i] = Double.NaN;
                }
                currentYs.add(my);
            }

            // Check intersections for all pairs
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    Double my1 = currentYs.get(i);
                    Double my2 = currentYs.get(j);
                    Double py1 = prevYs.get(i);
                    Double py2 = prevYs.get(j);

                    if (my1 != null && my2 != null && py1 != null && py2 != null) {
                        double diffNow = my1 - my2;
                        double diffPrev = py1 - py2;

                        if (diffNow * diffPrev <= 0 && Math.abs(diffNow) < 50 && Math.abs(diffPrev) < 50) {
                            double t = Math.abs(diffPrev) / (Math.abs(diffPrev) + Math.abs(diffNow));
                            if (Double.isNaN(t)) t = 0.5;
                            double intersectX = (mx - step) + t * step;

                            Expression e1 = expressions.get(i);
                            e1.setVariable("x", intersectX);
                            double intersectY = 0;
                            try {
                                intersectY = e1.evaluate();
                            } catch (Exception ignored) {}

                            double screenIntersectX = offsetX + intersectX * scale;
                            double screenIntersectY = offsetY - intersectY * scale;

                            if (screenIntersectX >= -50 && screenIntersectX <= WIDTH + 50
                                    && screenIntersectY >= -50 && screenIntersectY <= HEIGHT + 50) {
                                Circle dot = new Circle(screenIntersectX, screenIntersectY, 6, Color.web("#ffffff"));
                                dot.setStroke(Color.web("#ef4444"));
                                dot.setStrokeWidth(2);
                                dot.setEffect(new DropShadow(10, Color.web("#ef4444")));
                                intersectionPoints.add(dot);

                                Text coords = new Text(String.format("(%.2f, %.2f)", intersectX, intersectY));
                                coords.setFill(Color.web("#ffffff"));
                                coords.setFont(Font.font("Segoe UI", 12));
                                coords.setX(screenIntersectX + 10);
                                coords.setY(screenIntersectY - 10);
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
}
