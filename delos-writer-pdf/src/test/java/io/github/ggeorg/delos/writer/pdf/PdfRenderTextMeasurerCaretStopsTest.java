package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.render.RenderFont;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfRenderTextMeasurerCaretStopsTest {
    private static final RenderFont FONT = new RenderFont("Helvetica", 12.0, false, false);

    @Test
    void caretStopsUseOnePdfFontLookupInsteadOfMeasuringEveryPrefix() {
        CountingFontResolver fonts = new CountingFontResolver();
        PdfRenderTextMeasurer measurer = new PdfRenderTextMeasurer(fonts);
        String text = "The quick brown fox jumps over the lazy dog. ".repeat(80);

        List<Double> stops = measurer.caretStops(text, FONT);

        assertEquals(text.length() + 1, stops.size());
        assertEquals(1, fonts.textFontLookups);
        assertMonotonic(stops);
        assertEquals(measurer.textWidth(text, FONT), stops.getLast(), 0.001);
    }

    @Test
    void caretStopsPreserveSourceOffsetsForSanitizedInvisibleCharacters() {
        PdfRenderTextMeasurer measurer = new PdfRenderTextMeasurer(new CountingFontResolver());
        String text = "soft\u00ADzero\u200B";

        List<Double> stops = measurer.caretStops(text, FONT);

        assertEquals(text.length() + 1, stops.size());
        assertMonotonic(stops);
        assertEquals(measurer.textWidth("softzero", FONT), stops.getLast(), 0.001);
        assertEquals(stops.get(4), stops.get(5), 0.001, "soft hyphen must not advance the caret");
        assertEquals(stops.get(text.length() - 1), stops.get(text.length()), 0.001, "zero-width space must not advance the caret");
    }

    @Test
    void layoutTextUsesTheSameOptimizedCaretStopsContract() {
        PdfRenderTextMeasurer measurer = new PdfRenderTextMeasurer(new CountingFontResolver());
        String text = "PDF caret stops stay in sync with measured width.";

        var layout = measurer.layoutText(text, FONT);

        assertEquals(text.length() + 1, layout.caretStops().size());
        assertEquals(layout.width(), layout.endCaretStop(), 0.001);
        assertMonotonic(layout.caretStops());
    }

    private static void assertMonotonic(List<Double> stops) {
        for (int i = 1; i < stops.size(); i++) {
            assertTrue(stops.get(i) + 0.001 >= stops.get(i - 1), "caret stops must be monotonic at index " + i);
        }
    }

    private static final class CountingFontResolver implements PdfFontResolver {
        private final PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private int textFontLookups;

        @Override
        public RenderFont resolve(RenderFont font) {
            return font;
        }

        @Override
        public PDFont fontFor(RenderFont font) {
            return this.font;
        }

        @Override
        public PDFont fontFor(RenderFont font, String text) {
            textFontLookups++;
            return this.font;
        }
    }
}
