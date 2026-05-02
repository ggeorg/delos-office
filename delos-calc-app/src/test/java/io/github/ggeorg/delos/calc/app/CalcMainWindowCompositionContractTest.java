package io.github.ggeorg.delos.calc.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CalcMainWindowCompositionContractTest {
    @Test
    void mainWindowUsesTraditionalSpreadsheetChromeAndCommandRegistry() throws IOException {
        String source = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/calc/app/CalcMainWindow.java"));

        assertTrue(source.contains("CommandRegistry"));
        assertTrue(source.contains("CalcCommandProvider"));
        assertTrue(source.contains("CalcMenuBar"));
        assertTrue(source.contains("CalcToolBar"));
        assertTrue(source.contains("CalcStatusBar"));
        assertTrue(source.contains("new VBox(menuBar, toolBar)"));
        assertTrue(source.contains("setTop(topChrome)"));
        assertTrue(source.contains("setCenter(spreadsheet)"));
        assertTrue(source.contains("setBottom(statusBar)"));
        assertTrue(source.contains("installAccelerators"));
        assertFalse(source.contains("new MenuItem"), "menu construction belongs in CalcMenuBar");
        assertFalse(source.contains("new MenuBar"), "menu construction belongs in CalcMenuBar");
        assertFalse(source.contains("private Menu"), "menus should not be inline in CalcMainWindow");
    }

    @Test
    void extractedCalcChromeClassesExist() {
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/calc/app/CalcCommandProvider.java")));
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/calc/app/CalcMenuBar.java")));
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/calc/app/CalcToolBar.java")));
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/calc/app/CalcStatusBar.java")));
    }
}
