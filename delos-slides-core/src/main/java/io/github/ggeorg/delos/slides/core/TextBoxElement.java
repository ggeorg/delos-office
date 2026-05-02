package io.github.ggeorg.delos.slides.core;

import java.util.Objects;

/**
 * Immutable text box positioned in slide coordinates.
 */
public record TextBoxElement(
        String id,
        double x,
        double y,
        double width,
        double height,
        String text
) implements SlideElement {
    public TextBoxElement {
        id = requireText(id, "id");
        requireFinite(x, "x");
        requireFinite(y, "y");
        width = requirePositive(width, "width");
        height = requirePositive(height, "height");
        text = Objects.requireNonNullElse(text, "");
    }

    public static TextBoxElement title(String text) {
        return new TextBoxElement("title", 80, 64, 800, 96, text);
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static double requirePositive(double value, String name) {
        requireFinite(value, name);
        if (value <= 0.0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
