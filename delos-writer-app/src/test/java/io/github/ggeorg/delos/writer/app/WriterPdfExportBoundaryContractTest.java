package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterPdfExportBoundaryContractTest {
    @Test
    void writerAppExportsTheFrozenPreviewLayoutThroughTheHeadlessPdfBackend() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));
        String moduleInfo = Files.readString(Path.of("src/main/java/module-info.java"));
        String fileController = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterFileController.java"));
        String commandProvider = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterCommandProvider.java"));
        String mainWindow = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterMainWindow.java"));
        String outputPreview = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterOutputPreview.java"));

        assertTrue(pom.contains("delos-writer-pdf"), "Writer app should depend on the PDF backend for Export PDF");
        assertTrue(moduleInfo.contains("requires io.github.ggeorg.delos.writer.pdf;"));
        assertTrue(fileController.contains("WriterPdfService"));
        assertTrue(fileController.contains("WriterLayoutSnapshot snapshot = editor.createLayoutSnapshot()"),
                "Desktop WYSIWYG export must freeze the exact layout currently shown in the editor.");
        assertTrue(fileController.contains("pdfService.exportLayout(snapshot.document(), snapshot.layout(), normalizedTarget)"),
                "The frozen preview layout must still be rendered by the headless PDF backend.");
        assertFalse(fileController.contains("new PdfWriterExporter"),
                "Writer app should delegate through WriterPdfService, not construct the low-level PDF exporter");
        assertTrue(commandProvider.contains("fileController::exportPdf"));
        assertTrue(mainWindow.contains("WriterOutputPreview.createDefault()"));
        assertTrue(mainWindow.contains("outputPreview.createDocumentView(session)"));
        assertTrue(outputPreview.contains("DesktopPreviewLayoutPolicy.defaultPolicy()"),
                "Desktop preview quality should be governed by an explicit preview policy.");
        assertTrue(outputPreview.contains("previewFactory.createDocumentView(session, previewPolicy)"),
                "Desktop preview construction should be delegated to delos-writer-javafx.");
        assertFalse(outputPreview.contains("PdfExportOptions"),
                "Desktop preview quality must not depend on PDF export options.");
        assertFalse(outputPreview.contains("io.github.ggeorg.delos.writer.render.fx"),
                "Writer app must not import non-exported JavaFX renderer implementation packages.");
        assertFalse(outputPreview.contains("new PdfPreviewTextMeasurer()"),
                "The live editor preview should not force PDFBox render-time metrics and degrade on-screen text quality.");
        assertFalse(mainWindow.contains("new WriterDesktopPreviewFactory()"),
                "WriterMainWindow should not scatter output-preview construction details.");
    }
}
