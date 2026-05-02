package io.github.ggeorg.delos.javafx.chrome;

import io.github.ggeorg.delos.javafx.icon.DelosIconId;
import javafx.scene.Node;

import java.util.Objects;

/** Describes one right-side inspector tab such as Document, Format, or Arrange. */
public record InspectorTab(String id, String title, DelosIconId iconId, Node content) {
    public InspectorTab {
        id = requireText(id, "id");
        title = requireText(title, "title");
        content = Objects.requireNonNull(content, "content");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
