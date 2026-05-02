package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.writer.document.Block;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.DocumentMediaItem;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WriterPdfServiceTest {
    private static final byte[] ONE_PIXEL_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
    );

    @Test
    void laysOutWriterDocumentWithoutJavaFxEditorSnapshot() throws IOException {
        WriterPdfService service = new WriterPdfService();
        Document document = new Document("Headless", PageStyle.a4Default(), List.of(Paragraph.of("server report")));

        LaidOutDocument layout = service.layout(document);

        assertFalse(layout.pages().isEmpty());
        assertEquals("Helvetica", PdfExportOptions.defaultOptions().layoutTheme().bodyFont().family());
        assertEquals(PdfExportOptions.defaultOptions().layoutTheme().bodyFont(), PdfExportOptions.defaultOptions().renderTheme().bodyFont());
    }

    @Test
    void exportsPdfFromDocumentModel() throws IOException {
        WriterPdfService service = new WriterPdfService();
        Document document = new Document("Headless", PageStyle.a4Default(), List.of(Paragraph.of("PDF service")));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        service.export(document, output);

        String pdf = output.toString("ISO-8859-1");
        assertTrue(pdf.startsWith("%PDF-"));
        assertTrue(pdf.contains("%%EOF"));
    }

    @Test
    void embedsRasterImageMediaFromDocumentModel() throws IOException {
        WriterPdfService service = new WriterPdfService();
        Document document = Document.fromBlocks(
                "Image PDF",
                PageStyle.a4Default(),
                List.<Block>of(new ImageBlock("media/image-1.png", 64.0, 64.0, "one pixel")),
                List.of(DocumentMediaItem.image("media/image-1.png", "image/png", ONE_PIXEL_PNG))
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        service.export(document, output);

        assertTrue(pdfContainsImageXObject(output.toByteArray()));
    }

    private static boolean pdfContainsImageXObject(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            for (COSName name : document.getPage(0).getResources().getXObjectNames()) {
                PDXObject object = document.getPage(0).getResources().getXObject(name);
                if (object instanceof PDImageXObject) {
                    return true;
                }
            }
            return false;
        }
    }
}
