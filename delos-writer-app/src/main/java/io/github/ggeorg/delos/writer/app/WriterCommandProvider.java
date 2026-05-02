package io.github.ggeorg.delos.writer.app;

import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.command.EditorCommand;
import io.github.ggeorg.delos.writer.document.Alignment;
import io.github.ggeorg.delos.writer.document.ListMarkerKind;
import io.github.ggeorg.delos.writer.editor.TextStyle;
import io.github.ggeorg.delos.writer.session.EditorSession;
import io.github.ggeorg.delos.writer.ui.control.DelosEditor;
import io.github.ggeorg.delos.writer.ui.control.WriterDocumentView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.Objects;
import java.util.function.BooleanSupplier;

final class WriterCommandProvider {
    private final CommandRegistry registry;
    private final WriterFileController fileController;
    private final WriterInsertController insertController;
    private final WriterDocumentView documentView;
    private final EditorSession session;
    private final DelosEditor editor;
    private final Runnable toggleCommandPalette;
    private final Runnable toggleInspector;
    private final BooleanSupplier inspectorVisible;
    private final Runnable showWordCountDialog;
    private final Runnable showAboutDialog;

    WriterCommandProvider(
            CommandRegistry registry,
            WriterFileController fileController,
            WriterInsertController insertController,
            WriterDocumentView documentView,
            EditorSession session,
            DelosEditor editor,
            Runnable toggleCommandPalette,
            Runnable toggleInspector,
            BooleanSupplier inspectorVisible,
            Runnable showWordCountDialog,
            Runnable showAboutDialog
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.fileController = Objects.requireNonNull(fileController, "fileController");
        this.insertController = Objects.requireNonNull(insertController, "insertController");
        this.documentView = Objects.requireNonNull(documentView, "documentView");
        this.session = Objects.requireNonNull(session, "session");
        this.editor = Objects.requireNonNull(editor, "editor");
        this.toggleCommandPalette = Objects.requireNonNull(toggleCommandPalette, "toggleCommandPalette");
        this.toggleInspector = Objects.requireNonNull(toggleInspector, "toggleInspector");
        this.inspectorVisible = Objects.requireNonNull(inspectorVisible, "inspectorVisible");
        this.showWordCountDialog = Objects.requireNonNull(showWordCountDialog, "showWordCountDialog");
        this.showAboutDialog = Objects.requireNonNull(showAboutDialog, "showAboutDialog");
    }

    void registerCommands() {
        register("file.new", "New", "File", new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), fileController::newDocument);
        register("file.open", "Open…", "File", new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN), fileController::openDocument);
        register("file.save", "Save", "File", new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), fileController::saveDocument);
        register("file.saveAs", "Save As…", "File", new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), fileController::saveDocumentAs);
        register("file.exportPdf", "Export PDF…", "File", null, fileController::exportPdf);
        register("export.html", "Export HTML", "Export", null, fileController::exportHtml);
        register("export.markdown", "Export Markdown", "Export", new KeyCodeCombination(KeyCode.M, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), fileController::exportMarkdown);
        register("file.print", "Print…", "File", new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN), fileController::printDocument);

        register("edit.undo", "Undo", "Edit", new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), editor::undo, session::canUndo, null);
        register("edit.redo", "Redo", "Edit", new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), editor::redo, session::canRedo, null);
        register("edit.redo.alt", "Redo", "Edit", new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN), editor::redo, session::canRedo, null);
        register("edit.copy", "Copy", "Edit", new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN), editor::copy);
        register("edit.cut", "Cut", "Edit", new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN), editor::cut);
        register("edit.paste", "Paste", "Edit", new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN), editor::paste);
        register("edit.selectAll", "Select All", "Edit", new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN), editor::selectAll);
        registerDisabled("edit.find", "Find…", "Edit", new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN));
        register("edit.formula", "Edit Formula…", "Edit", null, insertController::editSelectedFormula, editor::hasSelectedFormulaBlock, null);
        register("edit.imageProperties", "Image Properties…", "Edit", null, insertController::editSelectedImageProperties, editor::hasSelectedImageBlock, null);

        registerDisabled("insert.pageBreak", "Page Break", "Insert", null);
        register("insert.image", "Image…", "Insert", null, insertController::insertImage);
        register("image.replace", "Replace Image…", "Format", null, insertController::insertImage, editor::hasSelectedImageBlock, null);
        register("insert.table", "Table…", "Insert", null, insertController::insertTable);
        register("table.insertRowAbove", "Insert Row Above", "Table", null, editor::insertTableRowAbove, editor::hasSelectedTable, null);
        register("table.insertRowBelow", "Insert Row Below", "Table", null, editor::insertTableRowBelow, editor::hasSelectedTable, null);
        register("table.deleteRow", "Delete Row", "Table", null, editor::deleteTableRow, editor::hasSelectedTable, null);
        register("table.insertColumnLeft", "Insert Column Left", "Table", null, editor::insertTableColumnLeft, editor::hasSelectedTable, null);
        register("table.insertColumnRight", "Insert Column Right", "Table", null, editor::insertTableColumnRight, editor::hasSelectedTable, null);
        register("table.deleteColumn", "Delete Column", "Table", null, editor::deleteTableColumn, editor::hasSelectedTable, null);
        register("insert.formula", "Formula…", "Insert", null, insertController::insertFormula);

        register("format.bold", "Bold", "Format", new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN), () -> editor.toggleStyle(TextStyle.BOLD), null, () -> editor.isTextStyleActive(TextStyle.BOLD));
        register("format.italic", "Italic", "Format", new KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN), () -> editor.toggleStyle(TextStyle.ITALIC), null, () -> editor.isTextStyleActive(TextStyle.ITALIC));
        register("format.underline", "Underline", "Format", new KeyCodeCombination(KeyCode.U, KeyCombination.SHORTCUT_DOWN), () -> editor.toggleStyle(TextStyle.UNDERLINE), null, () -> editor.isTextStyleActive(TextStyle.UNDERLINE));
        register("format.strikethrough", "Strikethrough", "Format", new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), () -> editor.toggleStyle(TextStyle.STRIKETHROUGH), null, () -> editor.isTextStyleActive(TextStyle.STRIKETHROUGH));
        registerDisabled("format.textColor", "Text Color", "Format", null);
        registerDisabled("format.clearFormatting", "Clear Formatting", "Format", null);
        register("format.alignLeft", "Align Left", "Format", new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN), () -> editor.setParagraphAlignment(Alignment.LEFT), null, () -> editor.isParagraphAlignmentActive(Alignment.LEFT));
        register("format.alignCenter", "Align Center", "Format", new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN), () -> editor.setParagraphAlignment(Alignment.CENTER), null, () -> editor.isParagraphAlignmentActive(Alignment.CENTER));
        register("format.alignRight", "Align Right", "Format", new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN), () -> editor.setParagraphAlignment(Alignment.RIGHT), null, () -> editor.isParagraphAlignmentActive(Alignment.RIGHT));
        register("format.justify", "Justify", "Format", new KeyCodeCombination(KeyCode.J, KeyCombination.SHORTCUT_DOWN), () -> editor.setParagraphAlignment(Alignment.JUSTIFY), null, () -> editor.isParagraphAlignmentActive(Alignment.JUSTIFY));
        register("format.bulletedList", "Bulleted List", "Format", null, () -> editor.toggleListKind(ListMarkerKind.BULLET), null, () -> editor.isListKindActive(ListMarkerKind.BULLET));
        register("format.numberedList", "Numbered List", "Format", null, () -> editor.toggleListKind(ListMarkerKind.NUMBERED), null, () -> editor.isListKindActive(ListMarkerKind.NUMBERED));
        register("format.decreaseListLevel", "Decrease List Level", "Format", null, editor::decreaseListLevel, editor::canDecreaseListLevel, null);
        register("format.increaseListLevel", "Increase List Level", "Format", null, editor::increaseListLevel, editor::canIncreaseListLevel, null);
        registerDisabled("format.lineSpacing", "Line Spacing", "Format", null);

        register("view.commandPalette", "Command Palette", "View", new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), toggleCommandPalette);
        register("view.zoomIn", "Zoom In", "View", new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN), () -> documentView.setZoomFactor(documentView.zoomFactorProperty().get() * 1.10));
        register("view.zoomOut", "Zoom Out", "View", new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN), () -> documentView.setZoomFactor(documentView.zoomFactorProperty().get() * 0.90));
        register("view.zoomReset", "Actual Size", "View", new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN), () -> documentView.setZoomFactor(1.0));
        register("view.zoomFitWidth", "Fit", "View", null, documentView::zoomToFitWidth, null, documentView::isFitWidthMode);
        registerZoomPreset("view.zoom50", "50%", 0.50);
        registerZoomPreset("view.zoom75", "75%", 0.75);
        registerZoomPreset("view.zoom90", "90%", 0.90);
        registerZoomPreset("view.zoom100", "100%", 1.00);
        registerZoomPreset("view.zoom125", "125%", 1.25);
        registerZoomPreset("view.zoom150", "150%", 1.50);
        registerZoomPreset("view.zoom200", "200%", 2.00);
        register("view.toggleRuler", "Show Ruler", "View", new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN),
                () -> documentView.setRulersVisible(!documentView.areRulersVisible()),
                null,
                documentView::areRulersVisible);
        register("view.toggleInspector", "Toggle Inspector", "View", new KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), toggleInspector, null, inspectorVisible);

        register("tools.wordCount", "Word Count", "Tools", null, showWordCountDialog);
        registerDisabled("app.preferences", "Preferences…", "Application", null);
        register("app.about", "About Delos Writer", "Application", null, showAboutDialog);
    }

    private void register(String id, String label, String category, KeyCombination accelerator, Runnable action) {
        register(id, label, category, accelerator, action, null, null);
    }

    private void registerDisabled(String id, String label, String category, KeyCombination accelerator) {
        register(id, label, category, accelerator, () -> { }, () -> false, null);
    }

    private void registerZoomPreset(String id, String label, double zoomFactor) {
        register(id, label, "View", null,
                () -> documentView.setZoomFactor(zoomFactor),
                null,
                () -> !documentView.isFitWidthMode()
                        && Math.abs(documentView.zoomFactorProperty().get() - zoomFactor) < 0.005);
    }

    private void register(String id, String label, String category, KeyCombination accelerator, Runnable action, BooleanSupplier enabled, BooleanSupplier active) {
        registry.register(new EditorCommand(id, label, category, accelerator, action, enabled, active));
    }
}
