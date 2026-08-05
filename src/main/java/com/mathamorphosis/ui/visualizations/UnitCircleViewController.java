package com.mathamorphosis.ui.visualizations;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

/**
 * Controller for unit_circle_view.fxml.
 * Embeds the fully self-contained {@link UnitCircleView} widget inside the FXML root.
 */
public class UnitCircleViewController {

    @FXML
    private StackPane rootPane;

    @FXML
    public void initialize() {
        UnitCircleView view = new UnitCircleView();
        rootPane.getChildren().setAll(view);
        StackPane.setAlignment(view, javafx.geometry.Pos.CENTER);
    }
}
