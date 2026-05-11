package application;

import engine.Game;
import engine.board.BoardManager;
import engine.board.Cell;
import engine.board.CellType;
import engine.board.SafeZone;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import model.Colour;
import model.player.Marble;
import model.player.Player;
import model.card.Card;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Visual representation of the game board.
 */
public class BoardView {

    private final GridPane boardGrid;
    private final Game game;
    private List<Marble> selectedMarbles = new ArrayList<>();
    // Track recently destroyed and trap-triggered cells
    private int lastDestroyedCellIndex = -1;
    private int lastTrapCellIndex = -1;
    
    // Map to track cell positions in the grid
    private Map<Integer, int[]> trackPositions = new HashMap<>();
    private Map<Integer, StackPane> cellNodes = new HashMap<>();
    
    // Grid dimensions
    private final int GRID_WIDTH = 40;
    private final int GRID_HEIGHT = 12;
    
    // Cell size
    private double cellSize;

    // Add a callback for marble selection
    private Runnable marbleSelectionCallback;
    
    public void setMarbleSelectionCallback(Runnable callback) {
        this.marbleSelectionCallback = callback;
    }

    public BoardView(Game game) {
        this.game = game;
        this.boardGrid = new GridPane();
        
        // Get screen dimensions
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        
        // Calculate cell size based on available space - ensuring the track takes exactly 70% of screen
        double boardWidth = screenBounds.getWidth() * 0.75;
        double boardHeight = screenBounds.getHeight() * 0.7;
        
        // Calculate cell size based on grid dimensions - make cells slightly smaller to ensure they all fit
        cellSize = Math.min(boardWidth / (GRID_WIDTH + 2), boardHeight / (GRID_HEIGHT + 1));
        
        boardGrid.setAlignment(Pos.CENTER_LEFT);
        boardGrid.setHgap(1);
        boardGrid.setVgap(1);
        boardGrid.setStyle("-fx-background-color: transparent;");
        
        buildBoard();
    }

    private void buildBoard() {
        // Create the track (cells 0-99) around the perimeter
        createPerimeterTrack();
        
        // Create the safe zones in a plus sign pattern
        createSafeZones();
        
        // Add marbles to the board
        placeMarbles();
    }
    
    private void createPerimeterTrack() {
        // Start from top-left and go clockwise
        int trackIndex = 0;
        
        // Top row
        for (int x = 0; x < GRID_WIDTH; x++) {
            createTrackCell(trackIndex++, x, 0);
        }
        
        // Right column (excluding top corner)
        for (int y = 1; y < GRID_HEIGHT; y++) {
            createTrackCell(trackIndex++, GRID_WIDTH - 1, y);
        }
        
        // Bottom row (excluding right corner)
        for (int x = GRID_WIDTH - 2; x >= 0; x--) {
            createTrackCell(trackIndex++, x, GRID_HEIGHT - 1);
        }
        
        // Left column (excluding bottom corner)
        for (int y = GRID_HEIGHT - 2; y > 0; y--) {
            createTrackCell(trackIndex++, 0, y);
        }
    }
    
    private void createTrackCell(int index, int x, int y) {
        Cell cell = null;
        
        // Find the corresponding cell in the game model
        if (index < game.getBoard().getTrack().size()) {
            cell = game.getBoard().getTrack().get(index);
        }
        
        StackPane cellNode = new StackPane();
        Rectangle rect = new Rectangle(cellSize, cellSize);
        rect.setStroke(Color.BLACK);
        
        // Apply appropriate styling based on cell type
        Color fillColor;
        
        if (index == lastDestroyedCellIndex) {
            fillColor = Color.YELLOW;
        } else if (index == lastTrapCellIndex) {
            fillColor = Color.MAGENTA;
        } else if (cell != null && cell.getCellType() == CellType.BASE) {
            fillColor = Color.LIGHTBLUE;
        } else if (cell != null && cell.getCellType() == CellType.ENTRY) {
            fillColor = Color.LIGHTGREEN;
        } else if (cell != null && cell.isTrap()) {
            fillColor = Color.RED;
        } else {
            fillColor = Color.WHITE;
        }
        
        rect.setFill(fillColor);
        
        // Add cell number label
        Label cellNumber = new Label(Integer.toString(index));
        cellNumber.setFont(Font.font("Arial", FontWeight.BOLD, cellSize / 4));
        
        cellNode.getChildren().addAll(rect, cellNumber);
        boardGrid.add(cellNode, x, y);
        
        // Save the position for later reference
        trackPositions.put(index, new int[]{x, y});
        cellNodes.put(index, cellNode);
    }
    
    private void createSafeZones() {
        // Find the entry cells for each color
        List<Cell> track = game.getBoard().getTrack();
        Map<Colour, ArrayList<Cell>> safeZones = new HashMap<>();
        
        // Map safe zones to their colors for easy access
        for (SafeZone safeZone : game.getBoard().getSafeZones()) {
            safeZones.put(safeZone.getColour(), safeZone.getCells());
        }
        
        // Create a mapping of player indices to their entry positions
        int[] entryPositions = {98, 23, 48, 73}; // Entry positions for players 0, 1, 2, 3
        
        // Create safe zones for each player and their corresponding color
        for (int playerIdx = 0; playerIdx < game.getPlayers().size(); playerIdx++) {
            Player player = game.getPlayers().get(playerIdx);
            Colour playerColour = player.getColour();
            int entryPosition = entryPositions[playerIdx];
            
            // Find the SafeZone with the matching color
            SafeZone matchingSafeZone = null;
            for (SafeZone safeZone : game.getBoard().getSafeZones()) {
                if (safeZone.getColour() == playerColour) {
                    matchingSafeZone = safeZone;
                    break;
                }
            }
            
            if (matchingSafeZone != null && trackPositions.containsKey(entryPosition)) {
                // Get entry cell coordinates
                int[] entryCoords = trackPositions.get(entryPosition);
                
                // Determine direction based on entry position
                String direction = getDirectionFromEntryPosition(entryPosition);
                
                // Get JavaFX color for rendering
                Color javaFxColor = convertToJavaFxColor(playerColour);
                
                // Create the safe zone
                List<StackPane> safeCells = createSafeZoneFromEntry(entryCoords[0], entryCoords[1], direction, 4, javaFxColor);
                
                // Store references to safe zone cells for marble rendering
                safeZoneCells.put(playerColour, safeCells);
                
                // Debug output to verify connections
                System.out.println("Connected safe zone for " + 
                                  (playerIdx == 0 ? "Human" : "CPU " + playerIdx) + 
                                  " with color " + playerColour + 
                                  " at entry position " + entryPosition);
            }
        }
        
        // Place marbles that are in safe zones
        for (SafeZone safeZone : game.getBoard().getSafeZones()) {
            ArrayList<Cell> cells = safeZone.getCells();
            for (int i = 0; i < cells.size(); i++) {
                Cell cell = cells.get(i);
                Marble marble = cell.getMarble();
                if (marble != null && safeZoneCells.containsKey(safeZone.getColour())) {
                    List<StackPane> safeCellNodes = safeZoneCells.get(safeZone.getColour());
                    if (i < safeCellNodes.size()) {
                        StackPane cellNode = safeCellNodes.get(i);
                        addMarbleToCell(cellNode, marble);
                    }
                }
            }
        }
    }
    
    /**
     * Gets the base position for a given color
     * Implementation follows the same logic as in the Board class
     */
    private int getBasePosition(Colour colour) {
        // Find which player has this color
        for (int playerIdx = 0; playerIdx < game.getPlayers().size(); playerIdx++) {
            if (game.getPlayers().get(playerIdx).getColour() == colour) {
                // Human player (index 0): position 0
                if (playerIdx == 0) return 0;
                // CPU players (index 1-3): positions 25, 50, 75
                return playerIdx * 25;
            }
        }
        return -1;
    }
    
    /**
     * Gets the entry position for a given color
     * Returns fixed entry positions for each player
     */
    private int getEntryPosition(Colour colour) {
        // Find which player has this color
        for (int playerIdx = 0; playerIdx < game.getPlayers().size(); playerIdx++) {
            if (game.getPlayers().get(playerIdx).getColour() == colour) {
                // Human player (index 0): entry position 98
                if (playerIdx == 0) return 98;
                // CPU 1 (index 1): entry position 23
                if (playerIdx == 1) return 23;
                // CPU 2 (index 2): entry position 48
                if (playerIdx == 2) return 48;
                // CPU 3 (index 3): entry position 73
                if (playerIdx == 3) return 73;
            }
        }
        return -1;
    }
    
    /**
     * Determines the direction for a safe zone based on the entry position
     */
    private String getDirectionFromEntryPosition(int entryPosition) {
        // Determine which side of the board the entry is on
        if (entryPosition >= 0 && entryPosition < 25) {
            // Top side - safe zone goes down
            return "down";
        } else if (entryPosition >= 25 && entryPosition < 50) {
            // Right side - safe zone goes left
            return "left";
        } else if (entryPosition >= 50 && entryPosition < 75) {
            // Bottom side - safe zone goes up
            return "up";
        } else {
            // Left side - safe zone goes right
            return "right";
        }
    }
    
    // Store references to safe zone cells for each color
    private Map<Colour, List<StackPane>> safeZoneCells = new HashMap<>();
    
    private List<StackPane> createSafeZoneFromEntry(int entryX, int entryY, String direction, int length, Color color) {
        // Calculate the starting position - one cell away from the entry to avoid overlap
        int startX = entryX;
        int startY = entryY;
        
        // Determine the direction of the safe zone
        int dx = 0;
        int dy = 0;
        
        switch (direction) {
            case "down":
                dy = 1;
                startY += 1; // Start one cell below
                break;
            case "up":
                dy = -1;
                startY -= 1; // Start one cell above
                break;
            case "right":
                dx = 1;
                startX += 1; // Start one cell to the right
                break;
            case "left":
                dx = -1;
                startX -= 1; // Start one cell to the left
                break;
        }
        
        List<StackPane> safeCells = new ArrayList<>();
        
        // Create the safe zone cells
        for (int i = 0; i < length; i++) {
            int x = startX + (i * dx);
            int y = startY + (i * dy);
            
            // Make sure coordinates are valid
            if (x >= 0 && x < GRID_WIDTH && y >= 0 && y < GRID_HEIGHT) {
                Rectangle rect = new Rectangle(cellSize, cellSize);
                rect.setFill(color.deriveColor(0, 1, 1.5, 0.7)); // Lighter version of color
                rect.setStroke(color);
                rect.setStrokeWidth(2);
                
                StackPane cellNode = new StackPane(rect);
                boardGrid.add(cellNode, x, y);
                
                // Store the cell for later reference
                safeCells.add(cellNode);
            }
        }
        
        return safeCells;
    }
    
    private void addMarbleToCell(StackPane cellNode, Marble marble) {
        Circle circle = new Circle(cellSize / 3);
        
        switch (marble.getColour()) {
            case RED -> circle.setFill(Color.RED);
            case GREEN -> circle.setFill(Color.GREEN);
            case BLUE -> circle.setFill(Color.BLUE);
            case YELLOW -> circle.setFill(Color.YELLOW);
        }
        
        // Highlight if selected
        if (game.getPlayers().get(game.getCurrentPlayerIndex()).getSelectedMarbles() != null &&
            game.getPlayers().get(game.getCurrentPlayerIndex()).getSelectedMarbles().contains(marble)) {
            circle.setEffect(new DropShadow(15, Color.DEEPSKYBLUE));
        }
        
        // Add click handler for the marble
        circle.setOnMouseClicked((MouseEvent e) -> {
            try {
                game.selectMarble(marble);
                update();
                
                // Notify the GameScreen that a marble has been selected
                if (marbleSelectionCallback != null) {
                    marbleSelectionCallback.run();
                }
            } catch (Exception ex) {
                ErrorDialog.show("Marble selection error: " + ex.getMessage());
            }
        });
        
        // Add the marble to the cell
        cellNode.getChildren().add(circle);
    }
    
    private void placeMarbles() {
        // Place marbles on the board according to game state
        List<Cell> track = game.getBoard().getTrack();
        
        for (int i = 0; i < track.size(); i++) {
            Cell cell = track.get(i);
            Marble marble = cell.getMarble();
            
            if (marble != null && trackPositions.containsKey(i)) {
                int[] pos = trackPositions.get(i);
                StackPane cellNode = cellNodes.get(i);
                
                if (cellNode != null) {
                    addMarbleToCell(cellNode, marble);
                }
            }
        }
    }

    public void update() {
        boardGrid.getChildren().clear();
        buildBoard();
    }

    public GridPane getNode() {
        return boardGrid;
    }

    public void flashDestroyedCell(int cellIndex) {
        lastDestroyedCellIndex = cellIndex;
        update();
        PauseTransition pause = new PauseTransition(Duration.seconds(0.4));
        pause.setOnFinished(e -> {
            lastDestroyedCellIndex = -1;
            update();
        });
        pause.play();
    }

    public void flashTrapCell(int cellIndex) {
        lastTrapCellIndex = cellIndex;
        update();
        PauseTransition pause = new PauseTransition(Duration.seconds(0.4));
        pause.setOnFinished(e -> {
            lastTrapCellIndex = -1;
            update();
        });
        pause.play();
    }

    private Color convertToJavaFxColor(Colour gameColor) {
        switch (gameColor) {
            case RED:
                return Color.RED;
            case GREEN:
                return Color.GREEN;
            case BLUE:
                return Color.BLUE;
            case YELLOW:
                return Color.YELLOW;
            default:
                return Color.BLACK; // Should never happen
        }
    }
}
