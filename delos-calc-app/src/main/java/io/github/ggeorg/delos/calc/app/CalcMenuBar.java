package io.github.ggeorg.delos.calc.app;

import io.github.ggeorg.delos.javafx.chrome.DelosMenus;
import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import java.util.Objects;

/** Traditional spreadsheet menu bar backed by the shared Delos command registry. */
final class CalcMenuBar extends MenuBar {
    private final CommandRegistry commandRegistry;

    CalcMenuBar(CommandRegistry commandRegistry) {
        this.commandRegistry = Objects.requireNonNull(commandRegistry, "commandRegistry");
        DelosMenus.configure(this, "calc-menu-bar");
        setUseSystemMenuBar(true);
        getMenus().setAll(fileMenu(), editMenu(), viewMenu(), insertMenu(), formatMenu(), dataMenu(), toolsMenu(), windowMenu(), helpMenu());
        refreshFromCommands();
    }

    void refreshFromCommands() {
        DelosMenus.refresh(this);
    }

    private Menu fileMenu() {
        Menu menu = new Menu("File");
        menu.getItems().setAll(item("file.new"), item("file.open"), new SeparatorMenuItem(), item("file.save"), item("file.saveAs"), new SeparatorMenuItem(), item("file.print"), new SeparatorMenuItem(), item("app.exit"));
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private Menu editMenu() {
        Menu menu = new Menu("Edit");
        menu.getItems().setAll(item("edit.undo"), item("edit.redo"), new SeparatorMenuItem(), item("edit.cut"), item("edit.copy"), item("edit.paste"), new SeparatorMenuItem(), item("edit.clearContents"), item("edit.selectAll"), new SeparatorMenuItem(), item("edit.find"));
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private Menu viewMenu() {
        Menu menu = new Menu("View");
        menu.getItems().setAll(disabledItem("Formula Bar"), disabledItem("Grid Lines"), new SeparatorMenuItem(), disabledItem("Freeze Panes"));
        return menu;
    }

    private Menu insertMenu() {
        Menu menu = new Menu("Insert");
        menu.getItems().setAll(item("insert.function"), new SeparatorMenuItem(), item("insert.row"), item("insert.column"), item("insert.sheet"), new SeparatorMenuItem(), item("insert.chart"));
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private Menu formatMenu() {
        Menu menu = new Menu("Format");
        menu.getItems().setAll(item("format.cells"), new SeparatorMenuItem(), item("format.bold"), item("format.italic"), item("format.textColor"), new SeparatorMenuItem(), item("format.number"), item("format.clearFormatting"));
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private Menu dataMenu() {
        Menu menu = new Menu("Data");
        menu.getItems().setAll(item("data.sort"), item("data.filter"));
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private Menu toolsMenu() {
        Menu menu = new Menu("Tools");
        menu.getItems().setAll(item("tools.recalculate"), new SeparatorMenuItem(), item("app.preferences"));
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

    private static MenuItem disabledItem(String label) {
        return DelosMenus.disabledItem(label);
    }
}
