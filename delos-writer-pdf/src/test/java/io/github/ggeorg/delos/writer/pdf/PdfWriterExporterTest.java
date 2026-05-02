package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.render.RenderTarget;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.render.PageRenderContext;
import io.github.ggeorg.delos.writer.render.PageRenderDestination;
import io.github.ggeorg.delos.writer.render.PageRenderer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfWriterExporterTest {
    @Test
    void exportsPdfHeaderAndTrailerForEmptyPage() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        new PdfWriterExporter().export(singleEmptyPage(), output);

        String pdf = output.toString("ISO-8859-1");
        assertTrue(pdf.startsWith("%PDF-"));
        assertTrue(pdf.contains("%%EOF"));
    }

    @Test
    void rendersEveryPageWithPdfExportDestination() throws IOException {
        AtomicReference<PageRenderContext> captured = new AtomicReference<>();
        PageRenderer renderer = (RenderTarget target, PageRenderContext context) -> captured.set(context);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        new PdfWriterExporter(renderer, new PdfFontRegistry(), PdfExportOptions.defaultOptions())
                .export(singleEmptyPage(), output);

        PageRenderContext context = captured.get();
        assertEquals(PageRenderDestination.PDF_EXPORT, context.destination());
        assertFalse(context.drawPageChrome());
        assertFalse(context.drawSelection());
        assertFalse(context.drawCaret());
    }

    private static LaidOutDocument singleEmptyPage() {
        PageStyle pageStyle = PageStyle.a4Default();
        return new LaidOutDocument(
                pageStyle,
                List.of(new LaidOutPage(0, pageStyle.width(), pageStyle.height(), new ArrayList<>()))
        );
    }
}
