package io.github.ggeorg.delos.writer.print;

import java.awt.print.PrinterException;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Sends an already-generated PDF artifact to a printer.
 */
@FunctionalInterface
public interface PdfDocumentPrinter {
    void print(Path pdf, PdfPrintOptions options) throws IOException, PrinterException;
}
