package io.github.ggeorg.delos.slides.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable presentation deck aggregate.
 */
public record PresentationDeck(String title, List<Slide> slides) {
    public PresentationDeck {
        title = normalizeTitle(title);
        slides = List.copyOf(Objects.requireNonNull(slides, "slides"));
        if (slides.isEmpty()) {
            throw new IllegalArgumentException("Presentation deck must contain at least one slide");
        }
        slides.forEach(slide -> Objects.requireNonNull(slide, "slide"));
    }

    public static PresentationDeck blank() {
        return new PresentationDeck("Untitled", List.of(Slide.blank("Title Slide")));
    }

    public Slide firstSlide() {
        return slides.get(0);
    }

    public Slide slideAt(int index) {
        if (index < 0 || index >= slides.size()) {
            throw new IndexOutOfBoundsException("Slide index out of range: " + index);
        }
        return slides.get(index);
    }

    public PresentationDeck withTitle(String title) {
        return new PresentationDeck(title, slides);
    }

    public PresentationDeck withSlide(int index, Slide replacement) {
        Objects.requireNonNull(replacement, "replacement");
        ArrayList<Slide> updated = new ArrayList<>(slides);
        updated.set(index, replacement);
        return new PresentationDeck(title, updated);
    }

    public PresentationDeck addSlide(Slide slide) {
        Objects.requireNonNull(slide, "slide");
        ArrayList<Slide> updated = new ArrayList<>(slides);
        updated.add(slide);
        return new PresentationDeck(title, updated);
    }

    private static String normalizeTitle(String value) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        return normalized.isEmpty() ? "Untitled" : normalized;
    }
}
