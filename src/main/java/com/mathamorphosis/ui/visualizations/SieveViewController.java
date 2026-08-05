package com.mathamorphosis.ui.visualizations;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

/**
 * Controller for sieve_view.fxml.
 * Embeds the fully self-contained {@link SieveView} widget inside the FXML root.
 */
public class SieveViewController {

    @FXML
    private StackPane rootPane;

    @FXML
    public void initialize() {
        SieveView view = new SieveView();
        rootPane.getChildren().setAll(view);
        StackPane.setAlignment(view, javafx.geometry.Pos.CENTER);
    }
}
