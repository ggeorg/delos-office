package io.github.ggeorg.delos.writer.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterJavaFxModuleExportsContractTest {
    @Test
    void writerJavaFxExportsOnlyTheTemporaryWriterUiSurface() throws IOException {
        String moduleInfo = Files.readString(Path.of("src/main/java/module-info.java"));

        assertFalse(moduleInfo.contains("exports io.github.ggeorg.delos.writer.render;"),
                "neutral render package should not be exported from writer-javafx");
        assertFalse(moduleInfo.contains("exports io.github.ggeorg.delos.writer.render.fx;"),
                "JavaFX render adapter package should stay internal to writer-javafx");
        assertTrue(moduleInfo.contains("exports io.github.ggeorg.delos.writer.ui.control;"),
                "DelosEditor remains the public control entry point");
    }
}
