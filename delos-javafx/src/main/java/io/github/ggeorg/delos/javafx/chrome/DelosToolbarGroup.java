package io.github.ggeorg.delos.javafx.chrome;

import javafx.scene.Node;
import javafx.scene.layout.HBox;

import java.util.Arrays;
import java.util.Objects;

/**
 * Small shared toolbar grouping widget for Delos desktop chrome.
 * <p>
 * It intentionally works for one child as well as many children. That lets apps
 * give a single icon button, a menu button, or a combo box the same Pages-like
 * grouped chrome as a multi-button cluster without duplicating CSS rules.
 */
public final class DelosToolbarGroup extends HBox {
    public DelosToolbarGroup(Node... children) {
        super(0.0);
        getStyleClass().add("delos-toolbar-group");
        Arrays.stream(Objects.requireNonNull(children, "children"))
                .forEach(child -> getChildren().add(Objects.requireNonNull(child, "child")));
    }
}
