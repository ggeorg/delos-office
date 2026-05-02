package io.github.ggeorg.delos.writer.pdf;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfModuleBoundaryTest {
    @Test
    void pdfModuleStaysHeadlessAndDesktopPrintLivesElsewhere() throws IOException {
        String moduleInfo = Files.readString(Path.of("src/main/java/module-info.java"));
        String packageInfo = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/pdf/package-info.java"));

        assertFalse(moduleInfo.contains("requires java.desktop"));
        assertTrue(packageInfo.contains("delos-writer-print"));
    }
}
