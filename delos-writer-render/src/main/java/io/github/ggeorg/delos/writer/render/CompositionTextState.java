package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.writer.layout.CaretGeometry;

import java.util.Objects;

/**
 * Temporary IME composition text painted by the editor overlay.
 * <p>
 * This is intentionally not document content. When an input method commits
 * text, the JavaFX input controller converts it into a normal edit command;
 * until then, the composed text lives only in render state.
 */
public record CompositionTextState(
        String text,
        int pageIndex,
        CaretGeometry caret
) {
    public static final CompositionTextState EMPTY = new CompositionTextState("", -1, null);

    public CompositionTextState(String text, CaretGeometry caret) {
        this(text, -1, caret);
    }

    public CompositionTextState {
        text = Objects.requireNonNullElse(text, "");
    }

    public boolean isEmpty() {
        return text.isEmpty() || pageIndex < 0 || caret == null;
    }
}
