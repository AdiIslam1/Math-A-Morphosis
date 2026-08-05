package com.mathamorphosis.ui.visualizations;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

/**
 * Controller for graphing_calculator_view.fxml.
 * Embeds the fully self-contained {@link GraphingCalculatorView} widget inside the FXML root.
 */
public class GraphingCalculatorViewController {

    @FXML
    private StackPane rootPane;

    @FXML
    public void initialize() {
        GraphingCalculatorView view = new GraphingCalculatorView();
        rootPane.getChildren().setAll(view);
        StackPane.setAlignment(view, javafx.geometry.Pos.CENTER);
    }
}
