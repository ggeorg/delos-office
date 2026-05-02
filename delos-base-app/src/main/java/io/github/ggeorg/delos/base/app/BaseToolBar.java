package io.github.ggeorg.delos.base.app;

import io.github.ggeorg.delos.javafx.chrome.DelosToolBars;
import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.icon.DelosIconId;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;

import java.util.Objects;

final class BaseToolBar extends ToolBar {
    private final CommandRegistry commandRegistry;

    BaseToolBar(CommandRegistry commandRegistry) {
        this.commandRegistry = Objects.requireNonNull(commandRegistry, "commandRegistry");
        DelosToolBars.configure(this, "base-toolbar");
        getItems().setAll(
                button("file.new", DelosIconId.NEW, "New"),
                button("file.open", DelosIconId.OPEN, "Open"),
                button("file.save", DelosIconId.SAVE, "Save"),
                separator(),
                button("insert.table", DelosIconId.TABLE, "Table"),
                button("insert.query", DelosIconId.QUERY, "Query"),
                button("insert.form", DelosIconId.FORM, "Form"),
                button("insert.report", DelosIconId.REPORT, "Report"),
                separator(),
                button("tools.sqlConsole", DelosIconId.SQL, "SQL"),
                button("tools.relationships", DelosIconId.RELATIONSHIPS, "Relations")
        );
        refreshFromCommands();
    }

    void refreshFromCommands() {
        DelosToolBars.refresh(this);
    }

    private Node button(String commandId, DelosIconId iconId, String displayText) {
        Node button = DelosToolBars.button(commandRegistry, commandId, iconId, displayText);
        button.getStyleClass().add("base-toolbar-button");
        return button;
    }

    private static Separator separator() {
        return DelosToolBars.separator();
    }
}
