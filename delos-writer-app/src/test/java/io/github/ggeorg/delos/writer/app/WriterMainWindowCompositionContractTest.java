package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WriterMainWindowCompositionContractTest {
    @Test
    void mainWindowUsesDocumentViewAndKeepsAppChromeOnly() throws IOException {
        String mainWindow = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterMainWindow.java"));

        assertTrue(mainWindow.contains("WriterDocumentView"));
        assertTrue(mainWindow.contains("documentView = outputPreview.createDocumentView(session)"));
        assertTrue(mainWindow.contains("editor = documentView.editor()"));
        assertTrue(mainWindow.contains("WriterCommandProvider"));
        assertTrue(mainWindow.contains("WriterFileController"));
        assertTrue(mainWindow.contains("WriterChromeController"));
        assertTrue(mainWindow.contains("WriterMenuBar"));
        assertTrue(mainWindow.contains("WriterToolBar"));
        assertTrue(mainWindow.contains("menuBar.isUseSystemMenuBar()"));
        assertTrue(mainWindow.contains("appChrome.getChildren().setAll(menuBar, toolBar)"));
        assertTrue(mainWindow.contains("appChrome.getChildren().setAll(toolBar)"));
        assertTrue(mainWindow.contains("fileController::renameDocumentTitle"));
        assertFalse(mainWindow.contains("setTop(null)"));
        assertTrue(mainWindow.contains("rootStack = new StackPane(documentView, canvasBadge)"));
        assertTrue(mainWindow.contains("rootStack.getChildren().add(overlayLayer)"));
        assertTrue(mainWindow.contains("rootStack.getChildren().remove(overlayLayer)"));
        assertTrue(mainWindow.contains("new WriterInspectorPane(session, editor, commandRegistry)"));
        assertTrue(mainWindow.contains("new HBox(documentShell, inspector)"));
        assertFalse(mainWindow.contains("private boolean rulerVisible"), "ruler state belongs to WriterDocumentView");
        assertFalse(mainWindow.contains("setHorizontalRulerVisible"), "app shell should not control individual ruler widgets");
        assertFalse(mainWindow.contains("setVerticalRulerVisible"), "app shell should not control individual ruler widgets");
        assertTrue(mainWindow.contains("showStatisticsPopover"));
        assertFalse(mainWindow.contains("documentShell.setTop(toolBar)"), "toolbar spans above document and inspector in the Pages-like shell");
        assertFalse(mainWindow.contains("documentShell.setBottom(statusBar)"), "UX-2 replaces the bottom status bar with the canvas badge");
        assertTrue(mainWindow.contains("setTop(appChrome)"));
        assertTrue(mainWindow.contains("setCenter(windowShell)"));
        assertFalse(mainWindow.contains("new ScrollPane"), "WriterMainWindow should not wire editor scrolling internals");
        assertFalse(mainWindow.contains("new ZoomViewportHost"), "zoom host belongs inside WriterDocumentView");
        assertFalse(mainWindow.contains("new Group(editor)"), "zoom group belongs inside WriterDocumentView");
        assertFalse(mainWindow.contains("new HorizontalRuler"), "rulers belong inside WriterDocumentView");
        assertFalse(mainWindow.contains("registerCommand(\""), "WriterMainWindow should not own inline command registration");
        assertFalse(mainWindow.contains("new WriterFileService()"), "WriterMainWindow should not own low-level file service wiring");
        assertFalse(mainWindow.contains("setOnZoomPercentChanged"), "zoom is controlled from the toolbar/menu, not the status bar");
    }

    @Test
    void extractedControllersAndTraditionalChromeClassesExist() {
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterCommandProvider.java")));
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterFileController.java")));
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterOutputPreview.java")));
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterChromeController.java")));
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterMenuBar.java")));
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterDocumentHeader.java")),
                "kept for possible future compact title/header experiments, but not mounted in Pages-style chrome");
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterCanvasBadge.java")));
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterToolBar.java")));
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/inspector/WriterInspectorPane.java")));
        assertTrue(Files.exists(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/inspector/WriterTextFormatInspector.java")));
        assertTrue(Files.exists(Path.of("../delos-writer-javafx/src/main/java/io/github/ggeorg/delos/writer/ui/control/WriterDocumentView.java")));
        assertTrue(Files.exists(Path.of("../delos-writer-javafx/src/main/java/io/github/ggeorg/delos/writer/ui/control/WriterDocumentViewSkin.java")));
    }
}
