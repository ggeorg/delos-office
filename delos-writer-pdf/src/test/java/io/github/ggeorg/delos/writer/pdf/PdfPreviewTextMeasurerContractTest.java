package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.render.RenderFont;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfPreviewTextMeasurerContractTest {
    @Test
    void usesPdfFontMappingForPreviewMeasurementAndRenderingHelpers() throws Exception {
        try (PdfPreviewTextMeasurer measurer = new PdfPreviewTextMeasurer()) {
            RenderFont body = new RenderFont("Serif", 13.5, false, false);
            RenderFont styled = measurer.styledFont(body, true, true);

            assertEquals("Times", styled.family());
            assertTrue(measurer.textWidth("Delos Writer", body) > 0.0);
            assertTrue(measurer.lineHeight(body) > 0.0);
            assertTrue(measurer.baseline(body) > 0.0);
        }
    }

    @Test
    void rejectsUseAfterCloseSoPreviewResourceLeaksAreVisibleDuringDevelopment() throws Exception {
        PdfPreviewTextMeasurer measurer = new PdfPreviewTextMeasurer();
        measurer.close();

        assertThrows(IllegalStateException.class,
                () -> measurer.textWidth("Delos", new RenderFont("Serif", 13.5, false, false)));
    }
}
