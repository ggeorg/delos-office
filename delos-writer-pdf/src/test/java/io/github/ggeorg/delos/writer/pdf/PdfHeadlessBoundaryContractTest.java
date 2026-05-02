package io.github.ggeorg.delos.writer.pdf;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the commercial/server path: PDF production must remain headless and
 * must not depend on the desktop editor, JavaFX, or printer APIs.
 */
class PdfHeadlessBoundaryContractTest {
    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    @Test
    void pdfProductionSourcesDoNotImportDesktopOrJavaFxApis() throws IOException {
        List<Path> offenders = javaFiles()
                .stream()
                .filter(PdfHeadlessBoundaryContractTest::importsForbiddenPackage)
                .toList();

        assertTrue(offenders.isEmpty(), () -> "PDF production leaked into desktop/UI APIs: " + offenders);
    }

    @Test
    void pdfModuleDoesNotRequireDesktopOrJavaFxModules() throws IOException {
        String moduleInfo = Files.readString(Path.of("src/main/java/module-info.java"));

        assertFalse(moduleInfo.contains("requires java.desktop"));
        assertFalse(moduleInfo.contains("requires javafx"));
    }

    private static List<Path> javaFiles() throws IOException {
        try (var paths = Files.walk(MAIN_SOURCES)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }

    private static boolean importsForbiddenPackage(Path path) {
        String source = read(path);
        return source.contains("import javafx.")
                || source.contains("import java.awt.")
                || source.contains("import javax.print.")
                || source.contains("import io.github.ggeorg.delos.writer.ui.");
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }
    }
}
