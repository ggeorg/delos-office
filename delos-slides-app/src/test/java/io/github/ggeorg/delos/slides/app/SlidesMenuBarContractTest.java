package io.github.ggeorg.delos.slides.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SlidesMenuBarContractTest {
    @Test
    void menuBarUsesTraditionalPresentationMenus() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/io/github/ggeorg/delos/slides/app/SlidesMenuBar.java"
        ));

        assertTrue(source.contains("setUseSystemMenuBar(true)"));
        assertTrue(source.contains("slideshowMenu()"));
        assertTrue(source.contains("item(\"insert.slide\")"));
        assertTrue(source.contains("item(\"insert.textBox\")"));
        assertTrue(source.contains("item(\"format.slideLayout\")"));
        assertTrue(source.contains("item(\"format.theme\")"));
        assertTrue(source.contains("item(\"file.exportPdf\")"));
    }
}
