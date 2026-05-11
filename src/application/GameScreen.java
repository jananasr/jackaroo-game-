package application;

import engine.Game;
import engine.GameManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.Node;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.stage.Screen;
import javafx.stage.Stage;
import model.card.Card;
import model.player.CPU;
import engine.board.Cell;

import java.util.List;
import java.util.ArrayList;

/**
 * Main game screen showing board, cards, and player info.
 */
public class GameScreen {

    private final BorderPane root;
    private final Game game;
    private final EntryPointHandler entryHandler;

    private final BoardView boardView;
    private final HBox cardHand;
    private final VBox playerInfo;
    private final Label turnLabel;
    private final Label firepitLabel;
    private final Label lastCardLabel;
    private final Button playButton;
    private final Button discardButton;
    private final Button deselectButton;
    private final Button nextTurnButton;

    private Integer splitDistance = null;

    // For board event tracking
    private boolean[] wasTrap = new boolean[100];
    private boolean[] hadMarble = new boolean[100];

    public GameScreen(Game game) {
        this.game = game;
        this.entryHandler = new EntryPointHandler(game);
        this.root = new BorderPane();
        
        // Make root fill available space completely
        root.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.setMinSize(1024, 768); // Minimum size fallback
        
        this.boardView = new BoardView(game);
        this.cardHand = new HBox(10);
        this.playerInfo = new VBox(10);
        this.turnLabel = new Label();
        this.firepitLabel = new Label("Firepit");
        this.lastCardLabel = new Label("None");
        this.playButton = new Button("Play");
        this.discardButton = new Button("Discard");
        this.deselectButton = new Button("Deselect");
        this.nextTurnButton = new Button("Next Turn");

        // Register the marble selection callback with the BoardView
        this.boardView.setMarbleSelectionCallback(this::checkForSevenSplitCondition);
        
        buildLayout();
        setupActions();
        setupKeyboardShortcuts();
        updateView();
        
        // Listen for fullscreen changes and adjust layout if needed
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getWindow().widthProperty().addListener((o, old, newWidth) -> adjustToScreenSize());
                newScene.getWindow().heightProperty().addListener((o, old, newHeight) -> adjustToScreenSize());
            }
        });
    }

    private void buildLayout() {
        // Configure the root to fill the entire screen
        root.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.setPadding(new Insets(15, 10, 15, 10)); // Consistent padding
        root.setStyle("-fx-background-color: #3c2f2f;"); // Dark brown background
        
        // Get screen dimensions for better proportions
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double screenWidth = screenBounds.getWidth();
        double screenHeight = screenBounds.getHeight();
        
        // Style buttons for the top bar
        String buttonStyle = "-fx-background-color: #8b7355; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 120px; -fx-min-height: 40px; -fx-font-size: 14px;";
        playButton.setStyle(buttonStyle);
        discardButton.setStyle(buttonStyle);
        deselectButton.setStyle(buttonStyle);
        nextTurnButton.setStyle(buttonStyle);
        
        // Create horizontal button bar at the top of the screen
        HBox buttonBar = new HBox(20, playButton, discardButton, deselectButton, nextTurnButton);
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.setPadding(new Insets(10));
        buttonBar.setStyle("-fx-background-color: #5a4c42; -fx-padding: 10; -fx-background-radius: 8;");
        
        // Create a top container with the buttons centered
        BorderPane topContainer = new BorderPane();
        topContainer.setCenter(buttonBar);
        topContainer.setPadding(new Insets(0, 0, 15, 0));
        topContainer.setPrefWidth(screenWidth);
        root.setTop(topContainer);
        
        // Create info panel with proper scaling
        VBox leftPanel = new VBox(15, turnLabel, playerInfo);
        leftPanel.setPrefWidth(Math.max(220, screenWidth * 0.15)); // Responsive width
        leftPanel.setMinWidth(220);
        leftPanel.setMaxWidth(300);
        leftPanel.setPadding(new Insets(15));
        leftPanel.setStyle("-fx-background-color: #5a4c42; -fx-padding: 15; -fx-background-radius: 8;");
        turnLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        root.setLeft(leftPanel);
        
        // Calculate optimal board size (80% width, 70% height)
        double boardWidth = screenWidth * 0.80;
        double boardHeight = screenHeight * 0.70;
        
        // Create a StackPane for the board and firepit overlay
        StackPane boardWithFirepit = new StackPane();
        
        // Create a container to control the board size
        StackPane boardContainer = new StackPane();
        boardContainer.getChildren().add(boardView.getNode());
        boardContainer.setMinSize(boardWidth, boardHeight);
        boardContainer.setMaxSize(boardWidth, boardHeight);
        boardContainer.setPrefSize(boardWidth, boardHeight);
        boardContainer.setAlignment(Pos.CENTER);
        boardContainer.setStyle("-fx-background-color: transparent;");
        
        // Style firepit label for center positioning
        firepitLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18px;");
        
        // Create firepit container - 20% smaller than before
        VBox firepitBox = new VBox(8);
        firepitBox.setAlignment(Pos.CENTER);
        firepitBox.setPadding(new Insets(12));
        firepitBox.setMaxWidth(144); // 20% smaller than 180
        firepitBox.setMaxHeight(80); // 20% smaller than 100
        firepitBox.setStyle(
            "-fx-background-color: #5a4c42; " +
            "-fx-background-radius: 20; " +
            "-fx-border-color: #8b7355; " +
            "-fx-border-width: 3; " +
            "-fx-border-radius: 20; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 10, 0.5, 0, 0);"
        );

        // Create container for last played card
        StackPane lastCardContainer = new StackPane();
        lastCardContainer.setMinSize(80, 60);
        lastCardContainer.setMaxSize(80, 60);
        lastCardContainer.setStyle(
            "-fx-background-color: #f9e4b7; " +
            "-fx-background-radius: 5; " +
            "-fx-border-color: #333333; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 5;"
        );

        // Use the class field lastCardLabel
        lastCardLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        lastCardContainer.getChildren().add(lastCardLabel);

        // Add to firepit box
        firepitBox.getChildren().addAll(firepitLabel, lastCardContainer);
        
        // Add both the board and firepit to the overlapping container
        boardWithFirepit.getChildren().addAll(boardContainer, firepitBox);
        boardWithFirepit.setAlignment(Pos.CENTER);
        
        // Center the container in the available space
        StackPane centerPane = new StackPane();
        centerPane.setPrefWidth(Double.MAX_VALUE);
        centerPane.setPrefHeight(Double.MAX_VALUE);
        centerPane.setMinHeight(screenHeight * 0.7);
        centerPane.setAlignment(Pos.CENTER);
        centerPane.setStyle("-fx-background-color: transparent;");
        centerPane.getChildren().add(boardWithFirepit);
        
        // Set the board in the center
        root.setCenter(centerPane);
        
        // We don't need the right panel anymore since firepit is in the center
        root.setRight(null);

        // Style and position card hand at the bottom
        cardHand.setPadding(new Insets(15));
        cardHand.setMinHeight(150);
        cardHand.setPrefHeight(Math.max(150, screenHeight * 0.15));
        cardHand.setAlignment(Pos.CENTER);
        cardHand.setStyle("-fx-background-color: #5a4c42; -fx-padding: 15; -fx-background-radius: 8 8 0 0;");
        root.setBottom(cardHand);
    }

    private void setupActions() {
        playButton.setOnAction(e -> {
            try {
                recordBoardState();
                // Check explicitly for Seven card and 2 marbles condition
                if (isSevenCardSelected() && getSelectedMarbleCount() == 2) {
                    showSplitDialogAndPlay();
                } else {
                    // Use the entry handler instead of direct play
                    entryHandler.handleCardPlay(game.getPlayers().get(game.getCurrentPlayerIndex()));
                    game.endPlayerTurn();
                    splitDistance = null;
                    flashBoardEvents();
                    // Explicitly update the firepit display right after a card is played
                    updateFirepitDisplay();
                    updateView();
                    if (game.checkWin() != null) {
                        new WinnerDialog(game.checkWin()).show();
                    }
                }
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        discardButton.setOnAction(e -> {
            try {
                var player = game.getPlayers().get(game.getCurrentPlayerIndex());
                var hand = player.getHand();
                if (!hand.isEmpty()) {
                    model.card.Card toDiscard = null;
                    int idx = -1;
                    for (int i = 0; i < hand.size(); i++) {
                        if (hand.get(i) != null) {
                            toDiscard = hand.get(i);
                            idx = i;
                            break;
                        }
                    }
                    if (toDiscard != null) {
                        player.selectCard(toDiscard); // Mark for discard
                        game.endPlayerTurn();
                        updateView();
                    } else {
                        showError("No valid card to discard.");
                    }
                } else {
                    showError("No cards in hand to discard.");
                }
            } catch (Exception ex) {
                showError("Discard error: " + ex.getMessage());
            }
        });

        deselectButton.setOnAction(e -> {
            game.deselectAll();
            updateView();
        });

        nextTurnButton.setOnAction(e -> {
            if (game.getCurrentPlayerIndex() == 0) {
                game.deselectAll();
                game.endPlayerTurn();
                // Explicitly update the firepit display right after ending turn
                updateFirepitDisplay();
                updateView();
            }
        });
    }

    private void updateView() {
        boardView.update();
        playerInfo.getChildren().setAll(new PlayerPanel(game).getNodes());
        
        // Only show cards for human player (index 0)
        if (game.getCurrentPlayerIndex() == 0) {
            // Create a custom CardView with our callback for Seven card selection
            CardView cardView = new CardView(game);
            cardView.setCardSelectionCallback(() -> {
                // Check immediately if we have a Seven card and 2 marbles selected
                checkForSevenSplitCondition();
            });
            
            cardHand.getChildren().setAll(cardView.getNodes());
        } else {
            // Clear the card hand for CPU players
            cardHand.getChildren().clear();
        }
        
        String current = game.getActivePlayerColour().toString();
        String next = game.getNextPlayerColour().toString();
        turnLabel.setText("Current Turn: " + current + " | Next: " + next);
        
        // Update firepit display
        updateFirepitDisplay();
        
        updateButtonStates();
        handleCPUTurns();
    }

    private void updateButtonStates() {
        var player = game.getPlayers().get(game.getCurrentPlayerIndex());
        var hand = player.getHand();
        
        // Clean null cards from the hand
        hand.removeIf(card -> card == null);
        
        boolean stuck = isPlayerStuck();
        boolean hasCards = !hand.isEmpty();
        boolean isHumanPlayer = game.getCurrentPlayerIndex() == 0;
        
        // Check specifically for Ten or Queen card
        boolean hasQueenOrTenCard = false;
        for (var card : hand) {
            if (card == null) continue;
            String name = card.getName().toLowerCase();
            if (name.contains("queen") || name.contains("ten")) {
                hasQueenOrTenCard = true;
                break;
            }
        }

        // Set button states - allow playing if player has a Queen or Ten card, even if "stuck"
        playButton.setDisable((stuck && !hasQueenOrTenCard) || !hasCards);
        discardButton.setDisable((!stuck || hasQueenOrTenCard) || !hasCards);
        nextTurnButton.setDisable(!isHumanPlayer);

        // Only auto-end turn for CPU players, never for human players
        if (!isHumanPlayer) {
            // If CPU player has no cards, auto-end their turn
            if (!hasCards) {
                PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
                pause.setOnFinished(e -> {
                    game.endPlayerTurn();
                    updateView();
                });
                pause.play();
            }
            // If CPU player is stuck and cannot discard, skip turn
            else if (stuck && !canDiscard()) {
                PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
                pause.setOnFinished(e -> {
                    game.endPlayerTurn();
                    updateView();
                });
                pause.play();
            }
        }
        // For human player, just update button states but never auto-end turn
        else {
            // Show a hint message for human player if they are stuck or have no cards
            if ((stuck || !hasCards) && !canDiscard()) {
                // You can add a label or some visual indication that the player should end their turn
                System.out.println("Human player cannot make a move. Please click Next Turn.");
            }
        }
    }

    private boolean isPlayerStuck() {
        var player = game.getPlayers().get(game.getCurrentPlayerIndex());
        var hand = player.getHand();
        
        // Only count marbles on the main track (not in safe zone)
        boolean hasMarbleOnTrack = false;
        for (var cell : game.getBoard().getTrack()) {
            var marble = cell.getMarble();
            if (marble != null && marble.getColour() == game.getActivePlayerColour()) {
                hasMarbleOnTrack = true;
                break;
            }
        }
        
        boolean hasFieldCard = false;
        boolean hasQueenOrTenCard = false;
        
        for (var card : hand) {
            // Skip null cards
            if (card == null) continue;
            
            String name = card.getName().toLowerCase();
            if (name.equals("king") || name.equals("ace")) {
                hasFieldCard = true;
            }
            
            // Queen and Ten can be played without marbles for discard effects
            if (name.contains("queen") || name.contains("ten")) {
                hasQueenOrTenCard = true;
            }
        }
        
        // Player is not stuck if they have a Queen or Ten card (can play without marbles)
        return !hasMarbleOnTrack && !hasFieldCard && !hasQueenOrTenCard;
    }

    private void handleCPUTurns() {
        // Extra safety check - never process human player's turn
        if (game.getCurrentPlayerIndex() == 0) {
            return;
        }
        
        // If the current player is a CPU, automate their turn after a short delay
        if (game.getPlayers().get(game.getCurrentPlayerIndex()) instanceof CPU) {
            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> {
                try {
                    // Double-check that we're still on a CPU player's turn (in case player changed during delay)
                    if (game.getCurrentPlayerIndex() == 0 || !(game.getPlayers().get(game.getCurrentPlayerIndex()) instanceof CPU)) {
                        return;
                    }
                    
                    var player = game.getPlayers().get(game.getCurrentPlayerIndex());
                    var hand = player.getHand();
                    
                    // Clean null cards from hand first
                    hand.removeIf(card -> card == null);
                    
                    boolean hasCards = !hand.isEmpty();
                    boolean stuck = isPlayerStuck();
                    // If stuck and cannot discard, skip turn
                    if ((stuck || !hasCards) && !canDiscard()) {
                        game.endPlayerTurn();
                        updateView();
                        return;
                    }
                    // Check if CPU has no cards, and skip turn if so
                    if (!hasCards) {
                        game.endPlayerTurn();
                        updateView();
                        return;
                    }
                    
                    recordBoardState();
                    if (stuck) {
                        // Discard a random card from the CPU's hand
                        if (!hand.isEmpty()) {
                            // Find a non-null card to discard
                            Card toDiscard = null;
                            for (Card c : hand) {
                                if (c != null) {
                                    toDiscard = c;
                                    break;
                                }
                            }
                                if (toDiscard != null) {
                                    player.selectCard(toDiscard);
                                game.endPlayerTurn();
                                updateView();
                            } else {
                                game.endPlayerTurn();
                                updateView();
                            }
                        } else {
                            game.endPlayerTurn();
                            updateView();
                        }
                    } else {
                        // Let the CPU play its turn
                        game.playPlayerTurn();
                        game.endPlayerTurn();
                        // Make sure to update the firepit display when CPU plays
                        updateFirepitDisplay();
                        flashBoardEvents();
                        updateView();
                    }
                    if (game.checkWin() != null) {
                        new WinnerDialog(game.checkWin()).show();
                    }
                } catch (Exception ex) {
                    showError("CPU turn error: " + ex.getMessage());
                }
            });
            pause.play();
        }
    }

    private void showError(String message) {
        ErrorDialog.show(message);
    }

    private boolean isSevenCardSelected() {
        Card selected = game.getPlayers().get(game.getCurrentPlayerIndex()).getSelectedCard();
        return selected != null && selected.getName().equalsIgnoreCase("7");
    }

    private int getSelectedMarbleCount() {
        return game.getPlayers().get(game.getCurrentPlayerIndex()).getSelectedMarbles().size();
    }

    private void showSplitDialogAndPlay() {
        TextInputDialog dialog = new TextInputDialog(""); // Remove the default "3" value
        dialog.setTitle("Split Seven Card Move");
        dialog.setHeaderText("How would you like to split the 7 spaces?");
        dialog.setContentText("Enter a number between 1-6 for the first marble (remaining goes to second marble):");
        
        // Make dialog look nicer
        dialog.getDialogPane().setStyle(
            "-fx-background-color: #5a4c42; " +
            "-fx-text-fill: white;"
        );
        dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK).setStyle(
            "-fx-background-color: #8b7355; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold;"
        );
        
        // Log that we're showing the dialog
        System.out.println("Showing split dialog for Seven card");
        
        dialog.showAndWait().ifPresent(response -> {
            try {
                int splitValue = Integer.parseInt(response);
                if (splitValue < 1 || splitValue > 6) {
                    showError("Split value must be between 1 and 6.");
                    return;
                }
                
                // Set split distance for the first marble
                game.editSplitDistance(splitValue);
                System.out.println("Split distance set to: " + splitValue + " (and " + (7-splitValue) + ")");
                
                // Play the card
                entryHandler.handleCardPlay(game.getPlayers().get(game.getCurrentPlayerIndex()));
                game.endPlayerTurn();
                splitDistance = null;
                
                // Update firepit display after a split play
                updateFirepitDisplay();
                flashBoardEvents();
                updateView();
                
                if (game.checkWin() != null) {
                    new WinnerDialog(game.checkWin()).show();
                }
            } catch (NumberFormatException e) {
                showError("Please enter a valid number.");
            } catch (Exception e) {
                showError("Error: " + e.getMessage());
            }
        });
    }

    private void recordBoardState() {
        for (int i = 0; i < game.getBoard().getTrack().size(); i++) {
            Cell cell = game.getBoard().getTrack().get(i);
            wasTrap[i] = cell.isTrap();
            hadMarble[i] = cell.getMarble() != null;
        }
    }

    private void flashBoardEvents() {
        for (int i = 0; i < game.getBoard().getTrack().size(); i++) {
            Cell cell = game.getBoard().getTrack().get(i);
            // Flash destroyed marble
            if (hadMarble[i] && cell.getMarble() == null && !wasTrap[i]) {
                boardView.flashDestroyedCell(i);
            }
            // Flash triggered trap
            if (wasTrap[i] && !cell.isTrap()) {
                boardView.flashTrapCell(i);
            }
        }
    }

    private boolean canDiscard() {
        // Check if any other player has cards to discard
        for (int i = 0; i < game.getPlayers().size(); i++) {
            if (i != game.getCurrentPlayerIndex() && 
                !game.getPlayers().get(i).getHand().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public BorderPane getRoot() {
        return root;
    }

    // Method to check if a Seven card and exactly 2 marbles are selected
    private void checkForSevenSplitCondition() {
        // Only check for human player (index 0) to avoid interfering with CPU turns
        if (game.getCurrentPlayerIndex() == 0) {
            boolean hasSeven = isSevenCardSelected();
            int marbleCount = getSelectedMarbleCount();
            
            // Log state for debugging
            System.out.println("Checking split condition: Seven card: " + hasSeven + ", Marble count: " + marbleCount);
            
            // Show visual indicator if conditions are met - highlight play button
            if (hasSeven && marbleCount == 2) {
                playButton.setStyle("-fx-background-color: #8b7355; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 120px; -fx-min-height: 40px; -fx-font-size: 14px; -fx-border-color: yellow; -fx-border-width: 3px;");
            } else {
                // Reset play button style if conditions are not met
                playButton.setStyle("-fx-background-color: #8b7355; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 120px; -fx-min-height: 40px; -fx-font-size: 14px;");
            }
        }
    }

    private void setupKeyboardShortcuts() {
        root.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case F:
                    // Only process for human player
                    if (game.getCurrentPlayerIndex() == 0) {
                        handleFieldShortcut();
                    }
                    break;
                case X:
                    // Exit application
                    javafx.application.Platform.exit();
                    break;
                case F11:
                    // Toggle fullscreen
                    if (root.getScene() != null && root.getScene().getWindow() instanceof Stage) {
                        Stage stage = (Stage) root.getScene().getWindow();
                        stage.setFullScreen(!stage.isFullScreen());
                    }
                    break;
                default:
                    break;
            }
        });
        
        // Make sure the root pane can receive focus for key events
        root.setFocusTraversable(true);
    }

    private void handleFieldShortcut() {
        var player = game.getPlayers().get(0); // Human player
        var hand = player.getHand();
        
        // Check if player has Ace or King
        Card aceOrKing = null;
        for (Card card : hand) {
            if (card == null) continue;
            String name = card.getName().toLowerCase();
            if (name.equals("king") || name.equals("ace")) {
                aceOrKing = card;
                break;
            }
        }
        
        // If Ace or King found, try to field a marble
        if (aceOrKing != null) {
            try {
                // Select the card
                player.selectCard(aceOrKing);
                // Attempt to field a marble (play with no marbles selected)
                recordBoardState();
                entryHandler.handleCardPlay(player);
                game.endPlayerTurn();
                flashBoardEvents();
                updateView();
                
                if (game.checkWin() != null) {
                    new WinnerDialog(game.checkWin()).show();
                }
            } catch (Exception ex) {
                showError("Cannot field a marble: " + ex.getMessage());
            }
        } else {
            showError("You need an Ace or King card to field a marble.");
        }
    }

    // Method to adjust layout based on current screen size
    private void adjustToScreenSize() {
        if (root.getScene() != null && root.getScene().getWindow() != null) {
            double width = root.getScene().getWindow().getWidth();
            double height = root.getScene().getWindow().getHeight();
            
            // Rebuild layout with current dimensions
            buildLayout();
        }
    }

    // Add this method to directly update the firepit display
    private void updateFirepitDisplay() {
        // Update the last played card display
        if (!game.getFirePit().isEmpty()) {
            Card topCard = game.getFirePit().get(game.getFirePit().size() - 1);
            if (topCard != null) {
                // Try to get suit if available
                String cardText = topCard.getName();
                String suitSymbol = "";
                String textColor = "-fx-text-fill: black;";
                
                try {
                    java.lang.reflect.Method getSuit = topCard.getClass().getMethod("getSuit");
                    Object suitObj = getSuit.invoke(topCard);
                    if (suitObj != null) {
                        String suitName = suitObj.toString().toUpperCase();
                        suitSymbol = switch (suitName) {
                            case "HEART" -> "♥";
                            case "DIAMOND" -> "♦";
                            case "CLUB" -> "♣";
                            case "SPADE" -> "♠";
                            default -> suitObj.toString();
                        };
                        
                        // Set color based on suit
                        if (suitName.equals("HEART") || suitName.equals("DIAMOND")) {
                            textColor = "-fx-text-fill: #cc0000;"; // Red for hearts and diamonds
                        }
                    }
                } catch (Exception ignored) {}
                
                // Create a styled card display
                lastCardLabel.setText(cardText + "\n" + suitSymbol);
                lastCardLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; " + textColor + " -fx-text-alignment: center;");
            } else {
                lastCardLabel.setText("None");
                lastCardLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            }
        } else {
            lastCardLabel.setText("None");
            lastCardLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        }
    }
}