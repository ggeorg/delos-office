package io.github.ggeorg.delos.slides.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SlidesJavaFxBoundaryContractTest {
    @Test
    void javafxModuleDoesNotContainApplicationClasses() throws Exception {
        Path sourceRoot = Path.of("src/main/java");
        assertTrue(Files.exists(sourceRoot));
        try (var paths = Files.walk(sourceRoot)) {
            assertFalse(paths
                    .filter(Files::isRegularFile)
                    .anyMatch(path -> path.getFileName().toString().endsWith("App.java")));
        }
    }
}
