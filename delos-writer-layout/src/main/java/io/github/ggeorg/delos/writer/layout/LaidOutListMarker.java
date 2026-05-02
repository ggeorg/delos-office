package io.github.ggeorg.delos.writer.layout;

import java.util.Objects;

/**
 * Visual marker for a paragraph-owned list item.
 */
public record LaidOutListMarker(
        String text,
        double x,
        double y,
        double width
) {
    public LaidOutListMarker {
        text = Objects.requireNonNullElse(text, "");
        if (width < 0) {
            throw new IllegalArgumentException("width must be >= 0");
        }
    }

    public static LaidOutListMarker none() {
        return new LaidOutListMarker("", 0, 0, 0);
    }

    public boolean visible() {
        return !text.isBlank();
    }
}
