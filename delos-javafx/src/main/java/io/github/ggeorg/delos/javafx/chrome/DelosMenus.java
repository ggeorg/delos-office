package io.github.ggeorg.delos.javafx.chrome;

import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.command.EditorCommand;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

import java.util.Objects;

/** Shared menu helpers for Delos application chrome. */
public final class DelosMenus {
    private DelosMenus() {
    }

    public static void configure(MenuBar menuBar, String appStyleClass) {
        Objects.requireNonNull(menuBar, "menuBar");
        menuBar.getStyleClass().add("delos-menu-bar");
        if (appStyleClass != null && !appStyleClass.isBlank()) {
            menuBar.getStyleClass().add(appStyleClass);
        }
        menuBar.setUseSystemMenuBar(true);
    }

    public static MenuItem item(CommandRegistry commandRegistry, String commandId) {
        Objects.requireNonNull(commandRegistry, "commandRegistry");
        EditorCommand command = commandRegistry.byId(commandId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown command: " + commandId));

        MenuItem item = command.active() == null ? new MenuItem(command.label()) : new CheckMenuItem(command.label());
        item.setUserData(command);
        item.setAccelerator(command.accelerator());
        item.setOnAction(event -> {
            command.execute();
            refresh(item);
        });
        refresh(item);
        return item;
    }

    public static MenuItem disabledItem(String label) {
        MenuItem item = new MenuItem(label);
        item.setDisable(true);
        return item;
    }

    public static void refresh(MenuBar menuBar) {
        Objects.requireNonNull(menuBar, "menuBar");
        menuBar.getMenus().forEach(DelosMenus::refresh);
    }

    public static void refresh(Menu menu) {
        refresh((MenuItem) menu);
        menu.getItems().forEach(item -> {
            refresh(item);
            if (item instanceof Menu subMenu) {
                refresh(subMenu);
            }
        });
    }

    private static void refresh(MenuItem menuItem) {
        Object userData = menuItem.getUserData();
        if (!(userData instanceof EditorCommand command)) {
            return;
        }
        menuItem.setDisable(!command.isEnabled());
        if (menuItem instanceof CheckMenuItem checkMenuItem) {
            checkMenuItem.setSelected(command.isActive());
        }
    }
}
