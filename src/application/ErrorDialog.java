package application;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.application.Platform;

public class ErrorDialog {
    public static void show(String message) {
        // Use Platform.runLater to ensure dialog shows after current animation frame
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show(); // Use show() instead of showAndWait() to prevent blocking
        });
    }
} 