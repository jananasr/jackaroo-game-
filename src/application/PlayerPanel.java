package application;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays info about all players: name, color, hand size.
 */
public class PlayerPanel {

    private final List<VBox> playerInfoBoxes;

    public PlayerPanel(engine.Game game) {
        this.playerInfoBoxes = new ArrayList<>();
        int currentPlayerIndex = game.getCurrentPlayerIndex();
        buildPlayerLabels(game, currentPlayerIndex);
    }

    private void buildPlayerLabels(engine.Game game, int currentPlayerIndex) {
        List<Player> players = game.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            int nonNullCardCount = 0;
            for (model.card.Card card : player.getHand()) {
                if (card != null) {
                    nonNullCardCount++;
                }
            }
            
            // Create a container for each player
            VBox playerBox = new VBox(8);
            playerBox.setPadding(new Insets(8));
            playerBox.setMaxWidth(Double.MAX_VALUE);
            
            // Create name label with color indicator
            HBox nameRow = new HBox(10);
            nameRow.setAlignment(Pos.CENTER_LEFT);
            
            // Add color indicator
            Circle colorCircle = new Circle(10);
            colorCircle.setStroke(Color.BLACK);
            colorCircle.setStrokeWidth(1);
            
            // Set fill color based on player color
            Color circleColor = Color.BLACK;
            switch (player.getColour()) {
                case RED:
                    circleColor = Color.RED;
                    break;
                case GREEN:
                    circleColor = Color.GREEN;
                    break;
                case BLUE:
                    circleColor = Color.BLUE;
                    break;
                case YELLOW:
                    circleColor = Color.YELLOW;
                    break;
            }
            colorCircle.setFill(circleColor);
            
            // Name label - make it larger and bold
            Label nameLabel = new Label(player.getName());
            nameLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);
            
            Label colorLabel = new Label("(" + player.getColour() + ")");
            colorLabel.setMaxWidth(Double.MAX_VALUE);
            
            nameRow.getChildren().addAll(colorCircle, nameLabel);
            
            // Create a separate row for color
            HBox colorRow = new HBox(10);
            colorRow.setAlignment(Pos.CENTER_LEFT);
            colorRow.getChildren().add(colorLabel);
            
            // Create a separate row for card count and marbles
            HBox infoRow = new HBox(10);
            infoRow.setAlignment(Pos.CENTER_LEFT);
            
            // Show card count for all players
            Label cardsLabel = new Label("Cards: " + nonNullCardCount);
            cardsLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(cardsLabel, Priority.ALWAYS);
            infoRow.getChildren().add(cardsLabel);
            
            // Home marbles count - more compact format
            Label marblesLabel = new Label("Marbles: " + player.getMarbles().size());
            marblesLabel.setMaxWidth(Double.MAX_VALUE);
            infoRow.getChildren().add(marblesLabel);
            
            // Add all elements to the player box
            playerBox.getChildren().addAll(nameRow, colorRow, infoRow);
            
            // Style based on current player
            if (i == currentPlayerIndex) {
                playerBox.setStyle("-fx-background-color: #8b7355; -fx-background-radius: 5;");
                nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");
                colorLabel.setStyle("-fx-text-fill: white; -fx-font-style: italic;");
                for (javafx.scene.Node node : infoRow.getChildren()) {
                    if (node instanceof Label) {
                        ((Label)node).setStyle("-fx-text-fill: white;");
                    }
                }
            } else {
                playerBox.setStyle("-fx-background-color: #70635a; -fx-background-radius: 5;");
                nameLabel.setStyle("-fx-text-fill: #e0e0e0;");
                colorLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-style: italic;");
                for (javafx.scene.Node node : infoRow.getChildren()) {
                    if (node instanceof Label) {
                        ((Label)node).setStyle("-fx-text-fill: #e0e0e0;");
                    }
                }
            }
            
            playerInfoBoxes.add(playerBox);
        }
    }

    public List<VBox> getNodes() {
        return playerInfoBoxes;
    }
}
