package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterFormulaInsertionContractTest {
    private static final Path INSERT_CONTROLLER = Path.of(
            "src/main/java/io/github/ggeorg/delos/writer/app/WriterInsertController.java"
    );

    @Test
    void formulaInsertionUsesLatexSourceDialogAndEditorApi() throws IOException {
        String source = Files.readString(INSERT_CONTROLLER);

        assertTrue(source.contains("void insertFormula()"));
        assertTrue(source.contains("chooseFormulaForInsert()"));
        assertTrue(source.contains("chooseFormula(\"Insert Formula\", \"Enter a LaTeX formula\""));
        assertTrue(source.contains("dialog.setHeaderText(headerText)"));
        assertTrue(source.contains("new FormulaInput(FormulaSourceFormat.LATEX"));
        assertTrue(source.contains("editor.insertFormula(input.sourceFormat(), input.source(), input.altText())"));
    }
}
