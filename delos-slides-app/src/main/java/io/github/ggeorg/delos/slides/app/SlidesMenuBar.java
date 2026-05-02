package io.github.ggeorg.delos.slides.app;

import io.github.ggeorg.delos.javafx.chrome.DelosMenus;
import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import java.util.Objects;

final class SlidesMenuBar extends MenuBar {
    private final CommandRegistry commandRegistry;

    SlidesMenuBar(CommandRegistry commandRegistry) {
        this.commandRegistry = Objects.requireNonNull(commandRegistry, "commandRegistry");
        DelosMenus.configure(this, "slides-menu-bar");
        setUseSystemMenuBar(true);
        getMenus().setAll(
                fileMenu(),
                editMenu(),
                viewMenu(),
                insertMenu(),
                formatMenu(),
                slideshowMenu(),
                toolsMenu(),
                windowMenu(),
                helpMenu()
        );
        refreshFromCommands();
    }

    void refreshFromCommands() {
        DelosMenus.refresh(this);
    }

    private Menu fileMenu() {
        Menu menu = new Menu("File");
        menu.getItems().setAll(
                item("file.new"),
                item("file.open"),
                new SeparatorMenuItem(),
                item("file.save"),
                item("file.saveAs"),
                new SeparatorMenuItem(),
                item("file.exportPdf"),
                new SeparatorMenuItem(),
                item("file.print")
        );
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private Menu editMenu() {
        Menu menu = new Menu("Edit");
        menu.getItems().setAll(
                item("edit.undo"),
                item("edit.redo"),
                new SeparatorMenuItem(),
                item("edit.cut"),
                item("edit.copy"),
                item("edit.paste"),
                new SeparatorMenuItem(),
                item("edit.selectAll")
        );
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private Menu viewMenu() {
        return new Menu("View");
    }

    private Menu insertMenu() {
        Menu menu = new Menu("Insert");
        menu.getItems().setAll(
                item("insert.slide"),
                new SeparatorMenuItem(),
                item("insert.textBox"),
                item("insert.image"),
                item("insert.shape")
        );
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private Menu formatMenu() {
        Menu menu = new Menu("Format");
        menu.getItems().setAll(
                item("format.slideLayout"),
                item("format.theme")
        );
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private Menu slideshowMenu() {
        Menu menu = new Menu("Slideshow");
        menu.getItems().setAll(item("slideshow.start"));
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private Menu toolsMenu() {
        Menu menu = new Menu("Tools");
        menu.getItems().setAll(item("app.preferences"));
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private Menu windowMenu() {
        return new Menu("Window");
    }

    private Menu helpMenu() {
        Menu menu = new Menu("Help");
        menu.getItems().setAll(item("app.about"));
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private MenuItem item(String commandId) {
        return DelosMenus.item(commandRegistry, commandId);
    }
}
