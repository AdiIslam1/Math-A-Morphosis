package com.mathamorphosis.ui.visualizations;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class RiemannView extends VBox {

    private Canvas canvas;
    private GraphicsContext gc;
    private Slider nSlider;
    private Label estAreaLabel;
    private Label actAreaLabel;
    private ComboBox<String> typeBox;
    
    private final double WIDTH = 800;
    private final double HEIGHT = 450; // slightly smaller canvas to fit new UI
    
    private double aValue = 0.0;
    private double bValue = 10.0;
    private String functionStr = "0.2 * x^2 + 3";
    private Expression expression;
    
    private AnimationTimer timer;
    private boolean isRunning = false;

    public RiemannView() {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(30);
        this.setPadding(new Insets(20));

        // Top Configuration Bar
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER);
        
        String fieldStyle = "-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-font-weight: bold; " +
                            "-fx-border-color: #38bdf8; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 5 10;";
        String labelStyle = "-fx-text-fill: #eab308; -fx-font-weight: bold; -fx-font-size: 14px;";

        Label funcLabel = new Label("f(x) =");
        funcLabel.setStyle(labelStyle);
        TextField funcField = new TextField(functionStr);
        funcField.setStyle(fieldStyle);
        funcField.setPrefWidth(180);
        
        Label typeLabel = new Label("Type:");
        typeLabel.setStyle(labelStyle);
        typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Definite", "Indefinite");
        typeBox.setValue("Definite");
        typeBox.setStyle("-fx-background-color: #1e293b; -fx-border-color: #38bdf8; -fx-border-radius: 5; -fx-background-radius: 5; -fx-font-weight: bold;");
        
        javafx.util.Callback<javafx.scene.control.ListView<String>, javafx.scene.control.ListCell<String>> cellFactory = lv -> new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #38bdf8; -fx-background-color: transparent;");
                }
            }
        };
        typeBox.setButtonCell(cellFactory.call(null));
        typeBox.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #38bdf8; -fx-background-color: #1e293b;");
                    setOnMouseEntered(e -> setStyle("-fx-text-fill: #38bdf8; -fx-background-color: #334155;"));
                    setOnMouseExited(e -> setStyle("-fx-text-fill: #38bdf8; -fx-background-color: #1e293b;"));
                }
            }
        });
        
        Label boundsLabel = new Label("Bounds [a, b]:");
        boundsLabel.setStyle(labelStyle);
        TextField aField = new TextField(String.valueOf(aValue));
        aField.setStyle(fieldStyle);
        aField.setPrefWidth(60);
        TextField bField = new TextField(String.valueOf(bValue));
        bField.setStyle(fieldStyle);
        bField.setPrefWidth(60);
        
        topBar.getChildren().addAll(funcLabel, funcField, typeLabel, typeBox, boundsLabel, aField, bField);

        funcField.textProperty().addListener((obs, oldV, newV) -> updateFunction(newV));
        typeBox.valueProperty().addListener((obs, oldV, newV) -> draw(getMappedN()));
        aField.textProperty().addListener((obs, oldV, newV) -> {
            try { aValue = Double.parseDouble(newV); draw(getMappedN()); } catch(Exception e){}
        });
        bField.textProperty().addListener((obs, oldV, newV) -> {
            try { bValue = Double.parseDouble(newV); draw(getMappedN()); } catch(Exception e){}
        });

        // Header Stats
        HBox statsBox = new HBox(50);
        statsBox.setAlignment(Pos.CENTER);
        estAreaLabel = new Label("Estimated Area: 0.0000");
        estAreaLabel.setTextFill(Color.web("#ef4444")); // Starts Red
        estAreaLabel.setFont(Font.font("Inter", 24));
        
        actAreaLabel = new Label("Actual Area: 0.0000");
        actAreaLabel.setTextFill(Color.web("#39ff14")); // Always Green
        actAreaLabel.setFont(Font.font("Inter", 24));
        statsBox.getChildren().addAll(estAreaLabel, actAreaLabel);

        // Canvas
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();

        // Slider
        VBox sliderBox = new VBox(10);
        sliderBox.setAlignment(Pos.CENTER);
        Label sliderLabel = new Label("Number of Rectangles (n)");
        sliderLabel.setTextFill(Color.WHITE);
        nSlider = new Slider(0, 100, 0); // 0 to 100 percentage
        nSlider.setPrefWidth(600);
        nSlider.setShowTickMarks(false);
        Label nValueLabel = new Label("1");
        nValueLabel.setTextFill(Color.web("#38bdf8"));
        nValueLabel.setFont(Font.font("Inter", 16));
        HBox sliderHBox = new HBox(10);
        sliderHBox.setAlignment(Pos.CENTER);
        sliderHBox.getChildren().addAll(nSlider, nValueLabel);
        nSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int n = getMappedN();
            if (nSlider.getValue() >= 99.9) {
                nValueLabel.setText("∞");
            } else {
                nValueLabel.setText(String.valueOf(n));
            }
            draw(n);
        });
        sliderBox.getChildren().addAll(sliderLabel, sliderHBox);

        // Playback Controls
        HBox controlBox = new HBox(15);
        controlBox.setAlignment(Pos.CENTER);
        
        ToggleButton modeToggle = new ToggleButton("Mode: Manual");
        modeToggle.getStyleClass().add("back-button");
        
        Button startBtn = new Button("Start");
        startBtn.getStyleClass().add("back-button");
        Button pauseBtn = new Button("Pause");
        pauseBtn.getStyleClass().add("back-button");
        Button restartBtn = new Button("Restart");
        restartBtn.getStyleClass().add("back-button");

        // Initial State (Manual)
        startBtn.setDisable(true);
        pauseBtn.setDisable(true);
        restartBtn.setDisable(true);
        nSlider.setDisable(false);

        modeToggle.setOnAction(e -> {
            if (modeToggle.isSelected()) {
                modeToggle.setText("Mode: Auto-Play");
                nSlider.setDisable(true);
                startBtn.setDisable(false);
                pauseBtn.setDisable(false);
                restartBtn.setDisable(false);
            } else {
                modeToggle.setText("Mode: Manual");
                nSlider.setDisable(false);
                startBtn.setDisable(true);
                pauseBtn.setDisable(true);
                restartBtn.setDisable(true);
                if (timer != null && isRunning) {
                    timer.stop();
                    isRunning = false;
                }
            }
        });

        startBtn.setOnAction(e -> {
            if (timer != null && !isRunning) {
                if (nSlider.getValue() >= 100) nSlider.setValue(0);
                timer.start();
                isRunning = true;
            }
        });
        
        pauseBtn.setOnAction(e -> {
            if (timer != null && isRunning) {
                timer.stop();
                isRunning = false;
            }
        });

        restartBtn.setOnAction(e -> {
            nSlider.setValue(0);
            if (timer != null && !isRunning) {
                timer.start();
                isRunning = true;
            }
        });

        controlBox.getChildren().addAll(modeToggle, startBtn, pauseBtn, restartBtn);

        this.getChildren().addAll(topBar, statsBox, canvas, sliderBox, controlBox);

        initTimer();
        updateFunction(functionStr);
    }

    private void initTimer() {
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
                if (dt > 0.1) dt = 0.016;

                double currentVal = nSlider.getValue();
                if (currentVal < 100) {
                    nSlider.setValue(currentVal + dt * 10); // Takes 10s to reach 100
                } else {
                    this.stop();
                    isRunning = false;
                }
            }
            
            @Override
            public void stop() {
                super.stop();
                lastUpdate = 0;
            }
        };
    }

    private int getMappedN() {
        double t = nSlider.getValue() / 100.0;
        // Non-linear cubic mapping: keeps 'n' low for longer, 
        // making the visual convergence much more satisfying and pronounced at the end.
        int n = (int) (1 + 199 * Math.pow(t, 3)); 
        return Math.max(1, Math.min(200, n));
    }

    private String insertImplicitMultiplication(String func) {
        func = func.replaceAll("\\s+", "");
        // 1. Number followed by x, function, or '('
        func = func.replaceAll("(\\d)(?=(x|sin|cos|tan|log|exp|sqrt|pi|\\())", "$1*");
        // 2. 'x' or ')' followed by x, digit, function, pi, e, or '('
        func = func.replaceAll("(x|\\))(?=(x|\\d|sin|cos|tan|log|exp|sqrt|pi|e|\\())", "$1*");
        return func;
    }

    private void updateFunction(String newFunc) {
        try {
            String processedFunc = insertImplicitMultiplication(newFunc);
            expression = new ExpressionBuilder(processedFunc).variables("x").build();
            expression.setVariable("x", 0).evaluate(); // Test evaluation
            functionStr = newFunc;
            draw(getMappedN());
        } catch (Exception e) {
            // Invalid function, ignore drawing until fixed
        }
    }

    private double f(double x) {
        if (expression == null) return 0;
        return expression.setVariable("x", x).evaluate();
    }

    private double calculateActualArea(double a, double b) {
        int N = 10000; // high precision sum
        double dx = (b - a) / N;
        double sum = 0;
        for (int i=0; i<N; i++) {
            sum += f(a + i*dx + dx/2.0) * dx;
        }
        return sum;
    }

    private void draw(int n) {
        if (expression == null) return;
        
        gc.setFill(Color.web("#050505"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        double rangeX = bValue - aValue;
        if (rangeX <= 0) rangeX = 1;
        
        // Find Y Bounds
        double minY = 0, maxY = 0;
        boolean isIndefinite = "Indefinite".equals(typeBox.getValue());
        
        for (int i=0; i<=100; i++) {
            double x = aValue + i * rangeX / 100.0;
            double y = f(x);
            if (y > maxY) maxY = y;
            if (y < minY) minY = y;
        }
        
        if (isIndefinite) {
            double integral = 0;
            double dx = rangeX / 100.0;
            for (int i=0; i<=100; i++) {
                double x = aValue + i * dx;
                integral += f(x) * dx;
                if (integral > maxY) maxY = integral;
                if (integral < minY) minY = integral;
            }
        }
        
        if (maxY == minY) { maxY += 5; minY -= 5; }
        
        double rangeY = maxY - minY;
        double scaleX = (WIDTH - 100) / rangeX;
        double scaleY = (HEIGHT - 100) / rangeY;
        
        double pY0_axis = 50 + maxY * scaleY; // pixel Y coordinate where math y=0

        // Draw Axes
        gc.setStroke(Color.web("#334155"));
        gc.setLineWidth(2);
        
        // X-axis
        if (pY0_axis >= 50 && pY0_axis <= HEIGHT - 50) {
            gc.strokeLine(50, pY0_axis, WIDTH - 50, pY0_axis); 
        }
        // Y-axis
        double pX0_axis = 50 + (0 - aValue) * scaleX;
        if (pX0_axis >= 50 && pX0_axis <= WIDTH - 50) {
            gc.strokeLine(pX0_axis, 50, pX0_axis, HEIGHT - 50);
        }

        // Draw Bounds Labels (a and b) below the x-axis
        gc.setFill(Color.web("#eab308")); // Yellow text for bounds
        gc.setFont(Font.font("Inter", 13));
        double textY = Math.min(Math.max(pY0_axis + 18, 68), HEIGHT - 15);
        String aText = String.format((aValue == (long) aValue) ? "%d" : "%.2f", (long) aValue);
        String bText = String.format((bValue == (long) bValue) ? "%d" : "%.2f", (long) bValue);
        if (aValue != (long) aValue) aText = String.format("%.2f", aValue);
        if (bValue != (long) bValue) bText = String.format("%.2f", bValue);
        gc.fillText(aText, 40, textY);
        gc.fillText(bText, WIDTH - 60, textY);

        double dx = rangeX / n;
        double estimatedArea = 0;

        // Draw Rectangles
        gc.setFill(Color.web("#ef4444", 0.5)); // Semi-transparent Red
        gc.setStroke(Color.web("#ef4444"));
        gc.setLineWidth(1);

        for (int i = 0; i < n; i++) {
            double rectStartX = aValue + i * dx;
            double xMid = rectStartX + dx / 2.0; // Midpoint Riemann sum for better accuracy
            double y = f(xMid); 
            estimatedArea += y * dx;

            double px = 50 + (rectStartX - aValue) * scaleX;
            double pw = dx * scaleX;
            
            double py_0 = 50 + maxY * scaleY;
            double py_y = 50 + (maxY - y) * scaleY;
            
            double rectY = Math.min(py_0, py_y);
            double rectH = Math.abs(py_0 - py_y);

            gc.fillRect(px, rectY, pw, rectH);
            if (n < 50) {
                gc.strokeRect(px, rectY, pw, rectH);
            }
        }

        // Draw Curve f(x)
        gc.setStroke(Color.web("#39ff14")); // Neon Green
        gc.setLineWidth(3);
        gc.beginPath();
        for (double x = aValue; x <= bValue; x += rangeX / 200.0) {
            double px = 50 + (x - aValue) * scaleX;
            double py = 50 + (maxY - f(x)) * scaleY;
            if (x == aValue) gc.moveTo(px, py);
            else gc.lineTo(px, py);
        }
        gc.stroke();
        
        // Legend
        gc.setFill(Color.web("#39ff14"));
        gc.setFont(Font.font("Inter", 18));
        gc.fillText("f(x)", WIDTH - 150, 50);

        // Draw Indefinite Integral F(x)
        if (isIndefinite) {
            gc.setStroke(Color.web("#eab308")); // Neon Yellow
            gc.setLineWidth(3);
            gc.beginPath();
            double currentF = 0;
            double step = rangeX / 200.0;
            for (double x = aValue; x <= bValue; x += step) {
                double px = 50 + (x - aValue) * scaleX;
                double py = 50 + (maxY - currentF) * scaleY;
                if (x == aValue) gc.moveTo(px, py);
                else gc.lineTo(px, py);
                currentF += f(x) * step;
            }
            gc.stroke();
            
            gc.setFill(Color.web("#eab308"));
            gc.fillText("F(x) = ∫ f(t) dt", WIDTH - 150, 80);
        }

        double actualArea = calculateActualArea(aValue, bValue);
        // Enforce exact match at max slider value to fulfill user visual requirement
        if (nSlider.getValue() >= 99.9 || n >= 200) {
            estimatedArea = actualArea;
        }

        estAreaLabel.setText(String.format("Estimated Area: %.4f", estimatedArea));
        actAreaLabel.setText(String.format("Actual Area: %.4f", actualArea));
        
        // Dynamically color the estimated area based on how close the slider is to max
        double progress = (n - 1.0) / 199.0; // 0.0 at n=1, 1.0 at n=200
        Color estColor = interpolateColor(Color.web("#ef4444"), Color.web("#39ff14"), progress);
        estAreaLabel.setTextFill(estColor);
    }
    
    private Color interpolateColor(Color c1, Color c2, double fraction) {
        double r = c1.getRed() + (c2.getRed() - c1.getRed()) * fraction;
        double g = c1.getGreen() + (c2.getGreen() - c1.getGreen()) * fraction;
        double b = c1.getBlue() + (c2.getBlue() - c1.getBlue()) * fraction;
        return new Color(r, g, b, 1.0);
    }
}
