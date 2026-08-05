package com.mathamorphosis.ui.visualizations;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

/**
 * Controller for least_squares_view.fxml.
 * Embeds the fully self-contained {@link LeastSquaresView} widget inside the FXML root.
 */
public class LeastSquaresViewController {

    @FXML
    private StackPane rootPane;

    @FXML
    public void initialize() {
        LeastSquaresView view = new LeastSquaresView();
        rootPane.getChildren().setAll(view);
        StackPane.setAlignment(view, javafx.geometry.Pos.CENTER);
    }
}
