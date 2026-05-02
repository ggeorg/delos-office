package io.github.ggeorg.delos.javafx.chrome;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Shared bottom status-line base for Delos JavaFX apps.
 * <p>
 * App-specific status bars still own their text and update logic; this class
 * only standardizes the layout and CSS hooks.
 */
public class DelosStatusLine extends HBox {
    protected DelosStatusLine() {
        getStyleClass().add("delos-status-line");
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(3, 10, 3, 10));
        setSpacing(12);
    }

    protected final Label statusItem(String... styleClasses) {
        Label label = new Label();
        label.getStyleClass().add("delos-status-item");
        label.getStyleClass().add("status-label");
        for (String styleClass : styleClasses) {
            if (styleClass != null && !styleClass.isBlank()) {
                label.getStyleClass().add(styleClass);
            }
        }
        return label;
    }

    protected final Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
}
