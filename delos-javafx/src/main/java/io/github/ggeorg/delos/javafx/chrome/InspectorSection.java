package io.github.ggeorg.delos.javafx.chrome;

import javafx.scene.Node;
import javafx.scene.control.TitledPane;

import java.util.Objects;

/** Collapsible section used inside Delos inspector tabs. */
public class InspectorSection extends TitledPane {
    public InspectorSection(String title, Node content) {
        super(requireText(title), Objects.requireNonNull(content, "content"));
        getStyleClass().add("delos-inspector-section");
        setAnimated(false);
        setCollapsible(true);
        setExpanded(true);
    }

    private static String requireText(String value) {
        Objects.requireNonNull(value, "title");
        if (value.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        return value;
    }
}
