package io.github.ggeorg.delos.writer.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterLayoutBoundaryContractTest {
    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    @Test
    void layoutPackageDoesNotImportJavaFxOrWriterUiOrWriterRender() throws IOException {
        List<Path> offenders = javaFiles()
                .stream()
                .filter(path -> importsForbiddenPackage(path))
                .toList();

        assertTrue(offenders.isEmpty(), () -> "Layout module leaked into JavaFX/UI/render imports: " + offenders);
    }

    @Test
    void layoutThemeDoesNotOwnViewChromeSpacingOrPageShadow() throws IOException {
        List<Path> offenders = javaFiles()
                .stream()
                .filter(path -> containsViewOnlyThemeTerm(path))
                .toList();

        assertTrue(offenders.isEmpty(), () -> "Layout module owns view-only theme values: " + offenders);
    }


    @Test
    void reusableHyphenationEngineLivesOutsideWriterLayout() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));
        String moduleInfo = Files.readString(Path.of("src/main/java/module-info.java"));

        assertTrue(pom.contains("delos-hyphenation"),
                "Writer layout should depend on the reusable Delos hyphenation module.");
        assertTrue(moduleInfo.contains("requires transitive io.github.ggeorg.delos.hyphenation;"),
                "Writer layout should read the public hyphenation module, not own the engine.");
        assertFalse(Files.exists(MAIN_SOURCES.resolve("io/github/ggeorg/delos/writer/layout/LiangHyphenator.java")),
                "Liang implementation belongs in delos-hyphenation, not writer layout.");
        assertFalse(Files.exists(MAIN_SOURCES.resolve("io/github/ggeorg/delos/writer/layout/DefaultHyphenators.java")),
                "Default hyphenator factories belong in delos-hyphenation, not writer layout.");
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
        return read(path)
                .lines()
                .map(String::trim)
                .filter(line -> line.startsWith("import "))
                .anyMatch(line -> line.startsWith("import javafx.")
                        || line.startsWith("import io.github.ggeorg.delos.writer.ui.")
                        || line.startsWith("import io.github.ggeorg.delos.writer.render."));
    }

    private static boolean containsViewOnlyThemeTerm(Path path) {
        String source = withoutComments(read(path));
        return source.contains("outerPadding")
                || source.contains("interPageGap")
                || source.contains("pageShadow")
                || source.contains("shadowExtent")
                || source.contains("pageCornerRadius")
                || source.contains("workspaceBackground");
    }

    private static String withoutComments(String source) {
        return source
                .replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll("(?m)//.*$", "");
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }
    }
}
