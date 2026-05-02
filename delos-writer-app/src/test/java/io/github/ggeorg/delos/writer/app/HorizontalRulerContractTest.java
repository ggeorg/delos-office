package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HorizontalRulerContractTest {
    @Test
    void appShellNoLongerOwnsRulerImplementation() throws IOException {
        String mainWindow = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterMainWindow.java"));
        String moduleInfo = Files.readString(Path.of("../delos-writer-javafx/src/main/java/module-info.java"));

        assertTrue(moduleInfo.contains("exports io.github.ggeorg.delos.writer.ui.ruler"));
        assertFalse(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/HorizontalRuler.java")),
                "ruler implementation belongs in delos-writer-javafx, not delos-writer-app");
        assertTrue(Files.exists(Path.of("../delos-writer-javafx/src/main/java/io/github/ggeorg/delos/writer/ui/ruler/HorizontalRuler.java")));
        assertTrue(Files.exists(Path.of("../delos-writer-javafx/src/main/java/io/github/ggeorg/delos/writer/ui/ruler/VerticalRuler.java")));
        assertTrue(Files.exists(Path.of("../delos-writer-javafx/src/main/java/io/github/ggeorg/delos/writer/ui/ruler/CornerRulerCell.java")));
        assertFalse(mainWindow.contains("HorizontalRuler horizontalRuler"));
        assertFalse(mainWindow.contains("horizontalRuler.visibleContentXProperty()"));
        assertFalse(mainWindow.contains("horizontalRuler.viewportWidthProperty()"));
    }
}
