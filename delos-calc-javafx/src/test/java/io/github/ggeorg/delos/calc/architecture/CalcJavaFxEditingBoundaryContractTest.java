package io.github.ggeorg.delos.calc.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CalcJavaFxEditingBoundaryContractTest {
    @Test
    void spreadsheetRoutesEditsThroughCoreWorkbookEditor() throws IOException {
        String source = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/calc/ui/control/DelosSpreadsheet.java"));

        assertTrue(source.contains("WorkbookEditor.edit(current)"),
                "DelosSpreadsheet should use the pure Calc editing facade for cell edits");
        assertFalse(source.contains("current.firstSheet().withInput"),
                "JavaFX control must not hard-wire edits to the first sheet");
    }

    @Test
    void spreadsheetExposesActiveSheetAndSelectionSeams() throws IOException {
        String source = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/calc/ui/control/DelosSpreadsheet.java"));

        assertTrue(source.contains("activeSheetNameProperty"));
        assertTrue(source.contains("selectionProperty"));
        assertTrue(source.contains("commitInput"));
    }
}
