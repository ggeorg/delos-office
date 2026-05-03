package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterCanvasBadgeContractTest {
    @Test
    void canvasBadgeReplacesBottomStatusBarAndOwnsStatisticsPopover() throws IOException {
        String mainWindow = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterMainWindow.java"));
        String badge = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterCanvasBadge.java"));
        String provider = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterCommandProvider.java"));

        assertTrue(mainWindow.contains("private final WriterCanvasBadge canvasBadge"));
        assertTrue(mainWindow.contains("rootStack = new StackPane(documentView, canvasBadge)"));
        assertTrue(mainWindow.contains("StackPane.setAlignment(canvasBadge, Pos.BOTTOM_CENTER)"));
        assertTrue(mainWindow.contains("StackPane.setMargin(canvasBadge, new Insets(0, 0, 20, 0))"));
        assertTrue(mainWindow.contains("this::showStatisticsPopover"));
        assertTrue(mainWindow.contains("canvasBadge.showStatisticsPopover()"));
        assertFalse(mainWindow.contains("documentShell.setBottom(statusBar)"));
        assertFalse(mainWindow.contains("new StatusBar()"));
        assertFalse(mainWindow.contains("showWordCountDialog"));

        assertTrue(badge.contains("Words"));
        assertTrue(badge.contains("Characters"));
        assertTrue(badge.contains("Paragraphs"));
        assertTrue(badge.contains("Reading time"));
        assertTrue(badge.contains("toggleStatisticsPopover"));
        assertTrue(badge.contains("setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE)"));
        assertTrue(provider.contains("register(\"tools.wordCount\", \"Word Count\", \"Tools\", null, showStatisticsPopover)"));
    }
}
