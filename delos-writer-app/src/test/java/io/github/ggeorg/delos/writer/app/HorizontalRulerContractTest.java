package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HorizontalRulerContractTest {
    @Test
    void rulerImplementationBelongsToWriterJavaFxButIsNotPublicApi() throws IOException {
        String moduleInfo = Files.readString(Path.of("../delos-writer-javafx/src/main/java/module-info.java"));
        String writerMain = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterMainWindow.java"));

        assertFalse(moduleInfo.contains("exports io.github.ggeorg.delos.writer.ui.ruler"),
                "rulers are implementation details of WriterDocumentView, not third-party API");
        assertFalse(writerMain.contains("HorizontalRuler"),
                "Writer app should consume WriterDocumentView instead of wiring ruler internals");
        assertTrue(Files.exists(Path.of("../delos-writer-javafx/src/main/java/io/github/ggeorg/delos/writer/ui/ruler/HorizontalRuler.java")));
        assertTrue(Files.exists(Path.of("../delos-writer-javafx/src/main/java/io/github/ggeorg/delos/writer/ui/ruler/VerticalRuler.java")));
        assertTrue(Files.exists(Path.of("../delos-writer-javafx/src/main/java/io/github/ggeorg/delos/writer/ui/ruler/CornerRulerCell.java")));
    }
}
