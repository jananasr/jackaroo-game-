package application;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import model.Colour;

/**
 * Displays a dialog when a player wins the game.
 */
public class WinnerDialog {

    private final Alert alert;

    public WinnerDialog(Colour winnerColour) {
        alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText("We have a winner!");
        alert.setContentText("Player with colour " + winnerColour + " wins the game!");
    }

    public void show() {
        alert.showAndWait();
    }
}
