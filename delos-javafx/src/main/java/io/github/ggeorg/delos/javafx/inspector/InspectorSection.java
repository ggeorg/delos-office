package io.github.ggeorg.delos.javafx.inspector;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;

/** A small titled group used inside a Delos right-side inspector. */
public final class InspectorSection extends VBox {
    private final VBox body = new VBox(8);

    public InspectorSection(String title) {
        Objects.requireNonNull(title, "title");
        getStyleClass().add("delos-inspector-section");
        setSpacing(8);
        setPadding(new Insets(0, 0, 14, 0));

        Label heading = new Label(title);
        heading.getStyleClass().add("delos-inspector-section-title");
        body.getStyleClass().add("delos-inspector-section-body");
        getChildren().setAll(heading, body);
    }

    public void add(Node node) {
        body.getChildren().add(Objects.requireNonNull(node, "node"));
    }

    public void addAll(Node... nodes) {
        for (Node node : nodes) {
            add(node);
        }
    }

    public static HBox row(String labelText, Node control) {
        Objects.requireNonNull(labelText, "labelText");
        Objects.requireNonNull(control, "control");

        Label label = new Label(labelText);
        label.getStyleClass().add("delos-inspector-form-label");
        label.setMinWidth(86);

        HBox row = new HBox(8, label, control);
        row.getStyleClass().add("delos-inspector-form-row");
        HBox.setHgrow(control, Priority.ALWAYS);
        return row;
    }
}
