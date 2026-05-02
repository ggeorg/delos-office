package io.github.ggeorg.delos.writer.app.inspector;

import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.command.EditorCommand;
import io.github.ggeorg.delos.javafx.icon.DelosIconId;
import io.github.ggeorg.delos.javafx.icon.DelosIconSize;
import io.github.ggeorg.delos.javafx.icon.DelosIcons;
import io.github.ggeorg.delos.javafx.inspector.InspectorSection;
import io.github.ggeorg.delos.writer.ui.control.DelosEditor;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** First useful Writer format inspector: text emphasis, paragraph alignment, and simple lists. */
final class WriterTextFormatInspector extends VBox {
    private final DelosEditor editor;
    private final CommandRegistry commandRegistry;
    private final List<ToggleButton> commandButtons = new ArrayList<>();

    WriterTextFormatInspector(DelosEditor editor, CommandRegistry commandRegistry) {
        this.editor = Objects.requireNonNull(editor, "editor");
        this.commandRegistry = Objects.requireNonNull(commandRegistry, "commandRegistry");

        getStyleClass().add("writer-text-format-inspector");
        setPadding(new Insets(12, 14, 18, 14));
        setSpacing(10);

        getChildren().setAll(textSection(), paragraphSection(), listsSection());
    }

    void refresh() {
        for (ToggleButton button : commandButtons) {
            Object userData = button.getUserData();
            if (userData instanceof EditorCommand command) {
                button.setDisable(!command.isEnabled());
                button.setSelected(command.isActive());
            }
        }
    }

    private Node textSection() {
        InspectorSection section = new InspectorSection("Text");
        section.addAll(
                InspectorSection.row("Style", disabledCombo("Normal text")),
                InspectorSection.row("Font", disabledCombo("Serif")),
                InspectorSection.row("Size", disabledCombo("13")),
                InspectorSection.row("Emphasis", commandStrip(
                        commandButton("format.bold", DelosIconId.BOLD, "B"),
                        commandButton("format.italic", DelosIconId.ITALIC, "I"),
                        commandButton("format.underline", DelosIconId.UNDERLINE, "U"),
                        commandButton("format.strikethrough", DelosIconId.STRIKETHROUGH, "S")
                ))
        );
        return section;
    }

    private Node paragraphSection() {
        InspectorSection section = new InspectorSection("Paragraph");
        section.addAll(
                InspectorSection.row("Align", commandStrip(
                        commandButton("format.alignLeft", DelosIconId.ALIGN_LEFT, "Left"),
                        commandButton("format.alignCenter", DelosIconId.ALIGN_CENTER, "Center"),
                        commandButton("format.alignRight", DelosIconId.ALIGN_RIGHT, "Right"),
                        commandButton("format.justify", DelosIconId.ALIGN_JUSTIFY, "Justify")
                )),
                InspectorSection.row("Spacing", disabledCombo("Single")),
                InspectorSection.row("Indent", disabledCombo("0 pt"))
        );
        return section;
    }

    private Node listsSection() {
        InspectorSection section = new InspectorSection("Lists");
        section.addAll(
                InspectorSection.row("Type", commandStrip(
                        commandButton("format.bulletedList", DelosIconId.BULLETED_LIST, "Bullets"),
                        commandButton("format.numberedList", DelosIconId.NUMBERED_LIST, "Numbers")
                )),
                InspectorSection.row("Level", commandStrip(
                        commandButton("format.decreaseListLevel", DelosIconId.DECREASE_INDENT, "Outdent"),
                        commandButton("format.increaseListLevel", DelosIconId.INCREASE_INDENT, "Indent")
                ))
        );
        return section;
    }

    private ComboBox<String> disabledCombo(String value) {
        ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(value));
        comboBox.getSelectionModel().select(value);
        comboBox.getStyleClass().add("delos-inspector-combo");
        comboBox.setMaxWidth(Double.MAX_VALUE);
        comboBox.setFocusTraversable(false);
        comboBox.setDisable(true);
        return comboBox;
    }

    private HBox commandStrip(ToggleButton... buttons) {
        HBox strip = new HBox(6, buttons);
        strip.getStyleClass().add("delos-inspector-command-strip");
        for (ToggleButton button : buttons) {
            HBox.setHgrow(button, Priority.ALWAYS);
            button.setMaxWidth(Double.MAX_VALUE);
        }
        return strip;
    }

    private ToggleButton commandButton(String commandId, DelosIconId iconId, String fallbackText) {
        EditorCommand command = commandRegistry.byId(commandId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown command: " + commandId));
        ToggleButton button = new ToggleButton(fallbackText);
        button.getStyleClass().add("delos-inspector-command-button");
        button.setUserData(command);
        button.setFocusTraversable(false);
        button.setAccessibleText(command.label());
        button.setTooltip(new Tooltip(command.label()));
        button.setGraphic(DelosIcons.icon(iconId, DelosIconSize.TOOLBAR));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setOnAction(event -> {
            command.execute();
            editor.focusEditor();
            refresh();
        });
        commandButtons.add(button);
        return button;
    }
}
