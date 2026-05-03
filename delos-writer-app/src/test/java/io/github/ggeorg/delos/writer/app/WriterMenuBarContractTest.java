package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterMenuBarContractTest {
    private static final Path MENU_BAR = Path.of(
            "src/main/java/io/github/ggeorg/delos/writer/app/WriterMenuBar.java"
    );

    @Test
    void menuBarUsesCommandBackedTraditionalWordProcessorMenus() throws IOException {
        String source = Files.readString(MENU_BAR);

        assertTrue(source.contains("DelosMenus.configure"));
        assertTrue(source.contains("exportMenu()"));
        assertTrue(source.contains("textMenu()"));
        assertTrue(source.contains("paragraphMenu()"));
        assertTrue(source.contains("item(\"file.print\")"));
        assertTrue(source.contains("item(\"edit.find\")"));
        assertTrue(source.contains("item(\"insert.pageBreak\")"));
        assertTrue(source.contains("item(\"format.bulletedList\")"));
        assertTrue(source.contains("item(\"format.lineSpacing\")"));
        assertTrue(source.contains("item(\"tools.wordCount\")"));
        assertTrue(source.contains("item(\"app.preferences\")"));
    }
}
