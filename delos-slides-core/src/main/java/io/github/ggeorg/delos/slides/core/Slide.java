package io.github.ggeorg.delos.slides.core;

import java.util.List;
import java.util.Objects;

/**
 * Immutable slide aggregate.
 */
public record Slide(String title, List<SlideElement> elements) {
    public Slide {
        title = normalizeTitle(title);
        elements = List.copyOf(Objects.requireNonNull(elements, "elements"));
    }

    public static Slide blank(String title) {
        return new Slide(title, List.of(TextBoxElement.title(title)));
    }

    public Slide withTitle(String title) {
        return new Slide(title, elements);
    }

    public Slide addElement(SlideElement element) {
        Objects.requireNonNull(element, "element");
        java.util.ArrayList<SlideElement> updated = new java.util.ArrayList<>(elements);
        updated.add(element);
        return new Slide(title, updated);
    }

    public int elementCount() {
        return elements.size();
    }

    private static String normalizeTitle(String value) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        return normalized.isEmpty() ? "Untitled Slide" : normalized;
    }
}
