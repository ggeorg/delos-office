package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LaidOutTextBlock;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * v117.10 guardrails for Delos-native WYSIWYG: the PDF/server path must honor
 * document geometry and derive its PDF fonts from the shared layout theme.
 */
class PdfWysiwygStabilizationContractTest {
    @Test
    void defaultPdfThemeDerivesFromSharedLayoutThemeInsteadOfForcingBodyHelvetica() {
        LayoutTheme defaultLayout = LayoutTheme.defaultTheme();
        PdfExportOptions options = PdfExportOptions.defaultOptions();

        assertEquals("System", defaultLayout.titleFont().family());
        assertEquals("Serif", defaultLayout.bodyFont().family());
        assertEquals("Helvetica", options.layoutTheme().titleFont().family());
        assertEquals("Times", options.layoutTheme().bodyFont().family());
        assertEquals(options.layoutTheme().titleFont(), options.renderTheme().titleFont());
        assertEquals(options.layoutTheme().bodyFont(), options.renderTheme().bodyFont());
    }

    @Test
    void pdfLayoutKeepsDocumentPageStyleAsGeometrySourceOfTruth() throws IOException {
        PageStyle pageStyle = new PageStyle(612.0, 792.0, 54.0, 72.0, 63.0, 81.0);
        Document document = new Document("Geometry", pageStyle, List.of(
                Paragraph.of("Delos PDF must use document page settings, not private exporter defaults.")
        ));

        LaidOutDocument layout = new WriterPdfService().layout(document);

        assertEquals(pageStyle, layout.pageStyle());
        assertFalse(layout.pages().isEmpty());
        assertEquals(pageStyle.width(), layout.pages().getFirst().width(), 0.001);
        assertEquals(pageStyle.height(), layout.pages().getFirst().height(), 0.001);
        LaidOutTextBlock block = assertInstanceOf(LaidOutTextBlock.class, layout.pages().getFirst().blocks().getFirst());
        assertEquals(pageStyle.marginLeft(), block.x(), 0.001);
        assertEquals(pageStyle.marginTop(), block.y(), 0.001);
        assertEquals(pageStyle.contentWidth(), block.width(), 0.001);
    }
}
