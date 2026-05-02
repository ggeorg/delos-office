package io.github.ggeorg.delos.javafx.chrome;

import java.util.Objects;

/** One option in a small inspector segmented control. */
public record SegmentedOption(String id, String title) {
    public SegmentedOption {
        id = requireText(id, "id");
        title = requireText(title, "title");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
