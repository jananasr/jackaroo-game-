package application;

import engine.Game;
import engine.GameManager;
import exception.GameException;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.stage.StageStyle;
import javafx.geometry.Rectangle2D;

/**
 * Entry point for the JavaFX Jackaroo game.
 */
public class JackarooApp extends Application {

    private Game game;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Configure the main stage for fullscreen
            primaryStage.setTitle("Jackaroo Board Game");
            
            // First make sure window size matches screen
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            primaryStage.setX(screenBounds.getMinX());
            primaryStage.setY(screenBounds.getMinY());
            primaryStage.setWidth(screenBounds.getWidth());
            primaryStage.setHeight(screenBounds.getHeight());
            
            // Additional fullscreen settings
            primaryStage.setMaximized(true);
            
            // Create the welcome screen
            WelcomeScreen welcomeScreen = new WelcomeScreen(name -> {
                try {
                    // Transition to game screen when player submits their name
                    game = new Game(name);
                    GameScreen gameScreen = new GameScreen(game);
                    Scene gameScene = new Scene(gameScreen.getRoot());
                    
                    // Apply CSS from file if exists
                    gameScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
                    
                    // Set up game window
                    primaryStage.setScene(gameScene);
                    primaryStage.setTitle("Jackaroo - " + name + "'s Game");
                    
                    // Explicitly ensure fullscreen mode is set
                    // Do this in a separate UI thread update to ensure it happens after scene transition
                    javafx.application.Platform.runLater(() -> {
                        // Set full screen without exit hint for cleaner experience
                        primaryStage.setFullScreenExitHint("");
                        primaryStage.setFullScreen(true);
                        
                        // Request focus on the game screen to ensure keyboard shortcuts work
                        gameScreen.getRoot().requestFocus();
                    });
                } catch (Exception e) {
                    showError("Error starting game: " + e.getMessage());
                }
            });

            // Create welcome scene
            Scene welcomeScene = new Scene(welcomeScreen.getRoot());
            primaryStage.setScene(welcomeScene);
            primaryStage.setResizable(true);
            primaryStage.setOnCloseRequest(e -> System.exit(0));
            
            // Set to fullscreen mode right from the start
            primaryStage.setFullScreen(true);
            primaryStage.setFullScreenExitHint("Press ESC to exit fullscreen");
            
            // Add CSS from file if exists
            welcomeScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
            
            // Display the window
            primaryStage.show();
            
            // Set keyboard focus to the welcome screen
            welcomeScreen.getRoot().requestFocus();
            
        } catch (Exception e) {
            showError("Initialization error: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
