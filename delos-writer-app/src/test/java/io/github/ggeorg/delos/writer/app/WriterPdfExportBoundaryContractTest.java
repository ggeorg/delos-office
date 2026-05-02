package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterPdfExportBoundaryContractTest {
    @Test
    void writerAppKeepsHeadlessPdfServiceAsTheCanonicalExportPath() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));
        String moduleInfo = Files.readString(Path.of("src/main/java/module-info.java"));
        String fileController = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterFileController.java"));
        String commandProvider = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterCommandProvider.java"));

        assertTrue(pom.contains("delos-writer-pdf"), "Writer app should depend on the PDF backend for Export PDF");
        assertTrue(moduleInfo.contains("requires io.github.ggeorg.delos.writer.pdf;"));
        assertTrue(fileController.contains("WriterPdfService"));
        assertTrue(fileController.contains("pdfService.export(session.document(), normalizedTarget)"),
                "Desktop export must use the same headless Document -> PDF path as server-side report generation.");
        assertFalse(fileController.contains("WriterLayoutSnapshot"),
                "JavaFX editor snapshots must not become the production PDF/export boundary.");
        assertFalse(fileController.contains("createLayoutSnapshot"),
                "The Writer UI preview should chase the headless PDF engine, not become the PDF engine.");
        assertFalse(fileController.contains("PdfWriterExporter"),
                "Writer app should delegate through WriterPdfService, not construct the low-level PDF exporter");
        assertTrue(commandProvider.contains("fileController::exportPdf"));
    }
}
