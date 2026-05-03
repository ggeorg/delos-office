package io.github.ggeorg.delos.writer.print;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.pdf.PdfWriterExporter;
import io.github.ggeorg.delos.writer.pdf.WriterPdfService;

import java.awt.print.PrinterException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * PDF-first print pipeline for Writer.
 *
 * <p>The service never prints {@code DelosEditor}, {@code PageView}, or JavaFX
 * nodes. It first exports the Writer document through the same headless PDF service used by server-side generation, then submits that frozen PDF artifact to the printer. This keeps printing aligned
 * with PDF export and gives Delos a clean diagnostic boundary: if the PDF is
 * correct, layout/export are correct and remaining issues belong to printer or
 * driver behavior.</p>
 */
public final class PdfWriterPrintService {
    private static final String TEMP_PREFIX = "delos-writer-print-";
    private static final String TEMP_SUFFIX = ".pdf";

    private final PdfWriterExporter exporter;
    private final WriterPdfService pdfService;
    private final PdfDocumentPrinter printer;

    public PdfWriterPrintService() {
        this(new PdfWriterExporter(), new WriterPdfService(), new PdfBoxDocumentPrinter());
    }

    public PdfWriterPrintService(PdfWriterExporter exporter, PdfDocumentPrinter printer) {
        this(exporter, new WriterPdfService(), printer);
    }

    public PdfWriterPrintService(WriterPdfService pdfService, PdfDocumentPrinter printer) {
        this(new PdfWriterExporter(), pdfService, printer);
    }

    public PdfWriterPrintService(PdfWriterExporter exporter, WriterPdfService pdfService, PdfDocumentPrinter printer) {
        this.exporter = Objects.requireNonNull(exporter, "exporter");
        this.pdfService = Objects.requireNonNull(pdfService, "pdfService");
        this.printer = Objects.requireNonNull(printer, "printer");
    }

    public void print(Document document) throws IOException, PrinterException {
        print(document, PdfPrintOptions.defaultOptions());
    }

    public void print(Document document, PdfPrintOptions options) throws IOException, PrinterException {
        Objects.requireNonNull(document, "document");
        PdfPrintOptions safeOptions = options == null ? PdfPrintOptions.defaultOptions() : options;
        printTemporaryPdf(safeOptions, pdf -> pdfService.export(document, pdf));
    }

    public void print(Document document, LaidOutDocument layout, PdfPrintOptions options) throws IOException, PrinterException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(layout, "layout");
        PdfPrintOptions safeOptions = options == null ? PdfPrintOptions.defaultOptions() : options;
        printTemporaryPdf(safeOptions, pdf -> pdfService.exportLayout(document, layout, pdf));
    }

    public void print(LaidOutDocument document) throws IOException, PrinterException {
        print(document, PdfPrintOptions.defaultOptions());
    }

    public void print(LaidOutDocument document, PdfPrintOptions options) throws IOException, PrinterException {
        Objects.requireNonNull(document, "document");
        PdfPrintOptions safeOptions = options == null ? PdfPrintOptions.defaultOptions() : options;
        printTemporaryPdf(safeOptions, pdf -> exporter.export(document, pdf));
    }

    private void printTemporaryPdf(PdfPrintOptions options, PdfExportOperation exportOperation) throws IOException, PrinterException {
        Path pdf = createTemporaryPdf(options);
        if (options.deleteTemporaryPdf()) {
            pdf.toFile().deleteOnExit();
        }
        try {
            exportOperation.export(pdf);
            printer.print(pdf, options);
        } finally {
            if (options.deleteTemporaryPdf()) {
                Files.deleteIfExists(pdf);
            }
        }
    }

    private static Path createTemporaryPdf(PdfPrintOptions options) throws IOException {
        Path directory = options.temporaryDirectory();
        if (directory == null) {
            return Files.createTempFile(TEMP_PREFIX, TEMP_SUFFIX);
        }
        Files.createDirectories(directory);
        return Files.createTempFile(directory, TEMP_PREFIX, TEMP_SUFFIX);
    }

    @FunctionalInterface
    private interface PdfExportOperation {
        void export(Path pdf) throws IOException;
    }
}
