package io.github.ggeorg.delos.calc.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CalcStatusBarContractTest {
    @Test
    void statusBarShowsSpreadsheetStateRatherThanOwningEditingBehavior() throws IOException {
        String source = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/calc/app/CalcStatusBar.java"));

        assertTrue(source.contains("Cell "));
        assertTrue(source.contains("usedCellCount()"));
        assertTrue(source.contains("Modified"));
        assertTrue(source.contains("Saved"));
        assertTrue(source.contains("status-bar"));
    }
}
