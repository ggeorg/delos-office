package io.github.ggeorg.delos.writer.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterJavaFxCalcBoundaryContractTest {
    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    @Test
    void writerJavaFxModuleDoesNotDependOnCalcModules() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));
        String moduleInfo = Files.readString(Path.of("src/main/java/module-info.java"));

        assertFalse(pom.contains("delos-calc"), "writer-javafx must not depend on Calc artifacts");
        assertFalse(moduleInfo.contains("delos.calc"), "writer-javafx module must not require Calc modules");
    }

    @Test
    void writerJavaFxSourcesDoNotImportCalcPackages() throws IOException {
        List<Path> offenders = javaFiles()
                .stream()
                .filter(path -> read(path).contains("io.github.ggeorg.delos.calc"))
                .toList();

        assertTrue(offenders.isEmpty(), () -> "writer-javafx imports Calc packages: " + offenders);
    }

    private static List<Path> javaFiles() throws IOException {
        try (var paths = Files.walk(MAIN_SOURCES)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }
    }
}
