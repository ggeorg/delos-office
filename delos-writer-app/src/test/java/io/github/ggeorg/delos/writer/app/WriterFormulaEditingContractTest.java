package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterFormulaEditingContractTest {
    private static final Path INSERT_CONTROLLER = Path.of(
            "src/main/java/io/github/ggeorg/delos/writer/app/WriterInsertController.java"
    );
    private static final Path COMMAND_PROVIDER = Path.of(
            "src/main/java/io/github/ggeorg/delos/writer/app/WriterCommandProvider.java"
    );

    @Test
    void formulaEditDialogReusesFormulaInputAndPrefillsSelectedFormula() throws IOException {
        String source = Files.readString(INSERT_CONTROLLER);

        assertTrue(source.contains("void editSelectedFormula()"));
        assertTrue(source.contains("FormulaBlock selected = editor.selectedFormulaBlock()"));
        assertTrue(source.contains("dialog.setTitle(title)"));
        assertTrue(source.contains("new TextArea(initial == null ? \"\" : initial.source())"));
        assertTrue(source.contains("new TextField(initial == null ? \"\" : initial.altText())"));
        assertTrue(source.contains("editor.updateSelectedFormula(input.sourceFormat(), input.source(), input.altText())"));
    }

    @Test
    void editFormulaCommandIsEnabledOnlyForSelectedFormulaBlocks() throws IOException {
        String source = Files.readString(COMMAND_PROVIDER);

        assertTrue(source.contains("register(\"edit.formula\", \"Edit Formula…\", \"Edit\""));
        assertTrue(source.contains("insertController::editSelectedFormula"));
        assertTrue(source.contains("editor::hasSelectedFormulaBlock"));
    }
}
