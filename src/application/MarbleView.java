package application;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import model.Colour;
import model.player.Marble;

/**
 * Visual representation of a marble.
 */
public class MarbleView {

    private final Marble marble;
    private final Circle view;

    public MarbleView(Marble marble) {
        this.marble = marble;
        this.view = new Circle(15);
        applyColor();
    }

    private void applyColor() {
        Colour color = marble.getColour();
        switch (color) {
            case RED -> view.setFill(Color.RED);
            case GREEN -> view.setFill(Color.GREEN);
            case BLUE -> view.setFill(Color.BLUE);
            case YELLOW -> view.setFill(Color.YELLOW);
        }
        view.setStroke(Color.BLACK);
    }

    public Circle getView() {
        return view;
    }

    public Marble getMarble() {
        return marble;
    }
}
