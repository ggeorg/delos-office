package io.github.ggeorg.delos.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TextLayoutResultContractTest {
    @Test
    void preservesExplicitDecorationMetrics() {
        RenderFont font = new RenderFont("System", 18.0, false, false);
        TextDecorationMetrics metrics = new TextDecorationMetrics(1.5, 6.0, 0.8);

        TextLayoutResult layout = new TextLayoutResult("abc", font, 30.0, 20.0, 14.0, List.of(0.0, 10.0, 20.0, 30.0), metrics);

        assertEquals(metrics, layout.decorations());
        assertEquals(30.0, layout.endCaretStop(), 0.001);
    }

    @Test
    void oldConstructorStillSuppliesFontRelativeDecorationMetrics() {
        RenderFont font = new RenderFont("System", 18.0, false, false);

        TextLayoutResult layout = new TextLayoutResult("abc", font, 30.0, 20.0, 14.0, List.of(0.0, 10.0, 20.0, 30.0));

        assertTrue(layout.decorations().underlineOffset() > 0.0);
        assertTrue(layout.decorations().strikethroughOffset() > 0.0);
        assertTrue(layout.decorations().thickness() > 0.0);
    }
}
