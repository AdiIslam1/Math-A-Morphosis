package com.mathamorphosis.ui.visualizations;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

/**
 * Controller for fourier_series_view.fxml.
 * Embeds the fully self-contained {@link FourierSeriesView} widget inside the FXML root.
 */
public class FourierSeriesViewController {

    @FXML
    private StackPane rootPane;

    @FXML
    public void initialize() {
        FourierSeriesView view = new FourierSeriesView();
        rootPane.getChildren().setAll(view);
        StackPane.setAlignment(view, javafx.geometry.Pos.CENTER);
    }
}
