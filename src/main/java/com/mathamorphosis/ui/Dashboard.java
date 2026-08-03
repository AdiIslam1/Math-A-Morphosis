package com.mathamorphosis.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class Dashboard extends VBox {

    public Dashboard(Consumer<String> onModuleSelected) {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(40);
        this.setPadding(new Insets(50));
        this.getStyleClass().add("root");

        // Header
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        Label title = new Label("Math-A-Morphosis");
        title.getStyleClass().add("header-text");
        
        Label subtitle = new Label("A Visual Mathematics Learning Studio");
        subtitle.getStyleClass().add("subheader-text");
        header.getChildren().addAll(title, subtitle);

        // Modules Grid
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(30);
        grid.setVgap(30);

        // Module Cards
        grid.add(createModuleCard("Number Theory", "Sieve of Eratosthenes", () -> onModuleSelected.accept("NUMBER_THEORY")), 0, 0);
        grid.add(createModuleCard("Calculus", "Riemann Sum Convergence", () -> onModuleSelected.accept("CALCULUS")), 1, 0);
        grid.add(createModuleCard("Linear Algebra", "Interactive Vector Projections", () -> onModuleSelected.accept("LINEAR_ALGEBRA")), 0, 1);
        grid.add(createModuleCard("Statistics", "Least Squares Regression Sandbox", () -> onModuleSelected.accept("LEAST_SQUARES")), 1, 1);
        grid.add(createModuleCard("Trigonometry", "Unit Circle Unroller", () -> onModuleSelected.accept("UNIT_CIRCLE")), 0, 2);
        grid.add(createModuleCard("Algebra", "2D Graphing Calculator", () -> onModuleSelected.accept("GRAPHING_CALC")), 1, 2);
        grid.add(createModuleCard("Signal Processing", "Fourier Series Epicycles", () -> onModuleSelected.accept("FOURIER_SERIES")), 0, 3);
        grid.add(createModuleCard("Mathematical Marvels", "The Chaos Game: Order from Randomness", () -> onModuleSelected.accept("CHAOS_GAME")), 1, 3);
        
        this.getChildren().addAll(header, grid);
    }

    private VBox createModuleCard(String titleText, String descText, Runnable onClick) {
        VBox card = new VBox(15);
        card.getStyleClass().add("module-card");
        card.setPrefSize(350, 180);
        card.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(titleText);
        title.getStyleClass().add("module-title");

        Label desc = new Label(descText);
        desc.getStyleClass().add("module-desc");

        card.getChildren().addAll(title, desc);
        card.setOnMouseClicked(e -> onClick.run());

        return card;
    }
}
