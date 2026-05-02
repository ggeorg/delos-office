package io.github.ggeorg.delos.writer.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class PageDecorationTest {
    @Test
    void normalizesInvalidGeometryAndDefaultsKindAndLayer() {
        PageDecoration decoration = new PageDecoration(null, null, Double.NaN, Double.POSITIVE_INFINITY, -3.0, -4.0);

        assertEquals(DecorationKind.SEARCH_HIGHLIGHT, decoration.kind());
        assertEquals(DecorationLayer.BEHIND_TEXT, decoration.layer());
        assertEquals(0.0, decoration.x());
        assertEquals(0.0, decoration.y());
        assertEquals(0.0, decoration.width());
        assertEquals(0.0, decoration.height());
    }

    @Test
    void renderStateDefensivelyCopiesDecorationList() {
        PageDecoration decoration = PageDecoration.highlight(DecorationKind.SEARCH_HIGHLIGHT, 1.0, 2.0, 3.0, 4.0);
        PageRenderState state = PageRenderState.EMPTY.withDecorations(List.of(decoration));

        assertFalse(state.decorations().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> state.decorations().add(decoration));
    }
}
