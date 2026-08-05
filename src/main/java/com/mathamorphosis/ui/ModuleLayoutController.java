package com.mathamorphosis.ui;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

public class ModuleLayoutController {

    @FXML
    private Button backBtn;

    @FXML
    private StackPane contentPane;

    private Runnable onBack;

    @FXML
    public void initialize() {
        // Initialization if needed
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    @FXML
    private void handleBack() {
        if (onBack != null) {
            onBack.run();
        }
    }

    /**
     * Replaces the content area with the given view node.
     * The node will fill the entire available space via the StackPane.
     */
    public void setContent(Node content) {
        contentPane.getChildren().setAll(content);
    }
}
