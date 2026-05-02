package io.github.ggeorg.delos.calc.app;

import io.github.ggeorg.delos.javafx.chrome.DelosToolBars;
import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.icon.DelosIconId;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;

import java.util.Objects;

/** Spreadsheet toolbar foundation using the shared Delos JavaFX toolbar helpers. */
final class CalcToolBar extends ToolBar {
    private final CommandRegistry commandRegistry;

    CalcToolBar(CommandRegistry commandRegistry) {
        this.commandRegistry = Objects.requireNonNull(commandRegistry, "commandRegistry");
        DelosToolBars.configure(this, "calc-toolbar");
        getItems().setAll(
                button("file.new", DelosIconId.NEW, "New"),
                button("file.open", DelosIconId.OPEN, "Open"),
                button("file.save", DelosIconId.SAVE, "Save"),
                button("file.print", DelosIconId.PRINT, "Print"),
                separator(),
                button("edit.cut", DelosIconId.CUT, "Cut"),
                button("edit.copy", DelosIconId.COPY, "Copy"),
                button("edit.paste", DelosIconId.PASTE, "Paste"),
                button("edit.clearContents", DelosIconId.CLEAR, "Clear"),
                separator(),
                nameBox(),
                formulaButton(),
                formulaField(),
                separator(),
                numberFormatPicker(),
                button("format.bold", DelosIconId.BOLD, "B"),
                button("format.italic", DelosIconId.ITALIC, "I"),
                button("format.textColor", DelosIconId.TEXT_COLOR, "A")
        );
        refreshFromCommands();
    }

    void refreshFromCommands() {
        DelosToolBars.refresh(this);
    }

    private Node button(String commandId, DelosIconId iconId, String displayText) {
        Node button = DelosToolBars.button(commandRegistry, commandId, iconId, displayText);
        button.getStyleClass().add("calc-toolbar-button");
        return button;
    }

    private static Separator separator() {
        Separator separator = DelosToolBars.separator();
        separator.getStyleClass().add("calc-toolbar-separator");
        return separator;
    }

    private Node formulaButton() {
        Button button = new Button("fx");
        button.getStyleClass().addAll("delos-toolbar-button", "calc-toolbar-button");
        button.setTooltip(new Tooltip("Function insertion is coming next."));
        button.setDisable(true);
        button.setFocusTraversable(false);
        return button;
    }

    private static Node nameBox() {
        TextField nameBox = new TextField("A1");
        nameBox.getStyleClass().add("calc-name-box");
        nameBox.setFocusTraversable(false);
        nameBox.setDisable(true);
        nameBox.setTooltip(new Tooltip("Name box support is coming next."));
        return nameBox;
    }

    private static Node formulaField() {
        TextField field = new TextField();
        field.getStyleClass().add("calc-formula-field");
        field.setPromptText("Formula bar coming next");
        field.setFocusTraversable(false);
        field.setDisable(true);
        return field;
    }

    private static Node numberFormatPicker() {
        ComboBox<String> picker = new ComboBox<>(FXCollections.observableArrayList("General", "Number", "Currency", "Percent", "Date"));
        picker.getSelectionModel().selectFirst();
        picker.getStyleClass().add("calc-toolbar-combo");
        picker.setFocusTraversable(false);
        picker.setTooltip(new Tooltip("Number formatting support is coming next."));
        picker.setDisable(true);
        return picker;
    }
}
