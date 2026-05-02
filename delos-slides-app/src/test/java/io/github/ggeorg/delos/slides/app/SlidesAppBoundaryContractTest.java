package io.github.ggeorg.delos.slides.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SlidesAppBoundaryContractTest {
    @Test
    void slidesAppUsesOfficialSlidesNaming() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));
        String moduleInfo = Files.readString(Path.of("src/main/java/module-info.java"));

        assertTrue(pom.contains("delos-slides-javafx"));
        assertTrue(moduleInfo.contains("delos.slides"));
        assertFalse(pom.contains("deck-app"));
        assertFalse(moduleInfo.contains("deck.app"));
    }

    @Test
    void slidesAppHasWriterLikeChromeClasses() throws IOException {
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/slides/app/SlidesMainWindow.java")));
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/slides/app/SlidesMenuBar.java")));
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/slides/app/SlidesToolBar.java")));
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/slides/app/SlidesStatusBar.java")));
    }
}
