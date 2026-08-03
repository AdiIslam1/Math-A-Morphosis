package com.mathamorphosis.ui.visualizations;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
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
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class RiemannView extends BorderPane {

    // ── Canvas ───────────────────────────────────────────────────────────────
    private Canvas canvas;
    private GraphicsContext gc;

    // ── Controls ─────────────────────────────────────────────────────────────
    private Slider nSlider;
    private ComboBox<String> typeBox;
    private TextField funcField;
    private TextField aField;
    private TextField bField;
    private ToggleButton modeToggle;
    private Button startBtn;
    private Button pauseBtn;
    private Button restartBtn;

    // ── Stat labels ──────────────────────────────────────────────────────────
    private Label estValueLabel;
    private Label actValueLabel;
    private Label nValueLabel;
    private Label errorValueLabel;

    // ── State ─────────────────────────────────────────────────────────────────
    private double aValue     = 0.0;
    private double bValue     = 10.0;
    private String functionStr = "0.2 * x^2 + 3";
    private Expression expression;

    private AnimationTimer timer;
    private boolean isRunning = false;

    // ── Preset functions ─────────────────────────────────────────────────────
    private static final String[] PRESETS = {
        "0.2*x^2+3", "sin(x)+2", "cos(x/2)+2", "sqrt(x)+1", "x^3/50+2"
    };
    private static final String[] PRESET_NAMES = {
        "Parabola", "Sine Wave", "Cosine Wave", "Square Root", "Cubic"
    };

    // ── Design constants (aligned with global theme) ──────────────────────────
    private static final String BG_DEEP     = "#1a1a2e";
    private static final String BG_CARD     = "#22223a";
    private static final String BG_INPUT    = "#2c2c4a";
    private static final String ACCENT      = "#8ab4d4";   // muted sky-blue
    private static final String ACCENT_WARM = "#d4aa7d";   // warm amber
    private static final String STAT_EST    = "#d4887a";   // terracotta
    private static final String STAT_ACT    = "#7dc4a8";   // sage green
    private static final String STAT_ERR    = "#b0a0d4";   // lavender
    private static final String TEXT_LIGHT  = "#f0f0f8";
    private static final String TEXT_MUTED  = "#8888aa";
    private static final String BORDER      = "#3c3c60";
    private static final String CURVE_COLOR = "#e0e0f0";   // near-white for f(x)
    private static final String RECT_FILL   = "#8ab4d4";   // sky blue for rects

    public RiemannView() {
        this.setStyle("-fx-background-color: " + BG_DEEP + ";");

        buildHeader();
        buildCenter();
        buildRightPanel();

        initTimer();
        updateFunction(functionStr);
    }

    // ── Header ───────────────────────────────────────────────────────────────

    private void buildHeader() {
        // Title area
        Label title = new Label("Riemann Sum Explorer");
        title.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:" + TEXT_LIGHT + ";");

        Label subtitle = new Label("Visualise how infinitely many rectangles approximate the area under any curve");
        subtitle.setStyle("-fx-font-size:13px; -fx-text-fill:" + TEXT_MUTED + ";");

        VBox titleBox = new VBox(4, title, subtitle);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Preset buttons row
        HBox presets = new HBox(8);
        presets.setAlignment(Pos.CENTER_RIGHT);
        final Button[] presetBtns = new Button[PRESETS.length];
        final int[] activePreset = {-1};
        for (int i = 0; i < PRESETS.length; i++) {
            final int idx = i;
            Button pb = new Button(PRESET_NAMES[i]);
            presetBtns[idx] = pb;
            final String PBBASE = "-fx-background-color:" + BG_INPUT + "; -fx-text-fill:" + ACCENT + ";" +
                "-fx-border-color:" + ACCENT + "; -fx-border-radius:6; -fx-background-radius:6;" +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-padding:5 12; -fx-cursor:hand;" +
                "-fx-effect: dropshadow(gaussian, transparent, 0, 0, 0, 0);";
            final String PBLIT  = "-fx-background-color:" + ACCENT + "; -fx-text-fill:#0a0a14;" +
                "-fx-border-color:" + ACCENT + "; -fx-border-radius:6; -fx-background-radius:6;" +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-padding:5 12; -fx-cursor:hand;" +
                "-fx-effect: dropshadow(gaussian, " + ACCENT + ", 10, 0.55, 0, 0);";
            pb.setStyle(PBBASE);
            pb.setOnMouseEntered(e -> pb.setStyle(PBLIT));
            pb.setOnMouseExited(e -> pb.setStyle(activePreset[0] == idx ? PBLIT : PBBASE));
            pb.setOnAction(e -> {
                activePreset[0] = idx;
                for (int j = 0; j < presetBtns.length; j++) {
                    final String bBase = "-fx-background-color:" + BG_INPUT + "; -fx-text-fill:" + ACCENT + ";" +
                        "-fx-border-color:" + ACCENT + "; -fx-border-radius:6; -fx-background-radius:6;" +
                        "-fx-font-size:11px; -fx-font-weight:bold; -fx-padding:5 12; -fx-cursor:hand;" +
                        "-fx-effect: dropshadow(gaussian, transparent, 0, 0, 0, 0);";
                    final String bLit  = "-fx-background-color:" + ACCENT + "; -fx-text-fill:#0a0a14;" +
                        "-fx-border-color:" + ACCENT + "; -fx-border-radius:6; -fx-background-radius:6;" +
                        "-fx-font-size:11px; -fx-font-weight:bold; -fx-padding:5 12; -fx-cursor:hand;" +
                        "-fx-effect: dropshadow(gaussian, " + ACCENT + ", 10, 0.55, 0, 0);";
                    presetBtns[j].setStyle(j == idx ? bLit : bBase);
                }
                funcField.setText(PRESETS[idx]);
                updateFunction(PRESETS[idx]);
            });
            presets.getChildren().add(pb);
        }

        HBox header = new HBox(20, titleBox, presets);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 16, 24));
        header.setStyle("-fx-background-color:" + BG_CARD + "; -fx-border-color:" + BORDER + "; -fx-border-width:0 0 1 0;");

        this.setTop(header);
    }

    // ── Center: canvas + slider ───────────────────────────────────────────────

    private void buildCenter() {
        // Canvas fills available space
        Pane canvasPane = new Pane();
        canvasPane.setStyle("-fx-background-color:" + BG_DEEP + ";");

        canvas = new Canvas();
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());
        canvas.widthProperty().addListener(o -> draw(getMappedN()));
        canvas.heightProperty().addListener(o -> draw(getMappedN()));
        gc = canvas.getGraphicsContext2D();
        canvasPane.getChildren().add(canvas);

        // Slider row at bottom of canvas area
        VBox centerBox = new VBox(0, canvasPane, buildSliderRow());
        VBox.setVgrow(canvasPane, Priority.ALWAYS);

        this.setCenter(centerBox);
    }

    private HBox buildSliderRow() {
        Label sliderLabel = new Label("Rectangles (n)");
        sliderLabel.setStyle("-fx-text-fill:" + TEXT_MUTED + "; -fx-font-size:12px;");

        nSlider = new Slider(0, 100, 0);
        nSlider.setStyle("-fx-accent:" + ACCENT + ";");
        HBox.setHgrow(nSlider, Priority.ALWAYS);

        nValueLabel = new Label("n = 1");
        nValueLabel.setStyle("-fx-text-fill:" + ACCENT + "; -fx-font-size:13px; -fx-font-weight:bold; -fx-min-width:70;");

        nSlider.valueProperty().addListener((obs, o, nv) -> {
            int n = getMappedN();
            if (nSlider.getValue() >= 99.9) {
                nValueLabel.setText("n = ∞");
            } else {
                nValueLabel.setText("n = " + n);
            }
            draw(n);
        });

        HBox row = new HBox(14, sliderLabel, nSlider, nValueLabel);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(10, 20, 10, 20));
        row.setStyle("-fx-background-color:" + BG_CARD + "; -fx-border-color:" + BORDER + "; -fx-border-width:1 0 0 0;");
        return row;
    }

    // ── Right Panel ───────────────────────────────────────────────────────────

    private void buildRightPanel() {
        VBox panel = new VBox(16);
        panel.setPrefWidth(270);
        panel.setPadding(new Insets(20, 16, 20, 16));
        panel.setStyle(
            "-fx-background-color:" + BG_CARD + ";" +
            "-fx-border-color:" + BORDER + "; -fx-border-width:0 0 0 1;"
        );

        // ── Function Input ──────────────────────────────────────────────────
        panel.getChildren().add(sectionLabel("⨍  Function"));

        funcField = styledTextField(functionStr);
        Label funcHint = new Label("f(x) =");
        funcHint.setStyle("-fx-text-fill:" + TEXT_MUTED + "; -fx-font-size:12px;");
        HBox funcRow = new HBox(8, funcHint, funcField);
        funcRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(funcField, Priority.ALWAYS);
        funcField.textProperty().addListener((obs, o, nv) -> updateFunction(nv));
        panel.getChildren().add(funcRow);

        // ── Integral Type ───────────────────────────────────────────────────
        panel.getChildren().add(sectionLabel("∫  Integral Type"));

        typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Definite", "Indefinite");
        typeBox.setValue("Definite");
        typeBox.setMaxWidth(Double.MAX_VALUE);
        typeBox.setStyle(
            "-fx-background-color:" + BG_INPUT + "; -fx-border-color:" + BORDER + ";" +
            "-fx-border-radius:8; -fx-background-radius:8; -fx-text-fill:" + ACCENT + ";"
        );
        styleComboBox(typeBox);
        typeBox.valueProperty().addListener((obs, o, nv) -> draw(getMappedN()));
        panel.getChildren().add(typeBox);

        // ── Bounds ──────────────────────────────────────────────────────────
        panel.getChildren().add(sectionLabel("⟦ ⟧  Bounds"));

        Label aLbl = new Label("a");
        aLbl.setStyle("-fx-text-fill:" + ACCENT_WARM + "; -fx-font-weight:bold; -fx-min-width:14;");
        aField = styledTextField("0.0");
        aField.setPrefWidth(80);
        aField.textProperty().addListener((obs, o, nv) -> {
            try { aValue = Double.parseDouble(nv); draw(getMappedN()); } catch (Exception ignored) {}
        });

        Label bLbl = new Label("b");
        bLbl.setStyle("-fx-text-fill:" + ACCENT_WARM + "; -fx-font-weight:bold; -fx-min-width:14;");
        bField = styledTextField("10.0");
        bField.setPrefWidth(80);
        bField.textProperty().addListener((obs, o, nv) -> {
            try { bValue = Double.parseDouble(nv); draw(getMappedN()); } catch (Exception ignored) {}
        });

        HBox boundsRow = new HBox(10, aLbl, aField, bLbl, bField);
        boundsRow.setAlignment(Pos.CENTER_LEFT);
        panel.getChildren().add(boundsRow);

        // ── Stats Cards ─────────────────────────────────────────────────────
        panel.getChildren().add(sectionLabel("📊  Statistics"));

        estValueLabel   = new Label("0.0000");
        actValueLabel   = new Label("0.0000");
        errorValueLabel = new Label("0.0000");

        panel.getChildren().addAll(
            statCard("Estimated Area", estValueLabel,   STAT_EST),
            statCard("Actual Area",    actValueLabel,   STAT_ACT),
            statCard("Error",          errorValueLabel, STAT_ERR)
        );

        // ── Playback Controls ────────────────────────────────────────────────
        panel.getChildren().add(sectionLabel("▶  Playback"));

        modeToggle = new ToggleButton("Manual Mode");
        styleToggleButton(modeToggle, ACCENT, false);
        modeToggle.setMaxWidth(Double.MAX_VALUE);

        startBtn   = controlButton("▶  Start",   STAT_ACT);
        pauseBtn   = controlButton("⏸  Pause",   ACCENT_WARM);
        restartBtn = controlButton("⟳  Restart", STAT_ERR);

        startBtn.setDisable(true);
        pauseBtn.setDisable(true);
        restartBtn.setDisable(true);

        modeToggle.setOnAction(e -> {
            if (modeToggle.isSelected()) {
                modeToggle.setText("Auto-Play Mode");
                styleToggleButton(modeToggle, STAT_ACT, true);
                nSlider.setDisable(true);
                startBtn.setDisable(false);
                pauseBtn.setDisable(false);
                restartBtn.setDisable(false);
            } else {
                modeToggle.setText("Manual Mode");
                styleToggleButton(modeToggle, ACCENT, false);
                nSlider.setDisable(false);
                startBtn.setDisable(true);
                pauseBtn.setDisable(true);
                restartBtn.setDisable(true);
                if (timer != null && isRunning) { timer.stop(); isRunning = false; }
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
            if (timer != null && isRunning) { timer.stop(); isRunning = false; }
        });
        restartBtn.setOnAction(e -> {
            nSlider.setValue(0);
            if (timer != null && !isRunning) { timer.start(); isRunning = true; }
        });

        HBox playRow = new HBox(8, startBtn, pauseBtn, restartBtn);
        playRow.setAlignment(Pos.CENTER);

        panel.getChildren().addAll(modeToggle, playRow);

        this.setRight(panel);
    }

    // ── Style helpers ─────────────────────────────────────────────────────────

    private Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
            "-fx-text-fill:" + TEXT_MUTED + "; -fx-font-size:11px; -fx-font-weight:bold;" +
            "-fx-padding:8 0 2 0;"
        );
        return lbl;
    }

    private TextField styledTextField(String initial) {
        TextField tf = new TextField(initial);
        tf.setStyle(
            "-fx-background-color:" + BG_INPUT + "; -fx-text-fill:" + ACCENT + ";" +
            "-fx-border-color:" + BORDER + "; -fx-border-radius:8; -fx-background-radius:8;" +
            "-fx-font-size:13px; -fx-padding:6 10;"
        );
        return tf;
    }

    private VBox statCard(String title, Label valueLabel, String accentColor) {
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill:" + TEXT_MUTED + "; -fx-font-size:10px; -fx-font-weight:bold; -fx-letter-spacing:1;");

        valueLabel.setStyle(
            "-fx-text-fill:" + accentColor + "; -fx-font-size:18px; -fx-font-weight:bold;"
        );

        VBox card = new VBox(2, titleLbl, valueLabel);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setStyle(
            "-fx-background-color:" + BG_DEEP + ";" +
            "-fx-border-color:" + accentColor + ";" +
            "-fx-border-width:0 0 0 2;" +
            "-fx-border-radius:0 4 4 0; -fx-background-radius:4;"
        );
        return card;
    }

    private Button controlButton(String text, String color) {
        Button btn = new Button(text);
        final boolean[] pressed = {false};
        final String BASE = "-fx-background-color:" + BG_INPUT + "; -fx-text-fill:" + color + ";" +
            "-fx-border-color:" + color + "; -fx-border-radius:6; -fx-background-radius:6;" +
            "-fx-font-size:11px; -fx-font-weight:bold; -fx-padding:6 8; -fx-cursor:hand;" +
            "-fx-effect: dropshadow(gaussian, transparent, 0, 0, 0, 0);";
        final String HOVER = "-fx-background-color:" + color + "; -fx-text-fill:#0a0a14;" +
            "-fx-border-color:" + color + "; -fx-border-radius:6; -fx-background-radius:6;" +
            "-fx-font-size:11px; -fx-font-weight:bold; -fx-padding:6 8; -fx-cursor:hand;" +
            "-fx-effect: dropshadow(gaussian, " + color + ", 12, 0.6, 0, 0);";
        btn.setStyle(BASE);
        btn.setMinWidth(72);
        btn.setOnMouseEntered(e -> btn.setStyle(HOVER));
        btn.setOnMouseExited(e -> btn.setStyle(pressed[0] ? HOVER : BASE));
        btn.setOnMousePressed(e -> { pressed[0] = true;  btn.setStyle(HOVER); });
        btn.setOnMouseReleased(e -> { pressed[0] = false; btn.setStyle(BASE); });
        HBox.setHgrow(btn, Priority.ALWAYS);
        return btn;
    }

    private void styleToggleButton(ToggleButton btn, String color, boolean active) {
        if (active) {
            btn.setStyle(
                "-fx-background-color:" + BORDER + "; -fx-text-fill:" + TEXT_LIGHT + ";" +
                "-fx-border-color:" + ACCENT + "; -fx-border-radius:4; -fx-background-radius:4;" +
                "-fx-font-size:13px; -fx-padding:7 14; -fx-cursor:hand;"
            );
        } else {
            btn.setStyle(
                "-fx-background-color:transparent; -fx-text-fill:" + TEXT_MUTED + ";" +
                "-fx-border-color:" + BORDER + "; -fx-border-radius:4; -fx-background-radius:4;" +
                "-fx-font-size:13px; -fx-padding:7 14; -fx-cursor:hand;"
            );
        }
    }

    private void styleComboBox(ComboBox<String> box) {
        javafx.util.Callback<javafx.scene.control.ListView<String>, javafx.scene.control.ListCell<String>> cf = lv ->
            new javafx.scene.control.ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); } else {
                        setText(item);
                        setStyle("-fx-text-fill:" + ACCENT + "; -fx-background-color:" + BG_INPUT + ";");
                        setOnMouseEntered(e -> setStyle("-fx-text-fill:#fff; -fx-background-color:" + BORDER + ";"));
                        setOnMouseExited(e  -> setStyle("-fx-text-fill:" + ACCENT + "; -fx-background-color:" + BG_INPUT + ";"));
                    }
                }
            };
        box.setButtonCell(cf.call(null));
        box.setCellFactory(cf);
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private void initTimer() {
        timer = new AnimationTimer() {
            private long lastUpdate = 0;
            @Override public void handle(long now) {
                if (lastUpdate == 0) { lastUpdate = now; return; }
                double dt = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;
                if (dt > 0.1) dt = 0.016;
                double currentVal = nSlider.getValue();
                if (currentVal < 100) {
                    nSlider.setValue(currentVal + dt * 10);
                } else {
                    this.stop(); isRunning = false;
                }
            }
            @Override public void stop() { super.stop(); lastUpdate = 0; }
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int getMappedN() {
        double t = nSlider.getValue() / 100.0;
        int n = (int) (1 + 199 * Math.pow(t, 3));
        return Math.max(1, Math.min(200, n));
    }

    private String insertImplicitMultiplication(String func) {
        func = func.replaceAll("\\s+", "");
        func = func.replaceAll("(\\d)(?=(x|sin|cos|tan|log|exp|sqrt|pi|\\())", "$1*");
        func = func.replaceAll("(x|\\))(?=(x|\\d|sin|cos|tan|log|exp|sqrt|pi|e|\\())", "$1*");
        return func;
    }

    private void updateFunction(String newFunc) {
        try {
            String processed = insertImplicitMultiplication(newFunc);
            expression = new ExpressionBuilder(processed).variables("x").build();
            expression.setVariable("x", 0).evaluate();
            functionStr = newFunc;
            draw(getMappedN());
        } catch (Exception ignored) {}
    }

    private double f(double x) {
        if (expression == null) return 0;
        return expression.setVariable("x", x).evaluate();
    }

    private double calculateActualArea(double a, double b) {
        int N = 10000;
        double dx = (b - a) / N;
        double sum = 0;
        for (int i = 0; i < N; i++) sum += f(a + i * dx + dx / 2.0) * dx;
        return sum;
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    private void draw(int n) {
        if (expression == null) return;

        double W = canvas.getWidth();
        double H = canvas.getHeight();
        if (W <= 0 || H <= 0) return;

        final double PAD_L = 60, PAD_R = 30, PAD_T = 30, PAD_B = 40;
        final double plotW = W - PAD_L - PAD_R;
        final double plotH = H - PAD_T - PAD_B;

        // Background
        gc.setFill(Color.web(BG_DEEP));
        gc.fillRect(0, 0, W, H);

        // Subtle plot area tint
        gc.setFill(Color.web("#20203a", 1.0));
        gc.fillRect(PAD_L, PAD_T, plotW, plotH);

        double rangeX = bValue - aValue;
        if (rangeX <= 0) rangeX = 1;

        boolean isIndefinite = "Indefinite".equals(typeBox.getValue());

        // Find Y bounds
        double minY = 0, maxY = 0;
        for (int i = 0; i <= 200; i++) {
            double x = aValue + i * rangeX / 200.0;
            double y = f(x);
            if (y > maxY) maxY = y;
            if (y < minY) minY = y;
        }
        if (isIndefinite) {
            double integral = 0, dx2 = rangeX / 200.0;
            for (int i = 0; i <= 200; i++) {
                double x = aValue + i * dx2;
                integral += f(x) * dx2;
                if (integral > maxY) maxY = integral;
                if (integral < minY) minY = integral;
            }
        }
        if (maxY == minY) { maxY += 5; minY -= 5; }

        double padding = (maxY - minY) * 0.1;
        maxY += padding; minY -= padding;
        double rangeY = maxY - minY;

        double scaleX = plotW / rangeX;
        double scaleY = plotH / rangeY;
        double pY0    = PAD_T + maxY * scaleY; // screen-y where math-y=0

        // ── Grid lines ────────────────────────────────────────────────────────
        gc.setStroke(Color.web("#2a2a35", 0.8));
        gc.setLineWidth(0.5);
        int gridLines = 6;
        for (int i = 0; i <= gridLines; i++) {
            double gy = PAD_T + i * plotH / gridLines;
            gc.strokeLine(PAD_L, gy, PAD_L + plotW, gy);
            double gx = PAD_L + i * plotW / gridLines;
            gc.strokeLine(gx, PAD_T, gx, PAD_T + plotH);
        }

        // ── Axes ──────────────────────────────────────────────────────────────
        gc.setStroke(Color.web("#3a3a48"));
        gc.setLineWidth(1);
        // X-axis
        double clampedY0 = Math.max(PAD_T, Math.min(PAD_T + plotH, pY0));
        gc.strokeLine(PAD_L, clampedY0, PAD_L + plotW, clampedY0);
        // Y-axis
        double pX0 = PAD_L + (0 - aValue) * scaleX;
        if (pX0 >= PAD_L && pX0 <= PAD_L + plotW) {
            gc.strokeLine(pX0, PAD_T, pX0, PAD_T + plotH);
        }

        // ── Axis tick labels ──────────────────────────────────────────────────
        gc.setFill(Color.web(TEXT_MUTED));
        gc.setFont(Font.font("Monospace", 11));
        // X ticks
        for (int i = 0; i <= 5; i++) {
            double val = aValue + i * rangeX / 5.0;
            double px  = PAD_L + i * plotW / 5.0;
            gc.fillText(String.format("%.1f", val), px - 10, PAD_T + plotH + 16);
        }
        // Y ticks
        for (int i = 0; i <= 4; i++) {
            double val = maxY - i * rangeY / 4.0;
            double py  = PAD_T + i * plotH / 4.0;
            gc.fillText(String.format("%.1f", val), 2, py + 4);
        }

        // ── Axis bound labels (a and b) ───────────────────────────────────────
        gc.setFill(Color.web(ACCENT_WARM));
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        gc.fillText(String.format("a=%.1f", aValue), PAD_L,              PAD_T + plotH + 30);
        gc.fillText(String.format("b=%.1f", bValue), PAD_L + plotW - 50, PAD_T + plotH + 30);

        // ── Riemann Rectangles ────────────────────────────────────────────────
        double dx  = rangeX / n;
        double estimatedArea = 0;

        // Colour: faint steel-blue fill, slightly more opaque at low n
        double progress = (n - 1.0) / 199.0;
        double alpha = 0.18 + 0.07 * (1.0 - progress); // slightly more visible at low n
        gc.setFill(Color.web(RECT_FILL, alpha));
        gc.setStroke(Color.web(RECT_FILL, 0.35 + 0.25 * (1.0 - progress)));
        gc.setLineWidth(n < 60 ? 0.8 : 0.2);

        for (int i = 0; i < n; i++) {
            double x0   = aValue + i * dx;
            double xMid = x0 + dx / 2.0;
            double y    = f(xMid);
            estimatedArea += y * dx;

            double px  = PAD_L + (x0 - aValue) * scaleX;
            double pw  = dx * scaleX;
            double py0 = PAD_T + maxY * scaleY;
            double pyY = PAD_T + (maxY - y) * scaleY;

            double rectY = Math.min(py0, pyY);
            double rectH = Math.abs(py0 - pyY);

            gc.fillRect(px, rectY, pw, rectH);
            if (n < 80) gc.strokeRect(px, rectY, pw, rectH);
        }

        // ── Curve f(x) ────────────────────────────────────────────────────────
        gc.setStroke(Color.web(CURVE_COLOR));
        gc.setEffect(null);
        gc.setLineWidth(1.8);
        gc.beginPath();
        boolean firstPoint = true;
        for (double x = aValue; x <= bValue; x += rangeX / 400.0) {
            double px = PAD_L + (x - aValue) * scaleX;
            double py = PAD_T + (maxY - f(x)) * scaleY;
            if (py < PAD_T || py > PAD_T + plotH) { firstPoint = true; continue; }
            if (firstPoint) { gc.moveTo(px, py); firstPoint = false; }
            else gc.lineTo(px, py);
        }
        gc.stroke();
        gc.setEffect(null);

        // ── Indefinite Integral F(x) ──────────────────────────────────────────
        if (isIndefinite) {
            gc.setStroke(Color.web(ACCENT_WARM, 0.8));
            gc.setEffect(null);
            gc.setLineWidth(1.5);
            gc.setLineDashes(6, 4);
            double cumF = 0;
            double step = rangeX / 400.0;
            firstPoint = true;
            for (double x = aValue; x <= bValue; x += step) {
                double px = PAD_L + (x - aValue) * scaleX;
                double py = PAD_T + (maxY - cumF) * scaleY;
                if (py < PAD_T || py > PAD_T + plotH) { cumF += f(x) * step; firstPoint = true; continue; }
                if (firstPoint) { gc.moveTo(px, py); firstPoint = false; }
                else gc.lineTo(px, py);
                cumF += f(x) * step;
            }
            gc.stroke();
            gc.setLineDashes(0);
            gc.setEffect(null);

            // F(x) legend
            gc.setFill(Color.web(ACCENT_WARM, 0.8));
            gc.setFont(Font.font("Monospace", FontWeight.NORMAL, 11));
            gc.fillText("F(x) = ∫f(t)dt", PAD_L + plotW - 130, PAD_T + 32);
        }

        // ── f(x) legend ───────────────────────────────────────────────────────
        gc.setFill(Color.web("#40e0d0"));
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 15));
        gc.fillText("f(x) = " + functionStr, PAD_L + 10, PAD_T + 20);

        // ── Stats update ──────────────────────────────────────────────────────
        double actualArea = calculateActualArea(aValue, bValue);
        if (nSlider.getValue() >= 99.9 || n >= 200) estimatedArea = actualArea;

        double error = Math.abs(actualArea - estimatedArea);
        estValueLabel.setText(String.format("%.4f", estimatedArea));
        actValueLabel.setText(String.format("%.4f", actualArea));
        errorValueLabel.setText(String.format("%.4f", error));

        // Fade estimated area colour from terracotta → sage as n grows
        Color estColor = interpolateColor(Color.web(STAT_EST), Color.web(STAT_ACT), progress);
        estValueLabel.setStyle(
            "-fx-text-fill: " + toHex(estColor) + "; -fx-font-size:18px; -fx-font-weight:bold;"
        );
    }

    // ── Colour utilities ──────────────────────────────────────────────────────

    private Color interpolateColor(Color c1, Color c2, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
            c1.getRed()   + (c2.getRed()   - c1.getRed())   * t,
            c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t,
            c1.getBlue()  + (c2.getBlue()  - c1.getBlue())  * t,
            1.0
        );
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x",
            (int)(c.getRed() * 255), (int)(c.getGreen() * 255), (int)(c.getBlue() * 255));
    }
}
