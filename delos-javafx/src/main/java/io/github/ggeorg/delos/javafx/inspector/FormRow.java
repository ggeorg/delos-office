package io.github.ggeorg.delos.javafx.inspector;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.Objects;

/** Shared label + control row for inspector forms. */
public class FormRow extends HBox {
    private final Label label;

    public FormRow(String labelText, Node control) {
        super(8.0);
        Objects.requireNonNull(control, "control");
        label = new Label(requireText(labelText));
        label.getStyleClass().add("delos-inspector-form-label");
        control.getStyleClass().add("delos-inspector-form-control");
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("delos-inspector-form-row");
        HBox.setHgrow(control, Priority.ALWAYS);
        getChildren().setAll(label, control);
    }

    public Label label() {
        return label;
    }

    private static String requireText(String value) {
        Objects.requireNonNull(value, "labelText");
        if (value.isBlank()) {
            throw new IllegalArgumentException("labelText must not be blank");
        }
        return value;
    }
}
