package application;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Displays the welcome screen to enter player name.
 */
public class WelcomeScreen {

    private final StackPane root;

    public WelcomeScreen(Consumer<String> onStart) {
        // Main container
        root = new StackPane();
        root.setPrefSize(1920, 1080); // Increased size for fullscreen
        
        // Create gradient background
        LinearGradient gradient = new LinearGradient(
            0, 0, 0, 1, true, null,
            new Stop(0, Color.web("#3c2f2f")),
            new Stop(1, Color.web("#5a4c42"))
        );
        root.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));
        
        // Content container with stylish border - larger for fullscreen
        VBox content = new VBox(30); // Increased spacing
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(50));
        content.setMaxWidth(600); // Increased width
        content.setMaxHeight(600); // Increased height
        
        // Stylish panel background for content
        content.setBackground(new Background(new BackgroundFill(
            Color.web("#8b7355", 0.85), new CornerRadii(20), Insets.EMPTY // Increased corner radius
        )));
        
        // Add drop shadow to the content panel
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#000000", 0.5));
        shadow.setRadius(20); // Increased shadow radius
        content.setEffect(shadow);
        
        // Game title with shadow effect - larger for fullscreen
        Label title = new Label("JACKAROO");
        title.setFont(Font.font("Verdana", FontWeight.BOLD, 72)); // Increased font size
        title.setTextFill(Color.web("#f9e4b7"));
        title.setTextAlignment(TextAlignment.CENTER);
        
        // Add shadow effect to title
        DropShadow titleShadow = new DropShadow();
        titleShadow.setColor(Color.web("#000000", 0.7));
        titleShadow.setRadius(8); // Increased shadow radius
        title.setEffect(titleShadow);
        
        // Welcome message - larger
        Label subtitle = new Label("The classic board game of strategy and luck");
        subtitle.setFont(Font.font("Verdana", FontWeight.NORMAL, 24)); // Increased font size
        subtitle.setTextFill(Color.web("#f9e4b7"));
        subtitle.setTextAlignment(TextAlignment.CENTER);
        
        // Name input section - larger
        Label nameLabel = new Label("ENTER YOUR NAME:");
        nameLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 22)); // Increased font size
        nameLabel.setTextFill(Color.web("#f9e4b7"));
        
        // Styled text field - larger
        TextField nameField = new TextField();
        nameField.setPromptText("Your Name");
        nameField.setPrefHeight(50); // Increased height
        nameField.setMaxWidth(350); // Increased width
        nameField.setStyle(
            "-fx-background-color: #f9e4b7; " +
            "-fx-background-radius: 10; " + // Increased radius
            "-fx-font-size: 20px; " + // Increased font size
            "-fx-padding: 10;" // Increased padding
        );
        
        // Start button with hover effect - larger
        Button startButton = new Button("START GAME");
        startButton.setPrefSize(250, 60); // Increased size
        startButton.setFont(Font.font("Verdana", FontWeight.BOLD, 24)); // Increased font size
        startButton.setStyle(
            "-fx-background-color: #8B4513; " +
            "-fx-text-fill: #f9e4b7; " +
            "-fx-background-radius: 10; " + // Increased radius
            "-fx-cursor: hand;"
        );
        
        // Hover effect for button
        startButton.setOnMouseEntered(e -> 
            startButton.setStyle(
                "-fx-background-color: #a0522d; " +
                "-fx-text-fill: #ffffff; " +
                "-fx-background-radius: 10; " + // Increased radius
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0.5, 0, 0);" // Increased effect
            )
        );
        
        startButton.setOnMouseExited(e -> 
            startButton.setStyle(
                "-fx-background-color: #8B4513; " +
                "-fx-text-fill: #f9e4b7; " +
                "-fx-background-radius: 10; " + // Increased radius
                "-fx-cursor: hand;"
            )
        );
        
        // Game instructions label - larger
        Label instructionsLabel = new Label("Race your marbles around the board and be the first to get all four into your safe zone!");
        instructionsLabel.setFont(Font.font("Verdana", FontWeight.NORMAL, 16)); // Increased font size
        instructionsLabel.setTextFill(Color.web("#f9e4b7"));
        instructionsLabel.setWrapText(true);
        instructionsLabel.setTextAlignment(TextAlignment.CENTER);
        instructionsLabel.setMaxWidth(500); // Increased width
        
        // Add keyboard shortcut instructions
        Label shortcutsLabel = new Label("• Press F during game to field a marble with Ace/King\n• Press X to exit game at any time");
        shortcutsLabel.setFont(Font.font("Verdana", FontWeight.NORMAL, 14));
        shortcutsLabel.setTextFill(Color.web("#f9e4b7"));
        shortcutsLabel.setTextAlignment(TextAlignment.CENTER);
        shortcutsLabel.setOpacity(0.8);
        
        // Add spacer region for better vertical spacing
        Region spacer1 = new Region();
        spacer1.setMinHeight(25);
        
        Region spacer2 = new Region();
        spacer2.setMinHeight(25);
        
        // Add all elements to content container with improved spacing
        content.getChildren().addAll(
            title, 
            subtitle, 
            spacer1,
            nameLabel, 
            nameField, 
            startButton, 
            spacer2,
            instructionsLabel,
            shortcutsLabel
        );
        
        // Add content to root
        root.getChildren().add(content);
        
        // Add fade-in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(1500), content);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
        
        // Button action
        startButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                onStart.accept(name);
            } else {
                nameField.setStyle(
                    "-fx-background-color: #f9e4b7; " +
                    "-fx-background-radius: 10; " + // Increased radius
                    "-fx-font-size: 20px; " + // Increased font size
                    "-fx-padding: 10; " + // Increased padding
                    "-fx-border-color: red; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 10;" // Increased radius
                );
                
                // Reset style on click
                nameField.setOnMouseClicked(event -> {
                    nameField.setStyle(
                        "-fx-background-color: #f9e4b7; " +
                        "-fx-background-radius: 10; " + // Increased radius
                        "-fx-font-size: 20px; " + // Increased font size
                        "-fx-padding: 10;" // Increased padding
                    );
                });
            }
        });
        
        // Enter key to start game
        nameField.setOnAction(startButton.getOnAction());
    }

    public StackPane getRoot() {
        return root;
    }
}
