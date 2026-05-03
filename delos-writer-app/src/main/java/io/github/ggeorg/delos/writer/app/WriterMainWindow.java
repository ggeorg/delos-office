package io.github.ggeorg.delos.writer.app;

import io.github.ggeorg.delos.javafx.command.CommandPalette;
import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.writer.app.inspector.WriterInspectorPane;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.session.EditorSession;
import io.github.ggeorg.delos.writer.ui.command.EditorContextMenuFactory;
import io.github.ggeorg.delos.writer.ui.control.DelosEditor;
import io.github.ggeorg.delos.writer.ui.control.WriterDocumentView;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public final class WriterMainWindow extends BorderPane {
    private final Stage stage;
    private final EditorSession session = new EditorSession(Document.sample());
    private final WriterOutputPreview outputPreview = WriterOutputPreview.createDefault();
    private final WriterDocumentView documentView;
    private final DelosEditor editor;
    private final CommandRegistry commandRegistry = new CommandRegistry();
    private final WriterFileController fileController;
    private final WriterInsertController insertController;
    private final WriterChromeController chromeController;
    private final EditorContextMenuFactory contextMenuFactory;
    private final WriterMenuBar menuBar;
    private final WriterToolBar toolBar;
    private final WriterCanvasBadge canvasBadge;
    private final WriterInspectorPane inspector;
    private final StackPane rootStack;
    private final VBox appChrome;
    private final BorderPane documentShell;
    private final HBox windowShell;
    private StackPane overlayLayer;
    private CommandPalette commandPalette;
    private boolean inspectorVisible = true;

    public WriterMainWindow(Stage stage) {
        this.stage = Objects.requireNonNull(stage, "stage");
        getStyleClass().add("main-window");
        setPadding(new Insets(0));

        documentView = outputPreview.createDocumentView(session);
        editor = documentView.editor();
        fileController = new WriterFileController(stage, session, editor, this::refreshChrome);
        insertController = new WriterInsertController(stage, editor);

        new WriterCommandProvider(
                commandRegistry,
                fileController,
                insertController,
                documentView,
                session,
                editor,
                this::toggleCommandPalette,
                this::toggleInspector,
                this::isInspectorVisible,
                this::showStatisticsPopover,
                this::showAboutDialog
        ).registerCommands();

        menuBar = new WriterMenuBar(commandRegistry);
        canvasBadge = new WriterCanvasBadge();
        inspector = new WriterInspectorPane(session, editor, commandRegistry);
        inspector.setManaged(inspectorVisible);
        inspector.setVisible(inspectorVisible);
        toolBar = new WriterToolBar(commandRegistry, inspector::selectTab, inspector::selectedTabId, fileController::displayName, session::isDirty, fileController::renameDocumentTitle);

        appChrome = new VBox();
        appChrome.getStyleClass().add("writer-app-chrome");
        if (!menuBar.isUseSystemMenuBar()) {
            appChrome.getChildren().setAll(menuBar, toolBar);
        } else {
            appChrome.getChildren().setAll(toolBar);
        }

        chromeController = new WriterChromeController(
                stage,
                session,
                editor,
                menuBar,
                toolBar,
                canvasBadge,
                fileController,
                this::isInspectorVisible
        );
        contextMenuFactory = new EditorContextMenuFactory(commandRegistry);

        configureCommandPalette();
        configureEditorContextMenu();

        rootStack = new StackPane(documentView, canvasBadge);
        rootStack.getStyleClass().add("editor-stack");
        StackPane.setAlignment(canvasBadge, Pos.BOTTOM_CENTER);
        StackPane.setMargin(canvasBadge, new Insets(0, 0, 20, 0));
        rootStack.widthProperty().addListener((obs, oldValue, newValue) -> resizeOverlayLayer());
        rootStack.heightProperty().addListener((obs, oldValue, newValue) -> resizeOverlayLayer());

        documentShell = new BorderPane();
        documentShell.getStyleClass().add("writer-document-shell");
        documentShell.setCenter(rootStack);

        windowShell = new HBox(documentShell, inspector);
        windowShell.getStyleClass().add("writer-window-shell");
        HBox.setHgrow(documentShell, Priority.ALWAYS);
        setTop(appChrome);
        setCenter(windowShell);

        session.addStateListener(this::refreshChrome);
        editor.caretGeometryProperty().addListener((obs, oldValue, newValue) -> refreshCaretChrome());
        documentView.zoomFactorProperty().addListener((obs, oldValue, newValue) -> refreshCaretChrome());
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            uninstallAccelerators(oldScene);
            installAccelerators(newScene);
        });
        refreshChrome();
    }

    private void configureCommandPalette() {
        overlayLayer = new StackPane();
        overlayLayer.getStyleClass().add("overlay-layer");
        overlayLayer.setManaged(false);
        overlayLayer.setVisible(false);
        overlayLayer.setDisable(true);
        overlayLayer.setPickOnBounds(false);
        overlayLayer.setMouseTransparent(true);

        commandPalette = new CommandPalette(commandRegistry);
        commandPalette.setOnCloseRequested(this::hideCommandPalette);
        commandPalette.setOnCommandExecuted(this::hideCommandPalette);
        StackPane.setAlignment(commandPalette, javafx.geometry.Pos.TOP_CENTER);
        StackPane.setMargin(commandPalette, new Insets(80, 0, 0, 0));
        overlayLayer.getChildren().add(commandPalette);
    }

    private void configureEditorContextMenu() {
        editor.setOnContextMenuRequested(event -> {
            ContextMenu menu = contextMenuFactory.build(editor.hasSelection());
            menu.show(editor, event.getScreenX(), event.getScreenY());
            event.consume();
        });
    }

    private void installAccelerators(Scene scene) {
        commandRegistry.installAccelerators(scene);
    }

    private void uninstallAccelerators(Scene scene) {
        commandRegistry.uninstallAccelerators(scene);
    }

    public boolean requestClose() {
        if (!fileController.requestClose()) {
            return false;
        }
        closePreviewResources();
        return true;
    }

    private void closePreviewResources() {
        try {
            outputPreview.close();
        } catch (IOException ignored) {
            // Closing measurement-only preview resources should not block the
            // app from closing after the user has already confirmed.
        }
    }

    private void toggleInspector() {
        inspectorVisible = !inspectorVisible;
        inspector.setManaged(inspectorVisible);
        inspector.setVisible(inspectorVisible);
        refreshChrome();
    }

    private boolean isInspectorVisible() {
        return inspectorVisible;
    }


    private void toggleCommandPalette() {
        if (commandPalette.isOpen()) {
            hideCommandPalette();
        } else {
            showCommandPalette();
        }
    }

    private void showCommandPalette() {
        canvasBadge.hideStatisticsPopover();
        if (!rootStack.getChildren().contains(overlayLayer)) {
            rootStack.getChildren().add(overlayLayer);
        }
        resizeOverlayLayer();
        overlayLayer.setDisable(false);
        overlayLayer.setVisible(true);
        overlayLayer.setMouseTransparent(false);
        overlayLayer.toFront();
        commandPalette.showPalette();
    }

    private void resizeOverlayLayer() {
        if (rootStack != null) {
            overlayLayer.resizeRelocate(0.0, 0.0, rootStack.getWidth(), rootStack.getHeight());
        }
    }

    private void hideCommandPalette() {
        commandPalette.hidePalette();
        overlayLayer.setMouseTransparent(true);
        overlayLayer.setVisible(false);
        overlayLayer.setDisable(true);
        if (rootStack != null) {
            rootStack.getChildren().remove(overlayLayer);
        }
        documentView.focusEditor();
    }

    private void showStatisticsPopover() {
        canvasBadge.showStatisticsPopover();
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle("About Delos Writer");
        alert.setHeaderText("Delos Writer");
        alert.setContentText("Delos Writer is the word-processing application in the Delos Office suite.");
        alert.showAndWait();
    }

    private void refreshChrome() {
        chromeController.refreshChrome();
        inspector.refresh();
    }

    private void refreshCaretChrome() {
        chromeController.refreshCaretChrome();
        inspector.refresh();
    }

    CommandRegistry commandRegistry() { return commandRegistry; }
    DelosEditor editor() { return editor; }
    WriterDocumentView documentView() { return documentView; }
    Path currentFile() { return fileController.currentFile(); }
    ReadOnlyDoubleProperty zoomFactorProperty() { return documentView.zoomFactorProperty(); }
}
