package io.github.ggeorg.delos.writer.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterCoreBoundaryContractTest {
    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    @Test
    void writerCoreDoesNotImportJavaFx() throws IOException {
        List<Path> offenders = javaFilesUnder("io/github/ggeorg/delos/writer/document", "io/github/ggeorg/delos/writer/session", "io/github/ggeorg/delos/writer/editor", "io/github/ggeorg/delos/writer/io")
                .stream()
                .filter(WriterCoreBoundaryContractTest::importsJavaFx)
                .toList();

        assertTrue(offenders.isEmpty(), () -> "JavaFX leaked into writer-core: " + offenders);
    }

    @Test
    void writerCoreDoesNotDependOnWriterLayoutRenderOrUi() throws IOException {
        List<Path> offenders = javaFilesUnder("io/github/ggeorg/delos/writer/document", "io/github/ggeorg/delos/writer/session", "io/github/ggeorg/delos/writer/editor", "io/github/ggeorg/delos/writer/io")
                .stream()
                .filter(WriterCoreBoundaryContractTest::importsWriterPresentationPackage)
                .toList();

        assertTrue(offenders.isEmpty(), () -> "writer-core depends on layout/render/ui: " + offenders);
    }

    private static List<Path> javaFilesUnder(String... packages) throws IOException {
        try (var paths = Files.walk(MAIN_SOURCES)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> isUnderAny(path, packages))
                    .toList();
        }
    }

    private static boolean isUnderAny(Path path, String[] packages) {
        String normalized = path.toString().replace('\\', '/');
        for (String pkg : packages) {
            if (normalized.contains(pkg)) {
                return true;
            }
        }
        return false;
    }

    private static boolean importsJavaFx(Path path) {
        return contains(path, "import javafx.");
    }

    private static boolean importsWriterPresentationPackage(Path path) {
        String source = read(path);
        return source.contains("io.github.ggeorg.delos.writer.layout")
                || source.contains("io.github.ggeorg.delos.writer.render")
                || source.contains("io.github.ggeorg.delos.writer.ui");
    }

    private static boolean contains(Path path, String needle) {
        return read(path).contains(needle);
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }
    }
}
