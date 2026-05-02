package io.github.ggeorg.delos.slides.app;

import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.command.EditorCommand;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.Objects;
import java.util.function.BooleanSupplier;

final class SlidesCommandProvider {
    private final CommandRegistry registry;
    private final SlidesMainWindow window;

    SlidesCommandProvider(CommandRegistry registry, SlidesMainWindow window) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.window = Objects.requireNonNull(window, "window");
    }

    void registerCommands() {
        register("file.new", "New", "File",
                shortcut(KeyCode.N), window::newDeck, enabled());
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
        register("edit.selectAll", "Select All", "Edit",
                shortcut(KeyCode.A), noop(), disabled());

        register("insert.slide", "New Slide", "Insert",
                new KeyCodeCombination(KeyCode.M, KeyCombination.SHORTCUT_DOWN), window::addSlide, enabled());
        register("insert.textBox", "Text Box", "Insert",
                null, noop(), disabled());
        register("insert.image", "Image…", "Insert",
                null, noop(), disabled());
        register("insert.shape", "Shape", "Insert",
                null, noop(), disabled());

        register("format.slideLayout", "Slide Layout", "Format",
                null, noop(), disabled());
        register("format.theme", "Theme", "Format",
                null, noop(), disabled());

        register("slideshow.start", "Start Slideshow", "Slideshow",
                new KeyCodeCombination(KeyCode.F5), noop(), disabled());
        register("app.preferences", "Preferences…", "Tools",
                null, noop(), disabled());
        register("app.about", "About Delos Slides", "Help",
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
