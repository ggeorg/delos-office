package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterPrintResponsivenessContractTest {
    private static final Path FILE_CONTROLLER = Path.of(
            "src/main/java/io/github/ggeorg/delos/writer/app/WriterFileController.java"
    );

    @Test
    void printRunsBlockingPdfAndPrinterWorkOffTheJavaFxApplicationThreadUsingFrozenPreviewLayout() throws IOException {
        String source = Files.readString(FILE_CONTROLLER);

        assertTrue(source.contains("private static final ExecutorService PRINT_EXECUTOR"),
                "Print needs a dedicated background executor because PDF creation, print dialogs, and spooling can block.");
        assertTrue(source.contains("PRINT_EXECUTOR.execute(() ->"),
                "The blocking print service must not be called directly from the JavaFX command handler.");
        assertTrue(source.contains("WriterLayoutSnapshot printSnapshot;") && source.contains("printSnapshot = editor.createLayoutSnapshot()"),
                "Desktop print must freeze the same layout the user sees before moving work off the JavaFX thread.");
        assertTrue(source.contains("printService.print(printSnapshot.document(), printSnapshot.layout(), options)"),
                "Desktop print must render the frozen preview layout through the same PDF-first print service.");
        assertTrue(source.contains("Platform.runLater"),
                "Background print failures and command-state refresh must return to the JavaFX thread.");
        assertTrue(source.contains("AtomicBoolean printing"),
                "Print should reject duplicate concurrent print jobs from repeated button presses.");
    }
}
