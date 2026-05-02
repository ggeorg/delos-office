package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.layout.CaretGeometry;

import java.util.List;

/**
 * View-state overlay for page rendering.
 */
public record PageRenderState(
        CaretGeometry caret,
        SelectionRange selection,
        boolean caretVisible,
        CompositionTextState composition,
        List<PageDecoration> decorations
) {
    public static final PageRenderState EMPTY = new PageRenderState(
            null,
            null,
            false,
            CompositionTextState.EMPTY,
            List.of()
    );

    public PageRenderState(CaretGeometry caret, SelectionRange selection, boolean caretVisible) {
        this(caret, selection, caretVisible, CompositionTextState.EMPTY, List.of());
    }

    public PageRenderState(CaretGeometry caret,
                           SelectionRange selection,
                           boolean caretVisible,
                           CompositionTextState composition) {
        this(caret, selection, caretVisible, composition, List.of());
    }

    public PageRenderState {
        composition = composition == null ? CompositionTextState.EMPTY : composition;
        decorations = decorations == null ? List.of() : List.copyOf(decorations);
    }

    public boolean hasDecorations() {
        return !decorations.isEmpty();
    }

    public PageRenderState withDecorations(List<PageDecoration> newDecorations) {
        return new PageRenderState(caret, selection, caretVisible, composition, newDecorations);
    }
}
