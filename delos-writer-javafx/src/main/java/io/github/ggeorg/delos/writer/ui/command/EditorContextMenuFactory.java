package io.github.ggeorg.delos.writer.ui.command;

import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.command.EditorCommand;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import java.util.Objects;

public final class EditorContextMenuFactory {
    private final CommandRegistry registry;

    public EditorContextMenuFactory(CommandRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public ContextMenu build(boolean hasSelection) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("editor-context-menu");

        if (hasSelection) {
            menu.getItems().addAll(
                    item("edit.cut"),
                    item("edit.copy"),
                    item("edit.paste"),
                    new SeparatorMenuItem(),
                    item("format.bold"),
                    item("format.italic"),
                    item("format.underline"),
                    item("format.strikethrough"),
                    new SeparatorMenuItem(),
                    alignmentMenu()
            );
        } else {
            menu.getItems().addAll(
                    item("edit.paste"),
                    new SeparatorMenuItem(),
                    item("edit.selectAll"),
                    new SeparatorMenuItem(),
                    alignmentMenu()
            );
        }

        return menu;
    }

    private Menu alignmentMenu() {
        Menu menu = new Menu("Alignment");
        menu.getItems().addAll(
                item("format.alignLeft"),
                item("format.alignCenter"),
                item("format.alignRight"),
                item("format.justify")
        );
        return menu;
    }

    private MenuItem item(String commandId) {
        EditorCommand command = registry.byId(commandId)
                .orElseThrow(() -> new IllegalArgumentException("Missing command: " + commandId));
        MenuItem item = new MenuItem(command.label());
        item.setOnAction(event -> command.execute());
        item.setDisable(!command.isEnabled());
        if (command.accelerator() != null) {
            item.setAccelerator(command.accelerator());
        }
        return item;
    }
}
