package io.github.ggeorg.delos.writer.ui.ruler;

import javafx.beans.value.ObservableValue;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;

/** Shared fixed-size canvas host for read-only writer rulers. */
abstract class RulerCanvasPane extends Pane {
    private final Canvas canvas = new Canvas();

    RulerCanvasPane(String styleClass, double fixedWidth, double fixedHeight) {
        getStyleClass().add(styleClass);
        if (fixedWidth > 0.0) {
            setMinWidth(fixedWidth);
            setPrefWidth(fixedWidth);
            setMaxWidth(fixedWidth);
        }
        if (fixedHeight > 0.0) {
            setMinHeight(fixedHeight);
            setPrefHeight(fixedHeight);
            setMaxHeight(fixedHeight);
        }
        getChildren().add(canvas);
        redrawWhenChanged(widthProperty(), heightProperty());
    }

    protected final Canvas canvas() {
        return canvas;
    }

    @SafeVarargs
    protected final void redrawWhenChanged(ObservableValue<? extends Number>... values) {
        for (ObservableValue<? extends Number> value : values) {
            value.addListener((obs, oldValue, newValue) -> redraw());
        }
    }

    @Override
    protected final void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        redraw();
    }

    protected abstract void redraw();
}
