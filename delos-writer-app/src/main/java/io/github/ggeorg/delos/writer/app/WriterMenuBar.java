package io.github.ggeorg.delos.writer.app;

import io.github.ggeorg.delos.javafx.chrome.DelosMenus;
import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SeparatorMenuItem;

import java.util.Objects;

final class WriterMenuBar extends MenuBar {
    private final CommandRegistry commandRegistry;

    WriterMenuBar(CommandRegistry commandRegistry) {
        this.commandRegistry = Objects.requireNonNull(commandRegistry, "commandRegistry");
        DelosMenus.configure(this, "writer-menu-bar");
        getMenus().setAll(
                fileMenu(),
                editMenu(),
                viewMenu(),
                insertMenu(),
                formatMenu(),
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
                exportMenu(),
                new SeparatorMenuItem(),
                item("file.print")
        );
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private Menu exportMenu() {
        Menu menu = new Menu("Export");
        menu.getItems().setAll(
                item("file.exportPdf"),
                item("export.html"),
                item("export.markdown")
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
                item("edit.selectAll"),
                new SeparatorMenuItem(),
                item("edit.formula"),
                item("edit.imageProperties"),
                new SeparatorMenuItem(),
                item("edit.find")
        );
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private Menu viewMenu() {
        Menu menu = new Menu("View");
        menu.getItems().setAll(
                item("view.commandPalette"),
                new SeparatorMenuItem(),
                item("view.zoomIn"),
                item("view.zoomOut"),
                item("view.zoomReset"),
                item("view.zoomFitWidth"),
                zoomPresetMenu(),
                new SeparatorMenuItem(),
                item("view.toggleRuler"),
                item("view.toggleInspector")
        );
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private Menu zoomPresetMenu() {
        Menu menu = new Menu("Zoom");
        menu.getItems().setAll(
                item("view.zoom50"),
                item("view.zoom75"),
                item("view.zoom90"),
                item("view.zoom100"),
                item("view.zoom125"),
                item("view.zoom150"),
                item("view.zoom200")
        );
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private Menu insertMenu() {
        Menu menu = new Menu("Insert");
        menu.getItems().setAll(
                item("insert.pageBreak"),
                new SeparatorMenuItem(),
                item("insert.image"),
                item("insert.table"),
                item("insert.formula")
        );
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private Menu formatMenu() {
        Menu menu = new Menu("Format");
        menu.getItems().setAll(
                textMenu(),
                paragraphMenu(),
                new SeparatorMenuItem(),
                item("format.clearFormatting")
        );
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private Menu textMenu() {
        Menu menu = new Menu("Text");
        menu.getItems().setAll(
                item("format.bold"),
                item("format.italic"),
                item("format.underline"),
                item("format.strikethrough"),
                new SeparatorMenuItem(),
                item("format.textColor")
        );
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private Menu paragraphMenu() {
        Menu menu = new Menu("Paragraph");
        menu.getItems().setAll(
                item("format.alignLeft"),
                item("format.alignCenter"),
                item("format.alignRight"),
                item("format.justify"),
                new SeparatorMenuItem(),
                item("format.bulletedList"),
                item("format.numberedList"),
                item("format.lineSpacing")
        );
        menu.setOnShowing(event -> refreshFromCommands());
        return menu;
    }

    private Menu toolsMenu() {
        Menu menu = new Menu("Tools");
        menu.getItems().setAll(
                item("tools.wordCount"),
                new SeparatorMenuItem(),
                item("app.preferences")
        );
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

    private javafx.scene.control.MenuItem item(String commandId) {
        return DelosMenus.item(commandRegistry, commandId);
    }
}
