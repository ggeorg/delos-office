package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.RenderTarget;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.render.PageRenderContext;
import io.github.ggeorg.delos.writer.render.PageRenderer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTextSanitizerTest {
    @Test
    void normalizesCompatibilitySpacingWithoutChangingVisibleTypography() {
        assertEquals(
                "Screenshot 2026-04-28 at 1.30.06 PM.png — ‘quote’ “quote” …",
                PdfTextSanitizer.sanitize("Screenshot 2026-04-28 at 1.30.06\u202FPM.png — ‘quote’ “quote” …")
        );
        assertEquals("Καλημέρα", PdfTextSanitizer.sanitize("Καλημέρα"));
    }

    @Test
    void removesInvisibleFormattingCharactersOnly() {
        assertEquals("softzero", PdfTextSanitizer.sanitize("soft\u00ADzero\u200B"));
    }

    @Test
    void pdfExportDoesNotFailForNarrowNoBreakSpaceInText() throws IOException {
        PageRenderer renderer = new PageRenderer() {
            @Override
            public void renderPage(RenderTarget target, PageRenderContext context) {
                target.setFont(new RenderFont("Helvetica", 12.0, false, false));
                target.fillText("Screenshot 2026-04-28 at 1.30.06\u202FPM.png", 40.0, 40.0);
            }
        };
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertDoesNotThrow(() -> new PdfWriterExporter(renderer, new PdfFontRegistry(List.of()), PdfExportOptions.defaultOptions())
                .export(singleEmptyPage(), output));

        assertTrue(output.toString("ISO-8859-1").startsWith("%PDF-"));
    }

    private static LaidOutDocument singleEmptyPage() {
        PageStyle pageStyle = PageStyle.a4Default();
        return new LaidOutDocument(
                pageStyle,
                List.of(new LaidOutPage(0, pageStyle.width(), pageStyle.height(), new ArrayList<>()))
        );
    }
}
