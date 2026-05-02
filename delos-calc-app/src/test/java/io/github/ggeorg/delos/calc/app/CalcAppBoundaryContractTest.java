package io.github.ggeorg.delos.calc.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalcAppBoundaryContractTest {
    @Test
    void calcAppDoesNotDependOnWriterModules() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));
        String moduleInfo = Files.readString(Path.of("src/main/java/module-info.java"));

        assertTrue(pom.contains("delos-calc-javafx"), "calc app should host the Calc JavaFX component");
        assertFalse(pom.contains("delos-writer-core"), "calc app must not depend on Writer core");
        assertFalse(pom.contains("delos-writer-javafx"), "calc app must not depend on Writer JavaFX");
        assertFalse(moduleInfo.contains("delos.writer"), "calc app module must not require Writer modules");
    }

    @Test
    void calcAppHasItsOwnMainClass() throws IOException {
        String source = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/calc/app/DelosCalcApp.java"));
        assertTrue(source.contains("extends Application"));
        assertTrue(source.contains("CalcMainWindow"));
    }
}
