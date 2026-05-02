package io.github.ggeorg.delos.writer.ui;

import io.github.ggeorg.delos.writer.contract.JavaFxTestSupport;
import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.command.EditorCommand;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TitleBarTest extends JavaFxTestSupport {

    @Test
    void reflectsDocumentTitleDirtyStateAndCommandStates() {
        AtomicBoolean undoEnabled = new AtomicBoolean(false);
        AtomicBoolean redoEnabled = new AtomicBoolean(true);
        AtomicBoolean inspectorActive = new AtomicBoolean(true);

        CommandRegistry registry = populatedRegistry(undoEnabled::get, redoEnabled::get, inspectorActive::get);
        TitleBar bar = onFxThread(() -> new TitleBar(registry));

        onFxThread(() -> {
            bar.setDocumentTitle("Draft Proposal");
            bar.setDirty(true);
            bar.refreshFromCommands();
        });

        assertEquals("Draft Proposal", onFxThread(() -> bar.documentTitleLabel().getText()));
        assertEquals("*", onFxThread(() -> bar.dirtyIndicatorLabel().getText()));
        assertTrue(onFxThread(() -> bar.inspectorToggle().isSelected()));
        assertFalse(onFxThread(() -> bar.inspectorToggle().isDisabled()));

        inspectorActive.set(false);
        onFxThread(bar::refreshFromCommands);

        assertFalse(onFxThread(() -> bar.inspectorToggle().isSelected()));
    }

    @Test
    void commitsRenamedDocumentTitleOnAction() {
        CommandRegistry registry = populatedRegistry(() -> true, () -> true, () -> false);
        AtomicReference<String> committed = new AtomicReference<>();
        TitleBar bar = onFxThread(() -> {
            TitleBar titleBar = new TitleBar(registry);
            titleBar.setDocumentTitle("Old Title");
            titleBar.setOnDocumentTitleCommitted(committed::set);
            return titleBar;
        });

        onFxThread(() -> {
            bar.beginTitleEdit();
            bar.documentTitleEditor().setText("Renamed");
            bar.commitTitleEdit();
        });

        assertEquals("Renamed", committed.get());
    }

    private static CommandRegistry populatedRegistry(
            BooleanSupplier undoEnabled,
            BooleanSupplier redoEnabled,
            BooleanSupplier inspectorActive
    ) {
        CommandRegistry registry = new CommandRegistry();
        register(registry, "file.new", "New", null, () -> { });
        register(registry, "file.open", "Open", null, () -> { });
        register(registry, "file.save", "Save", null, () -> { });
        register(registry, "file.saveAs", "Save As", null, () -> { });
        register(registry, "export.html", "Export HTML", null, () -> { });
        register(registry, "export.markdown", "Export Markdown", null, () -> { });
        register(registry, "app.about", "About Delos", null, () -> { });
        registry.register(new EditorCommand("edit.undo", "Undo", "Edit",
                new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), () -> { }, undoEnabled, null));
        registry.register(new EditorCommand("edit.redo", "Redo", "Edit",
                new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN), () -> { }, redoEnabled, null));
        registry.register(new EditorCommand("view.toggleInspector", "Toggle Inspector", "View",
                null, () -> { }, () -> true, inspectorActive));
        return registry;
    }

    private static void register(CommandRegistry registry, String id, String label, KeyCombination key, Runnable action) {
        registry.register(new EditorCommand(id, label, "Test", key, action, null, null));
    }
}
