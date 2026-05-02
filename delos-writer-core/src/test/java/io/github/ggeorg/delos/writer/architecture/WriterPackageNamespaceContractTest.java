package io.github.ggeorg.delos.writer.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterPackageNamespaceContractTest {
    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    @Test
    void writerCoreDoesNotOwnGenericDelosCorePackages() throws IOException {
        List<Path> offenders;
        try (var paths = Files.walk(MAIN_SOURCES)) {
            offenders = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().replace('\\', '/').contains("/io/github/ggeorg/delos/core/"))
                    .toList();
        }

        assertTrue(offenders.isEmpty(),
                () -> "writer-core must not use io.github.ggeorg.delos.core.* packages: " + offenders);
    }
}
