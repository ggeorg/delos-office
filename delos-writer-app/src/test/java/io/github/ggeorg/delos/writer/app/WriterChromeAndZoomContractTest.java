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

        assertTrue(source.contains("statusBar.setWordCount(StatusBar.countWords(session.document()))"));
        assertTrue(source.contains("statusBar.setPageInfo(editor.currentPageNumber(), editor.totalPageCount())"));
        assertTrue(source.contains("statusBar.setZoomFactor(editor.zoom())"));
        assertTrue(source.contains("menuBar.refreshFromCommands()"));
        assertTrue(source.contains("toolBar.refreshFromCommands()"));
        assertTrue(source.contains("session.isDirty() ? \" *\" : \"\""));
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
