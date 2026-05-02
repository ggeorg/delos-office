package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterPrintResponsivenessContractTest {
    private static final Path FILE_CONTROLLER = Path.of(
            "src/main/java/io/github/ggeorg/delos/writer/app/WriterFileController.java"
    );

    @Test
    void printRunsBlockingPdfAndPrinterWorkOffTheJavaFxApplicationThreadButUsesHeadlessPdfPath() throws IOException {
        String source = Files.readString(FILE_CONTROLLER);

        assertTrue(source.contains("private static final ExecutorService PRINT_EXECUTOR"),
                "Print needs a dedicated background executor because PDF creation, print dialogs, and spooling can block.");
        assertTrue(source.contains("PRINT_EXECUTOR.execute(() ->"),
                "The blocking print service must not be called directly from the JavaFX command handler.");
        assertTrue(source.contains("Document printDocument = session.document();"),
                "Print should capture the immutable document model, not a JavaFX layout snapshot.");
        assertTrue(source.contains("printService.print(printDocument, options)"),
                "Desktop print must route through the same headless Document -> PDF print path as server/report generation.");
        assertFalse(source.contains("WriterLayoutSnapshot"));
        assertFalse(source.contains("createLayoutSnapshot"));
        assertTrue(source.contains("Platform.runLater"),
                "Background print failures and command-state refresh must return to the JavaFX thread.");
        assertTrue(source.contains("AtomicBoolean printing"),
                "Print should reject duplicate concurrent print jobs from repeated button presses.");
    }
}
