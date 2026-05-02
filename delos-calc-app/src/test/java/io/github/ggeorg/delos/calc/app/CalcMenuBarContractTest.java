package io.github.ggeorg.delos.calc.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CalcMenuBarContractTest {
    @Test
    void menuBarUsesCommandBackedTraditionalSpreadsheetMenus() throws IOException {
        String source = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/calc/app/CalcMenuBar.java"));

        assertTrue(source.contains("setUseSystemMenuBar(true)"));
        assertTrue(source.contains("fileMenu()"));
        assertTrue(source.contains("editMenu()"));
        assertTrue(source.contains("insertMenu()"));
        assertTrue(source.contains("formatMenu()"));
        assertTrue(source.contains("dataMenu()"));
        assertTrue(source.contains("toolsMenu()"));
        assertTrue(source.contains("item(\"file.print\")"));
        assertTrue(source.contains("item(\"edit.clearContents\")"));
        assertTrue(source.contains("item(\"insert.function\")"));
        assertTrue(source.contains("item(\"format.cells\")"));
        assertTrue(source.contains("item(\"data.sort\")"));
        assertTrue(source.contains("item(\"tools.recalculate\")"));
    }
}
