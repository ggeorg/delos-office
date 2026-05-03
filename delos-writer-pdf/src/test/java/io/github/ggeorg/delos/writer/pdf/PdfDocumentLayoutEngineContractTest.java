package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LaidOutLine;
import io.github.ggeorg.delos.writer.layout.LaidOutTextBlock;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PdfDocumentLayoutEngineContractTest {
    @Test
    void exposesPdfCompatibleLayoutEngineForPreviewExportAndPrint() throws IOException {
        Document document = new Document("Parity", PageStyle.a4Default(), List.of(
                Paragraph.of("The desktop preview must use the same line-breaking assumptions as PDF export.")
        ));
        LayoutTheme theme = PdfExportOptions.defaultOptions().layoutTheme();

        LaidOutDocument direct = new PdfDocumentLayoutEngine().layout(document, theme);
        LaidOutDocument service = new WriterPdfService().layout(document, theme);

        assertEquals(service.pageStyle(), direct.pageStyle());
        assertEquals(service.pages().size(), direct.pages().size());
        LaidOutTextBlock serviceBlock = assertInstanceOf(LaidOutTextBlock.class, service.pages().getFirst().blocks().getFirst());
        LaidOutTextBlock directBlock = assertInstanceOf(LaidOutTextBlock.class, direct.pages().getFirst().blocks().getFirst());
        assertEquals(serviceBlock.lines().size(), directBlock.lines().size());
        assertFalse(directBlock.lines().isEmpty());
        for (int i = 0; i < serviceBlock.lines().size(); i++) {
            LaidOutLine expected = serviceBlock.lines().get(i);
            LaidOutLine actual = directBlock.lines().get(i);
            assertEquals(expected.text(), actual.text());
            assertEquals(expected.width(), actual.width(), 0.001);
            assertEquals(expected.y(), actual.y(), 0.001);
            assertEquals(expected.baseline(), actual.baseline(), 0.001);
        }
    }
}
