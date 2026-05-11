package application;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import model.card.Card;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays player's hand as clickable cards.
 */
public class CardView {

    private final Player player;
    private final List<Button> cardButtons;
    private Button selectedButton = null;

    // Add a callback for card selection
    private Runnable cardSelectionCallback;
    
    public void setCardSelectionCallback(Runnable callback) {
        this.cardSelectionCallback = callback;
    }

    public CardView(engine.Game game) {
        this.player = game.getPlayers().get(game.getCurrentPlayerIndex());
        this.cardButtons = new ArrayList<>();
        buildCardButtons(game);
    }

    private void buildCardButtons(engine.Game game) {
        for (Card card : player.getHand()) {
            // Skip null cards
            if (card == null) {
                continue;
            }
            
            String label = card.getName();
            try {
                // Try to get suit if available
                java.lang.reflect.Method getSuit = card.getClass().getMethod("getSuit");
                Object suitObj = getSuit.invoke(card);
                if (suitObj != null) {
                    String suitSymbol = suitToSymbol(suitObj.toString());
                    label += " " + suitSymbol;
                }
            } catch (Exception ignored) {}
            Button cardButton = new Button(label);
            cardButton.setPrefWidth(100);
            if (player.getSelectedCard() == card) {
                cardButton.setStyle("-fx-border-color: #0078D7; -fx-border-width: 3px;");
                selectedButton = cardButton;
            }
            cardButton.setOnAction(e -> {
                try {
                    game.selectCard(card);
                    if (selectedButton != null) selectedButton.setStyle("");
                    cardButton.setStyle("-fx-border-color: #0078D7; -fx-border-width: 3px;");
                    selectedButton = cardButton;
                    
                    // Notify the GameScreen that a card has been selected
                    if (cardSelectionCallback != null) {
                        cardSelectionCallback.run();
                    }
                } catch (Exception ex) {
                    ErrorDialog.show("Card selection error: " + ex.getMessage());
                }
            });
            cardButtons.add(cardButton);
        }
    }

    private String suitToSymbol(String suit) {
        return switch (suit.toUpperCase()) {
            case "HEART" -> "♥";
            case "DIAMOND" -> "♦";
            case "CLUB" -> "♣";
            case "SPADE" -> "♠";
            default -> suit;
        };
    }

    public List<Button> getNodes() {
        return cardButtons;
    }
}
