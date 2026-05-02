package io.github.ggeorg.delos.calc.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CalcToolBarContractTest {
    @Test
    void toolbarBindsFileEditAndFormulaChromeThroughCommands() throws IOException {
        String source = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/calc/app/CalcToolBar.java"));

        assertTrue(source.contains("button(\"file.new\""));
        assertTrue(source.contains("button(\"file.open\""));
        assertTrue(source.contains("button(\"file.save\""));
        assertTrue(source.contains("button(\"file.print\""));
        assertTrue(source.contains("button(\"edit.cut\""));
        assertTrue(source.contains("button(\"edit.copy\""));
        assertTrue(source.contains("button(\"edit.paste\""));
        assertTrue(source.contains("button(\"edit.clearContents\""));
        assertTrue(source.contains("formulaButton()"));
        assertTrue(source.contains("formulaField()"));
        assertTrue(source.contains("numberFormatPicker()"));
        assertTrue(source.contains("DelosToolBars.button(commandRegistry"));
    }
}
