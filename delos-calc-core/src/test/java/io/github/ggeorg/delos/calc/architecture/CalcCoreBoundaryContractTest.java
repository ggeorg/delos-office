package io.github.ggeorg.delos.calc.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalcCoreBoundaryContractTest {
    @Test
    void calcCoreDoesNotDependOnJavaFxWriterLayoutOrRendering() throws IOException {
        Path root = Path.of("src/main/java");
        List<Path> sources;
        try (var stream = Files.walk(root)) {
            sources = stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }

        for (Path source : sources) {
            String text = Files.readString(source);
            assertTrue(
                    !text.contains("javafx.")
                            && !text.contains("io.github.ggeorg.delos.writer.")
                            && !text.contains("io.github.ggeorg.delos.render."),
                    () -> "Calc core must stay pure, but found forbidden dependency in " + source
            );
        }
    }
}
