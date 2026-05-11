package application;

import java.util.ArrayList;

import engine.Game;
import engine.board.Cell;
import engine.board.CellType;
import exception.GameException;
import exception.CannotDiscardException;
import exception.IllegalDestroyException;
import exception.IllegalMovementException;
import model.Colour;
import model.card.Card;
import model.player.Marble;
import model.player.Player;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

/**
 * Helper class to handle entry point behavior in the UI layer.
 * This works around the issue where marbles automatically enter
 * the safe zone without stopping at entry points.
 */
public class EntryPointHandler {
    private final Game game;
    
    public EntryPointHandler(Game game) {
        this.game = game;
    }
    
    /**
     * Checks if a marble would pass its entry point and enter safe zone
     * with the selected card.
     * @param player The current player
     * @param marble The marble being moved
     * @param card The card being played
     * @return true if the marble would skip entry point, false otherwise
     */
    public boolean wouldSkipEntryPoint(Player player, Marble marble, Card card) {
        // Only handle number cards (standard cards with ranks)
        if (!card.getName().matches("\\d+|Ten|Jack|Queen|King|Ace")) {
            return false;
        }
        
        // Get the entry cell for this marble's color
        int entryPosition = getEntryPosition(marble.getColour());
        int marblePosition = getMarblePosition(marble);
        
        // If marble is already at entry point or in safe zone, no issue
        if (marblePosition == entryPosition || marblePosition == -1) {
            return false;
        }
        
        // Check if the move would take the marble past its entry point
        int rank = getRank(card);
        if (rank <= 0) {
            return false; // Not a forward move
        }
        
        // Calculate if the move would pass the entry point
        int distance = calculateDistance(marblePosition, entryPosition);
        return distance > 0 && distance <= rank;
    }
    
    /**
     * Checks if a marble at an entry cell is trying to move into the safe zone
     * with a card rank that's too high
     */
    private boolean isSafeZoneEntryRankTooHigh(Marble marble, Card card) {
        // Check if marble is at its entry position
        int marblePosition = getMarblePosition(marble);
        int entryPosition = getEntryPosition(marble.getColour());
        
        if (marblePosition != entryPosition) {
            return false; // Not at entry, not applicable
        }
        
        // Get the rank of the card
        int rank = getRank(card);
        
        // Safe zone has 4 cells total, so rank > 4 would be too high
        return rank > 4;
    }
    
    /**
     * Checks if there's another marble of the same color in the path into the safe zone
     */
    private boolean isMarbleBlockingSafeZoneEntry(Marble marble, Card card) {
        // Check if marble is at its entry position
        int marblePosition = getMarblePosition(marble);
        int entryPosition = getEntryPosition(marble.getColour());
        
        if (marblePosition != entryPosition) {
            return false; // Not at entry, not applicable
        }
        
        // Get the rank of the card
        int rank = getRank(card);
        if (rank <= 0) {
            return false; // Not a forward move
        }
        
        // Check if there are any marbles in the safe zone that would block the path
        ArrayList<Cell> safeZone = null;
        for (var sz : game.getBoard().getSafeZones()) {
            if (sz.getColour() == marble.getColour()) {
                safeZone = sz.getCells();
                break;
            }
        }
        
        if (safeZone == null) {
            return false;
        }
        
        // Check if any cells in the desired path contain a marble
        for (int i = 0; i < Math.min(rank, safeZone.size()); i++) {
            if (safeZone.get(i).getMarble() != null) {
                return true; // Found a blocking marble
            }
        }
        
        return false;
    }
    
    /**
     * Checks if the card is a King card and needs special handling.
     */
    private boolean isKingCard(Card card) {
        return card != null && card.getName().equals("King");
    }
    
    /**
     * Plays the card for the player, handling the entry point logic.
     * @throws GameException If the play is invalid
     */
    public void handleCardPlay(Player player) throws GameException {
        Card card = player.getSelectedCard();
        
        // If no card selected, use normal behavior
        if (card == null) {
            player.play();
            return;
        }
        
        // Special handling for Queen card with no marbles
        if (card.getName().equals("Queen") && player.getSelectedMarbles().isEmpty()) {
            try {
                player.play();
            } catch (exception.CannotDiscardException e) {
                // If there are no valid targets to discard from, still consider the card played
                player.deselectAll();
                ErrorDialog.show("No other players have cards to discard. Card has been played.");
            }
            return;
        }
        
        // If no marbles selected for other cards, use normal behavior
        if (player.getSelectedMarbles().isEmpty()) {
            player.play();
            return;
        }
        
        Marble marble = player.getSelectedMarbles().get(0);
        
        // Only apply entry point logic for cards with numeric values
        boolean isNumericCard = card.getName().matches("\\d+|Ten|Jack|Queen|King|Ace");
        
        // Special handling for marble at entry cell trying to move into safe zone
        if (isNumericCard && isAtEntryCellToSafeZone(marble)) {
            // Check if rank is too high (> 4)
            if (isSafeZoneEntryRankTooHigh(marble, card)) {
                player.deselectAll();
                ErrorDialog.show("Rank is too high! Maximum safe zone entry is 4 spaces.");
                return;
            }
            
            // Check if there's a marble blocking the path
            if (isMarbleBlockingSafeZoneEntry(marble, card)) {
                player.deselectAll();
                ErrorDialog.show("Cannot bypass my Safe Zone marbles!");
                return;
            }
            
            // Move the marble into the safe zone
            handleSafeZoneEntry(player, marble, card);
            return;
        }
        
        // Special handling for King card to fix destruction bug
        if (isKingCard(card) && player.getSelectedMarbles().size() == 1) {
            handleKingCard(player, marble);
            return;
        }
        
        // Special handling for cases where a marble would skip its entry
        if (isNumericCard && wouldSkipEntryPoint(player, marble, card)) {
            ArrayList<Marble> selectedMarbles = player.getSelectedMarbles();
            player.deselectAll();
            
            // Notify the user
            ErrorDialog.show("Moving marble to its entry point.");
            
            // Move marble exactly to entry position
            moveToEntry(marble);
            
            return;
        }
        
        // Default behavior for all other cases
        player.play();
    }
    
    /**
     * Check if a marble is at its entry cell ready to enter safe zone
     */
    private boolean isAtEntryCellToSafeZone(Marble marble) {
        int marblePosition = getMarblePosition(marble);
        int entryPosition = getEntryPosition(marble.getColour());
        
        // Check that:
        // 1. The marble is at the entry position
        // 2. The marble is the active player's marble
        // 3. The cell at this position is an actual ENTRY cell type
        return marblePosition == entryPosition && 
               marble.getColour() == game.getActivePlayerColour() &&
               game.getBoard().getTrack().get(marblePosition).getCellType() == CellType.ENTRY;
    }
    
    /**
     * Handle moving a marble from the entry cell into the safe zone
     */
    private void handleSafeZoneEntry(Player player, Marble marble, Card card) throws GameException {
        // Get rank of card
        int rank = getRank(card);
        if (rank <= 0) {
            // Non-numeric card, use default behavior
            player.play();
            return;
        }
        
        // Get marble position
        int currentPos = getMarblePosition(marble);
        
        // Get safe zone for this color
        ArrayList<Cell> safeZone = null;
        for (var sz : game.getBoard().getSafeZones()) {
            if (sz.getColour() == marble.getColour()) {
                safeZone = sz.getCells();
                break;
            }
        }
        
        if (safeZone == null || currentPos == -1) {
            player.play();
            return;
        }
        
        // Remove marble from track
        game.getBoard().getTrack().get(currentPos).setMarble(null);
        
        // Place in safe zone at the appropriate distance
        int safePosition = Math.min(rank - 1, safeZone.size() - 1);
        safeZone.get(safePosition).setMarble(marble);
        
        // Deselect card and marble
        player.deselectAll();
    }
    
    /**
     * Special handling for King card to fix the destruction bug
     */
    private void handleKingCard(Player player, Marble marble) throws GameException {
        // Get current position
        int currentPos = getMarblePosition(marble);
        if (currentPos == -1) {
            // If marble is not on track, use regular play (field a new marble)
            player.play();
            return;
        }
        
        // Calculate target position (13 spaces forward)
        int targetPos = (currentPos + 13) % 100;
        
        // Find marbles in the path to destroy them
        ArrayList<Marble> marblesToDestroy = new ArrayList<>();
        int pos = currentPos;
        while (pos != targetPos) {
            pos = (pos + 1) % 100;
            Cell cell = game.getBoard().getTrack().get(pos);
            Marble m = cell.getMarble();
            if (m != null && !m.equals(marble) && m.getColour() != player.getColour()) {
                marblesToDestroy.add(m);
            }
        }
        
        // Remove the source marble
        game.getBoard().getTrack().get(currentPos).setMarble(null);
        
        // Destroy any marbles in the way that are not the player's own marbles
        for (Marble m : marblesToDestroy) {
            int mPos = getMarblePosition(m);
            if (mPos != -1) {
                game.getBoard().getTrack().get(mPos).setMarble(null);
                for (Player p : game.getPlayers()) {
                    if (p.getColour() == m.getColour()) {
                        p.regainMarble(m);
                        break;
                    }
                }
            }
        }
        
        // Check if target is a trap
        Cell targetCell = game.getBoard().getTrack().get(targetPos);
        if (targetCell.isTrap()) {
            // Handle trap
            targetCell.setTrap(false);
            player.regainMarble(marble);
            // Reassign trap
            boolean assigned = false;
            while (!assigned) {
                int randIndex = (int)(Math.random() * 100);
                Cell cell = game.getBoard().getTrack().get(randIndex);
                if (cell.getCellType() == CellType.NORMAL && !cell.isTrap() && cell.getMarble() == null) {
                    cell.setTrap(true);
                    assigned = true;
                }
            }
        } else {
            // Place marble at target
            targetCell.setMarble(marble);
        }
        
        // Deselect card and marbles
        player.deselectAll();
    }
    
    /**
     * Move a marble to its entry position 
     */
    private void moveToEntry(Marble marble) {
        // Get current position and entry position
        int currentPos = getMarblePosition(marble);
        int entryPos = getEntryPosition(marble.getColour());
        
        if (currentPos == -1 || entryPos == -1) {
            return;
        }
        
        // Remove from current position
        game.getBoard().getTrack().get(currentPos).setMarble(null);
        
        // Place at entry position
        game.getBoard().getTrack().get(entryPos).setMarble(marble);
    }
    
    /**
     * Gets the rank of a card
     */
    private int getRank(Card card) {
        String name = card.getName().toLowerCase();
        
        if (name.equals("ace")) return 1;
        if (name.equals("jack")) return 11;
        if (name.equals("queen")) return 12;
        if (name.equals("king")) return 13;
        
        try {
            return Integer.parseInt(name);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Calculate the distance from position to entry point
     */
    private int calculateDistance(int fromPosition, int toPosition) {
        int clockwise = (toPosition - fromPosition + 100) % 100;
        return clockwise;
    }
    
    /**
     * Get a marble's position on the track
     */
    private int getMarblePosition(Marble marble) {
        ArrayList<Cell> track = game.getBoard().getTrack();
        for (int i = 0; i < track.size(); i++) {
            if (track.get(i).getMarble() == marble) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Get the entry position for a color
     */
    private int getEntryPosition(Colour colour) {
        // Use the same logic as in the Board class
        int basePosition = getBasePosition(colour);
        
        if(basePosition == -1)
            return -1;
        else
            return (basePosition - 2 + 100) % 100;
    }
    
    /**
     * Get the base position for a color
     */
    private int getBasePosition(Colour colour) {
        for(int i = 0; i < game.getBoard().getSafeZones().size(); i++) {
            if(game.getBoard().getSafeZones().get(i).getColour() == colour)
                return i * 25;
        }
        
        return -1;
    }
} 