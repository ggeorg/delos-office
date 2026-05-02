package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WriterAppBoundaryContractTest {
    @Test
    void writerAppDoesNotDependOnCalcModules() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));
        String moduleInfo = Files.readString(Path.of("src/main/java/module-info.java"));

        assertTrue(pom.contains("delos-writer-javafx"), "writer app should host the Writer JavaFX component");
        assertFalse(pom.contains("delos-calc-core"), "writer app must not depend on Calc core");
        assertFalse(pom.contains("delos-calc-javafx"), "writer app must not depend on Calc JavaFX");
        assertFalse(moduleInfo.contains("delos.calc"), "writer app module must not require Calc modules");
    }

    @Test
    void writerAppHasItsOwnMainClass() throws IOException {
        String source = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/DelosWriterApp.java"));
        assertTrue(source.contains("extends Application"));
        assertTrue(source.contains("WriterMainWindow"));
    }
}
