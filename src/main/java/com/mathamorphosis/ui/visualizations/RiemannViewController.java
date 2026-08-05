package com.mathamorphosis.ui.visualizations;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

/**
 * Controller for riemann_view.fxml.
 * Embeds the fully self-contained {@link RiemannView} widget inside the FXML root.
 */
public class RiemannViewController {

    @FXML
    private StackPane rootPane;

    @FXML
    public void initialize() {
        RiemannView view = new RiemannView();
        rootPane.getChildren().setAll(view);
        StackPane.setAlignment(view, javafx.geometry.Pos.CENTER);
    }
}
