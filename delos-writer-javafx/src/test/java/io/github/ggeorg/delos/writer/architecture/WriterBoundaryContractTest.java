package io.github.ggeorg.delos.writer.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterBoundaryContractTest {
    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    @Test
    void writerJavaFxModuleDoesNotContainExtractedLayoutPackage() {
        Path extractedLayoutPackage = MAIN_SOURCES.resolve("io/github/ggeorg/delos/writer/layout");

        assertFalse(Files.exists(extractedLayoutPackage),
                () -> "Layout sources belong in delos-writer-layout, not delos-writer-javafx: " + extractedLayoutPackage);
    }

    @Test
    void writerJavaFxModuleDoesNotDependOnTheApplicationShell() throws IOException {
        List<Path> offenders = javaFiles()
                .stream()
                .filter(path -> contains(path, "io.github.ggeorg.delos.app"))
                .toList();

        assertTrue(offenders.isEmpty(), () -> "writer-javafx must stay reusable and must not depend on delos-app: " + offenders);
    }

    @Test
    void neutralRenderPackageDoesNotImportJavaFx() throws IOException {
        List<Path> offenders = javaFilesUnder("io/github/ggeorg/delos/writer/render")
                .stream()
                .filter(path -> !path.toString().replace('\\', '/').contains("/render/fx/"))
                .filter(WriterBoundaryContractTest::importsJavaFx)
                .toList();

        assertTrue(offenders.isEmpty(), () -> "JavaFX leaked into neutral render package: " + offenders);
    }

    private static List<Path> javaFiles() throws IOException {
        try (var paths = Files.walk(MAIN_SOURCES)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }

    private static List<Path> javaFilesUnder(String packagePath) throws IOException {
        try (var paths = Files.walk(MAIN_SOURCES)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().replace('\\', '/').contains(packagePath))
                    .toList();
        }
    }

    private static boolean importsJavaFx(Path path) {
        return contains(path, "import javafx.");
    }

    private static boolean contains(Path path, String needle) {
        try {
            return Files.readString(path).contains(needle);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }
    }
}
