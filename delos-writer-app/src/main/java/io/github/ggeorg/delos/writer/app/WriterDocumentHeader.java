package io.github.ggeorg.delos.writer.app;

import io.github.ggeorg.delos.javafx.chrome.DelosToolBars;
import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.command.EditorCommand;
import io.github.ggeorg.delos.javafx.icon.DelosIconId;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.util.Objects;

/**
 * Delos-owned document header below native OS chrome.
 *
 * <p>This is intentionally not a custom title bar: it does not move, resize,
 * decorate, or otherwise replace the native platform window frame.</p>
 */
final class WriterDocumentHeader extends BorderPane {
    private final HBox commandBox = new HBox(4);
    private final Label title = new Label();

    WriterDocumentHeader(CommandRegistry commandRegistry) {
        Objects.requireNonNull(commandRegistry, "commandRegistry");

        getStyleClass().add("writer-document-header");
        setMinHeight(32.0);
        setPrefHeight(32.0);
        setMaxHeight(32.0);

        Label brand = new Label("Delos Writer");
        brand.getStyleClass().add("writer-document-header-brand");
        title.getStyleClass().add("writer-document-header-title");

        HBox titleBox = new HBox(6, brand, title);
        titleBox.getStyleClass().add("writer-document-header-title-box");
        titleBox.setAlignment(Pos.CENTER_LEFT);

        commandBox.getStyleClass().add("writer-document-header-actions");
        commandBox.setAlignment(Pos.CENTER_RIGHT);
        commandBox.getChildren().setAll(
                headerButton(commandRegistry, "edit.undo", DelosIconId.UNDO, "Undo"),
                headerButton(commandRegistry, "edit.redo", DelosIconId.REDO, "Redo"),
                headerToggle(commandRegistry, "view.toggleInspector", DelosIconId.INSPECTOR, "Inspector")
        );

        setLeft(titleBox);
        setRight(commandBox);
    }

    void refresh(String displayName, boolean dirty) {
        String safeDisplayName = displayName == null || displayName.isBlank() ? "Untitled" : displayName;
        title.setText("— " + safeDisplayName + (dirty ? " *" : ""));
        commandBox.getChildren().forEach(WriterDocumentHeader::refreshCommandNode);
    }

    private static void refreshCommandNode(Node node) {
        Object userData = node.getUserData();
        if (userData instanceof EditorCommand command) {
            node.setDisable(!command.isEnabled());
            if (node instanceof ToggleButton toggleButton) {
                toggleButton.setSelected(command.isActive());
            }
        }
        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(WriterDocumentHeader::refreshCommandNode);
        }
    }

    private static Node headerButton(CommandRegistry commandRegistry, String commandId, DelosIconId iconId, String displayText) {
        Node button = DelosToolBars.button(commandRegistry, commandId, iconId, displayText);
        configureHeaderCommand(button);
        return button;
    }

    private static Node headerToggle(CommandRegistry commandRegistry, String commandId, DelosIconId iconId, String displayText) {
        Node button = DelosToolBars.toggleButton(commandRegistry, commandId, iconId, displayText);
        configureHeaderCommand(button);
        return button;
    }

    private static void configureHeaderCommand(Node node) {
        node.getStyleClass().add("writer-document-header-button");
        if (node instanceof ButtonBase button) {
            button.setText("");
            button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            button.setMinSize(26.0, 26.0);
            button.setPrefSize(26.0, 26.0);
            button.setMaxSize(26.0, 26.0);
        }
    }
}
