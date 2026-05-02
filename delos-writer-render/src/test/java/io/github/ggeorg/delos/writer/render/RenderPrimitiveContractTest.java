package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RenderColor;
import io.github.ggeorg.delos.render.RenderFont;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RenderPrimitiveContractTest {

    @Test
    void renderColorNormalizesRgbAndClampsInvalidChannels() {
        RenderColor color = RenderColor.rgb(255, 128, 0);

        assertEquals(1.0, color.red());
        assertEquals(128.0 / 255.0, color.green());
        assertEquals(0.0, color.blue());
        assertEquals(1.0, color.alpha());

        RenderColor clamped = new RenderColor(Double.NaN, -1.0, 2.0, 0.5);
        assertEquals(0.0, clamped.red());
        assertEquals(0.0, clamped.green());
        assertEquals(1.0, clamped.blue());
        assertEquals(0.5, clamped.alpha());
    }

    @Test
    void renderFontFallsBackToSafeDefaultsAndPreservesStyleChanges() {
        RenderFont fallback = new RenderFont(" ", -4.0, false, false);

        assertEquals("System", fallback.family());
        assertEquals(12.0, fallback.size());

        RenderFont styled = fallback.withStyle(true, true);
        assertEquals("System", styled.family());
        assertEquals(12.0, styled.size());
        assertTrue(styled.bold());
        assertTrue(styled.italic());
    }
}
