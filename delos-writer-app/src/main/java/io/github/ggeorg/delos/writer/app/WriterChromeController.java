package io.github.ggeorg.delos.writer.app;

import io.github.ggeorg.delos.writer.session.EditorSession;
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
    private final WriterCanvasBadge canvasBadge;
    private final WriterFileController fileController;
    private final BooleanSupplier inspectorVisible;

    WriterChromeController(
            Stage stage,
            EditorSession session,
            DelosEditor editor,
            WriterMenuBar menuBar,
            WriterToolBar toolBar,
            WriterCanvasBadge canvasBadge,
            WriterFileController fileController,
            BooleanSupplier inspectorVisible
    ) {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.session = Objects.requireNonNull(session, "session");
        this.editor = Objects.requireNonNull(editor, "editor");
        this.menuBar = Objects.requireNonNull(menuBar, "menuBar");
        this.toolBar = Objects.requireNonNull(toolBar, "toolBar");
        this.canvasBadge = Objects.requireNonNull(canvasBadge, "canvasBadge");
        this.fileController = Objects.requireNonNull(fileController, "fileController");
        this.inspectorVisible = Objects.requireNonNull(inspectorVisible, "inspectorVisible");
    }

    void refreshChrome() {
        refreshDocumentChrome();
        refreshCaretChrome();
    }

    void refreshDocumentChrome() {
        menuBar.refreshFromCommands();
        toolBar.refreshFromCommands();
        refreshStageTitle();
    }

    void refreshCaretChrome() {
        canvasBadge.update(editor.currentPageNumber(), editor.totalPageCount(), session.document());
        menuBar.refreshFromCommands();
        toolBar.refreshFromCommands();
    }

    private void refreshStageTitle() {
        stage.setTitle(fileController.displayName()
                + (session.isDirty() ? " *" : "")
                + " — Delos Writer");
    }
}
