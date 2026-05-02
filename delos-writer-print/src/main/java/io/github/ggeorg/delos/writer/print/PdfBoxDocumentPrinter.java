package io.github.ggeorg.delos.writer.print;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * PDFBox-backed implementation that prints a frozen PDF artifact.
 */
public final class PdfBoxDocumentPrinter implements PdfDocumentPrinter {
    @Override
    public void print(Path pdf, PdfPrintOptions options) throws IOException, PrinterException {
        Objects.requireNonNull(pdf, "pdf");
        PdfPrintOptions safeOptions = options == null ? PdfPrintOptions.defaultOptions() : options;
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName(safeOptions.jobName());
            job.setPageable(new PDFPageable(document));

            if (!safeOptions.showPrintDialog() || job.printDialog()) {
                job.print();
            }
        }
    }
}
