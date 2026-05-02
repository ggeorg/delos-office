package io.github.ggeorg.delos.calc.app;

import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.command.EditorCommand;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Registers the spreadsheet application commands used by menus, toolbar, and accelerators. */
final class CalcCommandProvider {
    private final CommandRegistry registry;
    private final Runnable newWorkbook;
    private final Runnable openWorkbook;
    private final Runnable saveWorkbook;
    private final Runnable saveWorkbookAs;
    private final Runnable clearSelectedCell;
    private final BooleanSupplier canClearSelectedCell;
    private final Runnable exitApplication;
    private final Runnable showAboutDialog;

    CalcCommandProvider(
            CommandRegistry registry,
            Runnable newWorkbook,
            Runnable openWorkbook,
            Runnable saveWorkbook,
            Runnable saveWorkbookAs,
            Runnable clearSelectedCell,
            BooleanSupplier canClearSelectedCell,
            Runnable exitApplication,
            Runnable showAboutDialog
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.newWorkbook = Objects.requireNonNull(newWorkbook, "newWorkbook");
        this.openWorkbook = Objects.requireNonNull(openWorkbook, "openWorkbook");
        this.saveWorkbook = Objects.requireNonNull(saveWorkbook, "saveWorkbook");
        this.saveWorkbookAs = Objects.requireNonNull(saveWorkbookAs, "saveWorkbookAs");
        this.clearSelectedCell = Objects.requireNonNull(clearSelectedCell, "clearSelectedCell");
        this.canClearSelectedCell = Objects.requireNonNull(canClearSelectedCell, "canClearSelectedCell");
        this.exitApplication = Objects.requireNonNull(exitApplication, "exitApplication");
        this.showAboutDialog = Objects.requireNonNull(showAboutDialog, "showAboutDialog");
    }

    void registerCommands() {
        register("file.new", "New Spreadsheet", "File", new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), newWorkbook);
        register("file.open", "Open…", "File", new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN), openWorkbook);
        register("file.save", "Save", "File", new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), saveWorkbook);
        register("file.saveAs", "Save As…", "File", new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), saveWorkbookAs);
        registerDisabled("file.print", "Print…", "File", new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN));
        register("app.exit", "Exit", "Application", null, exitApplication);

        registerDisabled("edit.undo", "Undo", "Edit", new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN));
        registerDisabled("edit.redo", "Redo", "Edit", new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        registerDisabled("edit.cut", "Cut", "Edit", new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN));
        registerDisabled("edit.copy", "Copy", "Edit", new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));
        registerDisabled("edit.paste", "Paste", "Edit", new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN));
        registerDisabled("edit.selectAll", "Select All", "Edit", new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN));
        register("edit.clearContents", "Clear Contents", "Edit", new KeyCodeCombination(KeyCode.DELETE), clearSelectedCell, canClearSelectedCell, null);
        registerDisabled("edit.find", "Find…", "Edit", new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN));

        registerDisabled("insert.function", "Function…", "Insert", null);
        registerDisabled("insert.row", "Rows", "Insert", null);
        registerDisabled("insert.column", "Columns", "Insert", null);
        registerDisabled("insert.sheet", "Sheet", "Insert", null);
        registerDisabled("insert.chart", "Chart…", "Insert", null);

        registerDisabled("format.cells", "Cells…", "Format", new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.SHORTCUT_DOWN));
        registerDisabled("format.bold", "Bold", "Format", new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN));
        registerDisabled("format.italic", "Italic", "Format", new KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN));
        registerDisabled("format.textColor", "Text Color", "Format", null);
        registerDisabled("format.number", "Number", "Format", null);
        registerDisabled("format.clearFormatting", "Clear Formatting", "Format", null);

        registerDisabled("data.sort", "Sort Range…", "Data", null);
        registerDisabled("data.filter", "Filter", "Data", null);
        registerDisabled("tools.recalculate", "Recalculate", "Tools", null);
        registerDisabled("app.preferences", "Preferences…", "Application", null);
        register("app.about", "About Delos Calc", "Application", null, showAboutDialog);
    }

    private void register(String id, String label, String category, KeyCombination accelerator, Runnable action) {
        register(id, label, category, accelerator, action, null, null);
    }

    private void registerDisabled(String id, String label, String category, KeyCombination accelerator) {
        register(id, label, category, accelerator, () -> { }, () -> false, null);
    }

    private void register(String id, String label, String category, KeyCombination accelerator, Runnable action, BooleanSupplier enabled, BooleanSupplier active) {
        registry.register(new EditorCommand(id, label, category, accelerator, action, enabled, active));
    }
}
