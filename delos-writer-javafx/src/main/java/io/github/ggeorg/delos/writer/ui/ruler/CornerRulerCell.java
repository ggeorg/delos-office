package io.github.ggeorg.delos.writer.ui.ruler;

import javafx.scene.layout.Region;

/** Small visual corner where horizontal and vertical rulers meet. */
public final class CornerRulerCell extends Region {
    public static final double SIZE = 28.0;

    public CornerRulerCell() {
        getStyleClass().add("corner-ruler-cell");
        setMinSize(SIZE, SIZE);
        setPrefSize(SIZE, SIZE);
        setMaxSize(SIZE, SIZE);
    }
}
