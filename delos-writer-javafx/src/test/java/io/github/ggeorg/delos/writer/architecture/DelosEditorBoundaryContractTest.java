package io.github.ggeorg.delos.writer.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DelosEditorBoundaryContractTest {

    @Test
    void writerAppDoesNotReachDirectlyForDocumentViewport() throws IOException {
        Path appMain = Path.of("../delos-writer-app/src/main/java").normalize();
        if (!Files.isDirectory(appMain)) {
            return;
        }

        List<Path> offenders;
        try (var stream = Files.walk(appMain)) {
            offenders = stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> contains(path, "io.github.ggeorg.delos.writer.ui.DocumentViewport"))
                    .toList();
        }

        assertTrue(offenders.isEmpty(), "Writer app must use DelosEditor, not DocumentViewport: " + offenders);
    }

    private static boolean contains(Path path, String token) {
        try {
            return Files.readString(path).contains(token);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
