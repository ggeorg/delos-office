package io.github.ggeorg.delos.base.app;

import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.command.EditorCommand;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.Objects;
import java.util.function.BooleanSupplier;

final class BaseCommandProvider {
    private final CommandRegistry registry;
    private final BaseMainWindow window;

    BaseCommandProvider(CommandRegistry registry, BaseMainWindow window) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.window = Objects.requireNonNull(window, "window");
    }

    void registerCommands() {
        register("file.new", "New", "File",
                shortcut(KeyCode.N), window::newProject, enabled());
        register("file.open", "Open…", "File",
                shortcut(KeyCode.O), noop(), disabled());
        register("file.save", "Save", "File",
                shortcut(KeyCode.S), noop(), disabled());
        register("file.saveAs", "Save As…", "File",
                new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                noop(), disabled());
        register("file.exportPdf", "Export as PDF", "File",
                null, noop(), disabled());
        register("file.print", "Print…", "File",
                shortcut(KeyCode.P), noop(), disabled());

        register("edit.undo", "Undo", "Edit",
                shortcut(KeyCode.Z), noop(), disabled());
        register("edit.redo", "Redo", "Edit",
                new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                noop(), disabled());
        register("edit.cut", "Cut", "Edit",
                shortcut(KeyCode.X), noop(), disabled());
        register("edit.copy", "Copy", "Edit",
                shortcut(KeyCode.C), noop(), disabled());
        register("edit.paste", "Paste", "Edit",
                shortcut(KeyCode.V), noop(), disabled());
        register("edit.delete", "Delete", "Edit",
                null, noop(), disabled());

        register("insert.table", "Table", "Insert",
                null, window::addTable, enabled());
        register("insert.query", "Query", "Insert",
                null, window::addQuery, enabled());
        register("insert.form", "Form", "Insert",
                null, window::addForm, enabled());
        register("insert.report", "Report", "Insert",
                null, window::addReport, enabled());

        register("tools.sqlConsole", "SQL Console", "Tools",
                null, noop(), disabled());
        register("tools.relationships", "Relationships", "Tools",
                null, noop(), disabled());
        register("app.preferences", "Preferences…", "Tools",
                null, noop(), disabled());
        register("app.about", "About Delos Base", "Help",
                null, window::showAboutDialog, enabled());
    }

    private void register(String id, String label, String category, KeyCombination accelerator,
                          Runnable action, BooleanSupplier enabled) {
        registry.register(new EditorCommand(id, label, category, accelerator, action, enabled, null));
    }

    private static KeyCodeCombination shortcut(KeyCode keyCode) {
        return new KeyCodeCombination(keyCode, KeyCombination.SHORTCUT_DOWN);
    }

    private static Runnable noop() {
        return () -> { };
    }

    private static BooleanSupplier enabled() {
        return () -> true;
    }

    private static BooleanSupplier disabled() {
        return () -> false;
    }
}
