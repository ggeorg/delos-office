package io.github.ggeorg.delos.calc.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CalcSharedUiContractTest {
    @Test
    void calcUsesSharedDelosUiHelpersButDoesNotDependOnIkonliDirectly() throws IOException {
        String toolbar = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/calc/app/CalcToolBar.java"));
        String app = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/calc/app/DelosCalcApp.java"));

        assertTrue(toolbar.contains("DelosToolBars"));
        assertTrue(toolbar.contains("DelosIconId"));
        assertTrue(app.contains("DelosStylesheets.addTo(scene)"));
        assertFalse(toolbar.contains("org.kordamp.ikonli"));
    }
}
