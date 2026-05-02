package io.github.ggeorg.delos.base.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BaseAppBoundaryContractTest {
    @Test
    void baseAppUsesOfficialBaseNaming() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));
        String moduleInfo = Files.readString(Path.of("src/main/java/module-info.java"));

        assertTrue(pom.contains("delos-base-javafx"));
        assertTrue(moduleInfo.contains("delos.base"));
        assertFalse(pom.contains("database-app"));
        assertFalse(moduleInfo.contains("database.app"));
    }

    @Test
    void baseAppHasWriterLikeChromeClasses() throws IOException {
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/base/app/BaseMainWindow.java")));
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/base/app/BaseMenuBar.java")));
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/base/app/BaseToolBar.java")));
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/base/app/BaseStatusBar.java")));
    }
}
