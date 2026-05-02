package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Golden-style smoke contracts for the server-side PDF production path.
 *
 * <p>These tests intentionally start from the pure Writer {@link Document}
 * model and use {@link WriterPdfService}. They do not involve DelosEditor,
 * PageView, JavaFX snapshots, or desktop printing.</p>
 */
class PdfProductionQualityContractTest {
    @Test
    void exportsReportTextAsExtractablePdfTextInHeadlessMode() throws IOException {
        String previousHeadless = System.getProperty("java.awt.headless");
        System.setProperty("java.awt.headless", "true");
        try {
            WriterPdfService service = new WriterPdfService();
            Document document = new Document("Invoice Report", PageStyle.a4Default(), List.of(
                    Paragraph.of("Invoice #INV-1001"),
                    Paragraph.of("Customer: Acme Research"),
                    Paragraph.of("Service period: April 2026"),
                    Paragraph.of("TOTAL 1234.56")
            ));

            byte[] pdf = export(service, document);

            try (PDDocument loaded = Loader.loadPDF(pdf)) {
                String text = new PDFTextStripper().getText(loaded);
                assertTrue(text.contains("Invoice #INV-1001"), text);
                assertTrue(text.contains("Customer: Acme Research"), text);
                assertTrue(text.contains("TOTAL 1234.56"), text);
            }
        } finally {
            if (previousHeadless == null) {
                System.clearProperty("java.awt.headless");
            } else {
                System.setProperty("java.awt.headless", previousHeadless);
            }
        }
    }

    @Test
    void preservesConfiguredPageSizeAcrossMultiPageReport() throws IOException {
        WriterPdfService service = new WriterPdfService();
        PageStyle pageStyle = PageStyle.a4Default();
        Document document = new Document("Long Report", pageStyle, longReportParagraphs());

        byte[] pdf = export(service, document);

        try (PDDocument loaded = Loader.loadPDF(pdf)) {
            assertTrue(loaded.getNumberOfPages() >= 2, "fixture should exercise multi-page PDF production");
            for (PDPage page : loaded.getPages()) {
                PDRectangle mediaBox = page.getMediaBox();
                assertEquals(pageStyle.width(), mediaBox.getWidth(), 0.01);
                assertEquals(pageStyle.height(), mediaBox.getHeight(), 0.01);
            }
        }
    }

    private static byte[] export(WriterPdfService service, Document document) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        service.export(document, output);
        return output.toByteArray();
    }

    private static List<Paragraph> longReportParagraphs() {
        List<Paragraph> paragraphs = new ArrayList<>();
        paragraphs.add(Paragraph.of("Delos Report Server Golden Fixture"));
        for (int i = 1; i <= 90; i++) {
            paragraphs.add(Paragraph.of("Line " + i + ": server-side PDF production must stay stable, headless, and printable."));
        }
        return paragraphs;
    }
}
