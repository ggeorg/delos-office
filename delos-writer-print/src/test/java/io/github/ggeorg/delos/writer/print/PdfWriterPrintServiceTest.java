package io.github.ggeorg.delos.writer.print;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.pdf.PdfWriterExporter;
import io.github.ggeorg.delos.writer.pdf.WriterPdfService;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.print.PrinterException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfWriterPrintServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void printsFrozenTemporaryPdfArtifactAndDeletesItByDefault() throws Exception {
        CapturingPrinter printer = new CapturingPrinter();
        PdfWriterPrintService service = new PdfWriterPrintService(new PdfWriterExporter(), printer);

        service.print(singleEmptyPage(), PdfPrintOptions.defaultOptions().withTemporaryDirectory(tempDir));

        assertEquals("Delos Writer Document", printer.jobName.get());
        Path printedPdf = printer.pdf.get();
        assertTrue(printedPdf.getFileName().toString().endsWith(".pdf"));
        assertFalse(Files.exists(printedPdf), "temporary print PDF should be deleted by default");
    }

    @Test
    void printsDocumentThroughHeadlessPdfService() throws Exception {
        CapturingPrinter printer = new CapturingPrinter();
        PdfWriterPrintService service = new PdfWriterPrintService(new WriterPdfService(), printer);

        service.print(Document.blank(), PdfPrintOptions.defaultOptions()
                .withTemporaryDirectory(tempDir)
                .withJobName("Headless Document Print")
                .keepingTemporaryPdf());

        Path printedPdf = printer.pdf.get();
        assertEquals("Headless Document Print", printer.jobName.get());
        assertTrue(Files.exists(printedPdf), "debug mode keeps the frozen print PDF");
        assertTrue(Files.size(printedPdf) > 0, "document print artifact should contain exported PDF bytes");
        Files.deleteIfExists(printedPdf);
    }

    @Test
    void canKeepTemporaryPdfForDebugging() throws Exception {
        CapturingPrinter printer = new CapturingPrinter();
        PdfWriterPrintService service = new PdfWriterPrintService(new PdfWriterExporter(), printer);

        service.print(singleEmptyPage(), PdfPrintOptions.defaultOptions()
                .withTemporaryDirectory(tempDir)
                .withJobName("Debug Print")
                .keepingTemporaryPdf());

        Path printedPdf = printer.pdf.get();
        assertEquals("Debug Print", printer.jobName.get());
        assertTrue(Files.exists(printedPdf), "debug mode keeps the frozen print PDF");
        assertTrue(Files.size(printedPdf) > 0, "print artifact should contain exported PDF bytes");
        Files.deleteIfExists(printedPdf);
    }



    @Test
    void passesPrintOptionsToPrinter() throws Exception {
        CapturingPrinter printer = new CapturingPrinter();
        PdfWriterPrintService service = new PdfWriterPrintService(new PdfWriterExporter(), printer);

        service.print(singleEmptyPage(), PdfPrintOptions.defaultOptions()
                .withTemporaryDirectory(tempDir)
                .withoutPrintDialog());

        assertFalse(printer.options.get().showPrintDialog());
    }


    @Test
    void deletesTemporaryPdfEvenWhenPrinterFails() {
        AtomicReference<Path> capturedPdf = new AtomicReference<>();
        PdfDocumentPrinter failingPrinter = (pdf, options) -> {
            capturedPdf.set(pdf);
            assertEquals("Delos Writer Document", options.jobName());
            assertTrue(Files.exists(pdf), "PDF must exist while printer is invoked");
            throw new PrinterException("printer offline");
        };
        PdfWriterPrintService service = new PdfWriterPrintService(new PdfWriterExporter(), failingPrinter);

        assertThrows(PrinterException.class, () -> service.print(singleEmptyPage(),
                PdfPrintOptions.defaultOptions().withTemporaryDirectory(tempDir)));

        assertFalse(Files.exists(capturedPdf.get()), "temporary print PDF should be removed after printer failure");
    }

    private static LaidOutDocument singleEmptyPage() {
        PageStyle pageStyle = PageStyle.a4Default();
        return new LaidOutDocument(
                pageStyle,
                List.of(new LaidOutPage(0, pageStyle.width(), pageStyle.height(), new ArrayList<>()))
        );
    }

    private static final class CapturingPrinter implements PdfDocumentPrinter {
        private final AtomicReference<Path> pdf = new AtomicReference<>();
        private final AtomicReference<String> jobName = new AtomicReference<>();
        private final AtomicReference<PdfPrintOptions> options = new AtomicReference<>();

        @Override
        public void print(Path pdf, PdfPrintOptions options) throws IOException {
            this.pdf.set(pdf);
            this.jobName.set(options.jobName());
            this.options.set(options);
            assertTrue(Files.exists(pdf), "PDF must exist while printer is invoked");
            assertTrue(Files.size(pdf) > 0, "print artifact should contain exported PDF bytes");
        }
    }
}
