package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.TextLayoutResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PdfRenderTextMeasurerTest {
    @Test
    void measuresTextAndCaretStopsWithPdfFonts() {
        PdfRenderTextMeasurer measurer = new PdfRenderTextMeasurer(new PdfFontRegistry());
        RenderFont font = new RenderFont("Helvetica", 12.0, false, false);

        double width = measurer.textWidth("Delos", font);

        assertTrue(width > 0.0);
        assertEquals(6, measurer.caretStops("Delos", font).size());
        assertEquals(width, measurer.caretStops("Delos", font).get(5), 0.0001);
    }

    @Test
    void styledFontReturnsCanonicalPdfFontFamily() {
        PdfRenderTextMeasurer measurer = new PdfRenderTextMeasurer(new PdfFontRegistry());

        RenderFont styled = measurer.styledFont(new RenderFont("Serif", 12.0, false, false), true, true);

        assertEquals(new RenderFont("Times", 12.0, true, true), styled);
    }


    @Test
    void lineBoxMetricsComeFromPdfFontDescriptors() {
        PdfRenderTextMeasurer measurer = new PdfRenderTextMeasurer(new PdfFontRegistry());
        RenderFont helvetica = new RenderFont("Helvetica", 12.0, false, false);
        RenderFont times = new RenderFont("Times", 12.0, false, false);

        double helveticaLineHeight = measurer.lineHeight(helvetica);
        double timesLineHeight = measurer.lineHeight(times);
        double helveticaBaseline = measurer.baseline(helvetica);
        double timesBaseline = measurer.baseline(times);

        assertTrue(helveticaLineHeight > 0.0);
        assertTrue(timesLineHeight > 0.0);
        assertTrue(helveticaBaseline > 0.0 && helveticaBaseline < helveticaLineHeight);
        assertTrue(timesBaseline > 0.0 && timesBaseline < timesLineHeight);
        assertNotEquals(helveticaLineHeight, timesLineHeight, 0.0001);
        assertNotEquals(helveticaBaseline, timesBaseline, 0.0001);
    }

    @Test
    void caretStopsEndAtWholeStringWidthForMixedFallbackTextWhenUnicodeFontIsAvailable() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PdfRenderTextMeasurer measurer = new PdfRenderTextMeasurer(new PdfFontRegistry().forDocument(document));
            RenderFont font = new RenderFont("Helvetica", 12.0, false, false);

            assumeTrue(canMeasure(measurer, "AΩ", font), "No embeddable Unicode fallback font available on this machine");
            double width = measurer.textWidth("AΩ", font);

            assertEquals(3, measurer.caretStops("AΩ", font).size());
            assertEquals(width, measurer.caretStops("AΩ", font).getLast(), 0.0001);
        }
    }

    @Test
    void layoutTextKeepsWidthAndCaretStopsConsistentWhenUnicodeFontIsAvailable() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PdfRenderTextMeasurer measurer = new PdfRenderTextMeasurer(new PdfFontRegistry().forDocument(document));
            RenderFont font = new RenderFont("System", 12.0, false, false);

            assumeTrue(canMeasure(measurer, "AΩ", font), "No embeddable Unicode fallback font available on this machine");
            TextLayoutResult result = measurer.layoutText("AΩ", font);

            assertEquals(result.width(), result.endCaretStop(), 0.0001);
            assertEquals(new RenderFont("Helvetica", 12.0, false, false), result.font());
        }
    }

    @Test
    void unicodeTextRequiresDocumentScopedFontRegistry() {
        PdfRenderTextMeasurer measurer = new PdfRenderTextMeasurer(new PdfFontRegistry());
        RenderFont font = new RenderFont("Helvetica", 12.0, false, false);

        assumeTrue(!canMeasure(measurer, "AΩ", font), "Default platform font already encodes the mixed sample");
    }

    private static boolean canMeasure(PdfRenderTextMeasurer measurer, String text, RenderFont font) {
        try {
            measurer.textWidth(text, font);
            return true;
        } catch (PdfRenderException ex) {
            return false;
        }
    }
}
