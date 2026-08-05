package com.mathamorphosis.ui.visualizations;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

/**
 * Controller for chaos_game_view.fxml.
 * Embeds the fully self-contained {@link ChaosGameView} widget inside the FXML root.
 */
public class ChaosGameViewController {

    @FXML
    private StackPane rootPane;

    @FXML
    public void initialize() {
        ChaosGameView view = new ChaosGameView();
        rootPane.getChildren().setAll(view);
        StackPane.setAlignment(view, javafx.geometry.Pos.CENTER);
    }
}
