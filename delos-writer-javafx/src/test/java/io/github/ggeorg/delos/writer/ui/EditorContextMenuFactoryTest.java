package io.github.ggeorg.delos.writer.ui;

import io.github.ggeorg.delos.writer.contract.JavaFxTestSupport;
import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.command.EditorCommand;
import io.github.ggeorg.delos.writer.ui.command.EditorContextMenuFactory;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class EditorContextMenuFactoryTest extends JavaFxTestSupport {
    @Test
    void selectionMenuContainsEditAndFormatActions() {
        ContextMenu menu = onFxThread(() -> new EditorContextMenuFactory(registry()).build(true));
        assertEquals("Cut", onFxThread(() -> menu.getItems().get(0).getText()));
        assertEquals("Copy", onFxThread(() -> menu.getItems().get(1).getText()));
        assertEquals("Paste", onFxThread(() -> menu.getItems().get(2).getText()));
        assertEquals("Strikethrough", onFxThread(() -> menu.getItems().get(7).getText()));
        Menu alignment = onFxThread(() -> (Menu) menu.getItems().get(menu.getItems().size() - 1));
        assertEquals("Alignment", onFxThread(alignment::getText));
    }

    @Test
    void caretOnlyMenuContainsPasteAndSelectAll() {
        ContextMenu menu = onFxThread(() -> new EditorContextMenuFactory(registry()).build(false));
        assertEquals("Paste", onFxThread(() -> menu.getItems().get(0).getText()));
        assertEquals("Select All", onFxThread(() -> menu.getItems().get(2).getText()));
    }

    private CommandRegistry registry() {
        CommandRegistry registry = new CommandRegistry();
        register(registry, "edit.cut", "Cut");
        register(registry, "edit.copy", "Copy");
        register(registry, "edit.paste", "Paste");
        register(registry, "edit.selectAll", "Select All");
        register(registry, "format.bold", "Bold");
        register(registry, "format.italic", "Italic");
        register(registry, "format.underline", "Underline");
        register(registry, "format.strikethrough", "Strikethrough");
        register(registry, "format.alignLeft", "Align Left");
        register(registry, "format.alignCenter", "Align Center");
        register(registry, "format.alignRight", "Align Right");
        register(registry, "format.justify", "Justify");
        return registry;
    }

    private void register(CommandRegistry registry, String id, String label) {
        registry.register(new EditorCommand(id, label, "Test", null, () -> {}, null, null));
    }
}
