package io.github.ggeorg.delos.calc.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CalcJavaFxBoundaryContractTest {
    private static final Path MAIN_SOURCES = Path.of("src/main/java");
    private static final Path MODULE_INFO = MAIN_SOURCES.resolve("module-info.java");

    private static final Pattern REQUIRES_TRANSITIVE_CALC_CORE = Pattern.compile(
            "requires\\s+transitive\\s+io\\.github\\.ggeorg\\.delos\\.calc\\.core\\s*;"
    );

    @Test
    void calcJavaFxDependsOnCalcCoreButNotWriterOrApp() throws IOException {
        List<Path> offenders = javaFiles().stream()
                .filter(CalcJavaFxBoundaryContractTest::importsWriterOrApp)
                .toList();

        assertTrue(offenders.isEmpty(), () -> "calc-javafx must stay reusable and must not depend on Writer or app shell: " + offenders);
    }

    @Test
    void calcJavaFxModuleRequiresCalcCoreTransitively() throws IOException {
        String moduleInfo = Files.readString(MODULE_INFO);
        assertTrue(REQUIRES_TRANSITIVE_CALC_CORE.matcher(moduleInfo).find(),
                "delos-calc-javafx exposes calc-core types through DelosSpreadsheet, so calc-core must be required transitively");
    }

    private static List<Path> javaFiles() throws IOException {
        try (var paths = Files.walk(MAIN_SOURCES)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }

    private static boolean importsWriterOrApp(Path path) {
        String source = read(path);
        return source.contains("io.github.ggeorg.delos.writer.")
                || source.contains("io.github.ggeorg.delos.app.");
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }
    }
}
