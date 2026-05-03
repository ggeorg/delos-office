package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterChromeAndZoomContractTest {
    @Test
    void chromeRefreshOwnsStatusCommandChromeAndStageTitle() throws IOException {
        String source = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterChromeController.java"));

        assertTrue(source.contains("canvasBadge.update(editor.currentPageNumber(), editor.totalPageCount(), session.document())"));
                assertTrue(source.contains("menuBar.refreshFromCommands()"));
        assertTrue(source.contains("toolBar.refreshFromCommands()"));
        assertTrue(source.contains("session.isDirty() ? \" *\" : \"\""));
        assertTrue(source.contains("+ \" — Delos Writer\""));
    }

    @Test
    void documentViewZoomControllerOwnsScrollAndZoomMath() throws IOException {
        String source = Files.readString(Path.of("../delos-writer-javafx/src/main/java/io/github/ggeorg/delos/writer/ui/control/WriterDocumentViewZoomController.java"));

        assertTrue(source.contains("ZoomMath.clampZoom"));
        assertTrue(source.contains("zoomScale.setX(z)"));
        assertTrue(source.contains("zoomScale.setY(z)"));
        assertTrue(source.contains("zoomHost.setZoomFactor(z)"));
        assertTrue(source.contains("ScrollIntoViewCoordinator.adjustedOffset"));
        assertTrue(source.contains("documentView.updateVisibleViewport"));
    }
}
