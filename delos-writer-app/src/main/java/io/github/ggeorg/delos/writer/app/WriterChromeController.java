package io.github.ggeorg.delos.writer.app;

import io.github.ggeorg.delos.writer.session.EditorSession;
import io.github.ggeorg.delos.writer.ui.StatusBar;
import io.github.ggeorg.delos.writer.ui.control.DelosEditor;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.function.BooleanSupplier;

final class WriterChromeController {
    private final Stage stage;
    private final EditorSession session;
    private final DelosEditor editor;
    private final WriterMenuBar menuBar;
    private final WriterToolBar toolBar;
    private final StatusBar statusBar;
    private final WriterFileController fileController;
    private final BooleanSupplier inspectorVisible;

    WriterChromeController(
            Stage stage,
            EditorSession session,
            DelosEditor editor,
            WriterMenuBar menuBar,
            WriterToolBar toolBar,
            StatusBar statusBar,
            WriterFileController fileController,
            BooleanSupplier inspectorVisible
    ) {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.session = Objects.requireNonNull(session, "session");
        this.editor = Objects.requireNonNull(editor, "editor");
        this.menuBar = Objects.requireNonNull(menuBar, "menuBar");
        this.toolBar = Objects.requireNonNull(toolBar, "toolBar");
        this.statusBar = Objects.requireNonNull(statusBar, "statusBar");
        this.fileController = Objects.requireNonNull(fileController, "fileController");
        this.inspectorVisible = Objects.requireNonNull(inspectorVisible, "inspectorVisible");
    }

    void refreshChrome() {
        refreshDocumentChrome();
        refreshCaretChrome();
    }

    void refreshDocumentChrome() {
        statusBar.setWordCount(StatusBar.countWords(session.document()));
        statusBar.setLanguage("English");
        menuBar.refreshFromCommands();
        toolBar.refreshFromCommands();
        refreshStageTitle();
    }

    void refreshCaretChrome() {
        statusBar.setPageInfo(editor.currentPageNumber(), editor.totalPageCount());
        statusBar.setZoomFactor(editor.zoom());
        menuBar.refreshFromCommands();
        toolBar.refreshFromCommands();
    }

    private void refreshStageTitle() {
        stage.setTitle("Delos Writer — " + fileController.displayName()
                + (session.isDirty() ? " *" : "")
                + (inspectorVisible.getAsBoolean() ? " · inspector" : ""));
    }
}
