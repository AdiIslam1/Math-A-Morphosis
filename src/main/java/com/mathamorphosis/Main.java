package com.mathamorphosis;

import com.mathamorphosis.ui.Dashboard;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.geometry.Insets;

public class Main extends Application {

    private Stage primaryStage;
    private Scene mainScene;
    private StackPane rootNode;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Math-A-Morphosis");
        this.primaryStage.setMaximized(true);

        rootNode = new StackPane();
        mainScene = new Scene(rootNode, 1280, 720);
        
        // Load CSS
        String css = getClass().getResource("/styles/theme.css").toExternalForm();
        mainScene.getStylesheets().add(css);

        showStartupScreen();

        this.primaryStage.setScene(mainScene);
        this.primaryStage.show();
    }

    private void showStartupScreen() {
        com.mathamorphosis.ui.StartupScreen startup = new com.mathamorphosis.ui.StartupScreen(this::showDashboard, 1280, 720);
        rootNode.getChildren().setAll(startup);
    }

    private void showDashboard() {
        Dashboard dashboard = new Dashboard(this::loadModule);
        rootNode.getChildren().setAll(dashboard);
    }

    private void loadModule(String moduleId) {
        BorderPane moduleLayout = new BorderPane();
        moduleLayout.getStyleClass().add("root");
        moduleLayout.setPadding(new Insets(20));

        // Top nav with back button
        Button backBtn = new Button("< Back to Dashboard");
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> showDashboard());
        moduleLayout.setTop(backBtn);
        BorderPane.setMargin(backBtn, new Insets(0, 0, 20, 0));

        // Module content placeholder
        VBox content = new VBox();
        content.setAlignment(Pos.CENTER);
        Rectangle contentClip = new Rectangle();
        contentClip.widthProperty().bind(content.widthProperty());
        contentClip.heightProperty().bind(content.heightProperty());
        content.setClip(contentClip);
        Label placeholder = new Label("Loading " + moduleId + "...");
        placeholder.getStyleClass().add("header-text");
        content.getChildren().add(placeholder);
        
        moduleLayout.setCenter(content);

        if (moduleId.equals("NUMBER_THEORY")) {
            content.getChildren().setAll(new com.mathamorphosis.ui.visualizations.SieveView());
        } else if (moduleId.equals("CALCULUS")) {
            content.getChildren().setAll(new com.mathamorphosis.ui.visualizations.RiemannView());
        } else if (moduleId.equals("LINEAR_ALGEBRA")) {
            content.getChildren().setAll(new com.mathamorphosis.ui.visualizations.VectorView());
        } else if (moduleId.equals("LEAST_SQUARES")) {
            content.getChildren().setAll(new com.mathamorphosis.ui.visualizations.LeastSquaresView());
        } else if (moduleId.equals("UNIT_CIRCLE")) {
            content.getChildren().setAll(new com.mathamorphosis.ui.visualizations.UnitCircleView());
        } else if (moduleId.equals("GRAPHING_CALC")) {
            content.getChildren().setAll(new com.mathamorphosis.ui.visualizations.GraphingCalculatorView());
        } else if (moduleId.equals("FOURIER_SERIES")) {
            com.mathamorphosis.ui.visualizations.FourierSeriesView fourierView =
                    new com.mathamorphosis.ui.visualizations.FourierSeriesView();
            VBox.setVgrow(fourierView, Priority.ALWAYS);
            fourierView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            content.getChildren().setAll(fourierView);
        } else if (moduleId.equals("CHAOS_GAME")) {
            com.mathamorphosis.ui.visualizations.ChaosGameView chaosView =
                    new com.mathamorphosis.ui.visualizations.ChaosGameView();
            VBox.setVgrow(chaosView, Priority.ALWAYS);
            chaosView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            content.getChildren().setAll(chaosView);
        }

        rootNode.getChildren().setAll(moduleLayout);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
