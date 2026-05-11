package engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import engine.board.Board;
import engine.board.SafeZone;
import exception.CannotDiscardException;
import exception.CannotFieldException;
import exception.GameException;
import exception.IllegalDestroyException;
import exception.InvalidCardException;
import exception.InvalidMarbleException;
import exception.SplitOutOfRangeException;
import model.Colour;
import model.card.Card;
import model.card.Deck;
import model.player.*;

@SuppressWarnings("unused")
public class Game implements GameManager {
    private final Board board;
    private final ArrayList<Player> players;
	private int currentPlayerIndex;
    private final ArrayList<Card> firePit;
    private int turn;

    public Game(String playerName) throws IOException {
        turn = 0;
        currentPlayerIndex = 0;
        firePit = new ArrayList<>();

        ArrayList<Colour> colourOrder = new ArrayList<>();
        
        colourOrder.addAll(Arrays.asList(Colour.values()));
        
        Collections.shuffle(colourOrder);
        
        this.board = new Board(colourOrder, this);
        
        Deck.loadCardPool(this.board, (GameManager)this);
        
        this.players = new ArrayList<>();
        this.players.add(new Player(playerName, colourOrder.get(0)));
        
        for (int i = 1; i < 4; i++) 
            this.players.add(new CPU("CPU " + i, colourOrder.get(i), this.board));
        
        for (int i = 0; i < 4; i++) 
            this.players.get(i).setHand(Deck.drawCards());
        
    }
    
    public Board getBoard() {
        return board;
    }

	public int getCurrentPlayerIndex() {
		return currentPlayerIndex;
	}

	public ArrayList<Player> getPlayers() {
        return players;
    }

    public ArrayList<Card> getFirePit() {
        return firePit;
    }
    
    public void selectCard(Card card) throws InvalidCardException {
        players.get(currentPlayerIndex).selectCard(card);
    }

    public void selectMarble(Marble marble) throws InvalidMarbleException {
        players.get(currentPlayerIndex).selectMarble(marble);
    }

    public void deselectAll() {
        players.get(currentPlayerIndex).deselectAll();
    }

    public void editSplitDistance(int splitDistance) throws SplitOutOfRangeException {
        if(splitDistance < 1 || splitDistance > 6)
            throw new SplitOutOfRangeException();

        board.setSplitDistance(splitDistance);
    }

    public boolean canPlayTurn() {
        // Original condition - player has the expected number of cards for this turn
        boolean hasExpectedCards = players.get(currentPlayerIndex).getHand().size() == (4 - turn);
        
        // Check if player has a Queen or Ten card which can always be played
        boolean hasQueenOrTen = false;
        for (Card card : players.get(currentPlayerIndex).getHand()) {
            if (card.getName().contains("Queen") || card.getName().contains("Ten")) {
                hasQueenOrTen = true;
                break;
            }
        }
        
        return hasExpectedCards || hasQueenOrTen;
    }

    public void playPlayerTurn() throws GameException {
        players.get(currentPlayerIndex).play();
    }

    public void endPlayerTurn() {
        Card selected = players.get(currentPlayerIndex).getSelectedCard();
        
        // Only remove a card if the player had one selected (they could have no cards)
        if (selected != null) {
            players.get(currentPlayerIndex).getHand().remove(selected);
            firePit.add(selected);
        }
        
        players.get(currentPlayerIndex).deselectAll();
        
        // Increment the turn counter when completing a full round (after player 3)
        if (currentPlayerIndex == 3) {
            turn++;
        }
        
        // Move to next player
        currentPlayerIndex = (currentPlayerIndex + 1) % 4;
        
        // Check if we've completed a round (after moving to the next player)
        // If we've played 4 rounds (each player has played 4 cards or been skipped)
        if (turn == 4 && currentPlayerIndex == 0) {
            // End of a round (all 4 cards played)
            // Reset turn counter
            turn = 0;
            
            // Refill all players' hands
            for (Player p : players) {
                if(Deck.getPoolSize() < 4) {
                    Deck.refillPool(firePit);
                    firePit.clear();
                }
                ArrayList<Card> newHand = Deck.drawCards();
                p.setHand(newHand);
            }
        }
        
        // Skip players with no cards (they shouldn't get a turn)
        if (players.get(currentPlayerIndex).getHand().isEmpty() && !allPlayersHaveEmptyHands()) {
            // Skip this player's turn by recursively calling endPlayerTurn
            // We don't need to set a card to discard since they have none
            endPlayerTurn();
        }
    }
    
    // Helper method to check if all players have empty hands
    private boolean allPlayersHaveEmptyHands() {
        for (Player p : players) {
            // If any player still has cards, return false
            if (!p.getHand().isEmpty()) {
                return false;
            }
        }
        // All hands are empty
        return true;
    }

    public Colour checkWin() {
        for(SafeZone safeZone : board.getSafeZones()) 
            if(safeZone.isFull())
                return safeZone.getColour();
    
        return null;
    }

    @Override
    public void sendHome(Marble marble) {
        for (Player player : players) {
            if (player.getColour() == marble.getColour()) {
                player.regainMarble(marble);
                break;
            }
        }
    }

    @Override
    public void fieldMarble() throws CannotFieldException, IllegalDestroyException {
        Marble marble = players.get(currentPlayerIndex).getOneMarble();
        
        if (marble == null)
        	throw new CannotFieldException("No marbles left in the Home Zone to field.");
        
        board.sendToBase(marble);
        players.get(currentPlayerIndex).getMarbles().remove(marble);
    }
    
    @Override
    public void discardCard(Colour colour) throws CannotDiscardException {
        for (Player player : players) {
            if (player.getColour() == colour) {
                int handSize = player.getHand().size();
                if(handSize == 0)
                    throw new CannotDiscardException("Player has no cards to discard.");
                int randIndex = (int) (Math.random() * handSize);
                this.firePit.add(player.getHand().remove(randIndex));
            }
        }
    }

    @Override
    public void discardCard() throws CannotDiscardException {
        int randIndex = (int) (Math.random() * 4);
        while(randIndex == currentPlayerIndex)
            randIndex = (int) (Math.random() * 4);

        discardCard(players.get(randIndex).getColour());
    }

    @Override
    public Colour getActivePlayerColour() {
        return players.get(currentPlayerIndex).getColour();
    }

    @Override
    public Colour getNextPlayerColour() {
        return players.get((currentPlayerIndex + 1) % 4).getColour();
    }
    
}
