package com.mathamorphosis.ui.visualizations;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

/**
 * Controller for vector_view.fxml.
 * Embeds the fully self-contained {@link VectorView} widget inside the FXML root.
 */
public class VectorViewController {

    @FXML
    private StackPane rootPane;

    @FXML
    public void initialize() {
        VectorView view = new VectorView();
        rootPane.getChildren().setAll(view);
        StackPane.setAlignment(view, javafx.geometry.Pos.CENTER);
    }
}
