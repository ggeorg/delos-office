package io.github.ggeorg.delos.javafx.chrome;

import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.command.EditorCommand;
import io.github.ggeorg.delos.javafx.icon.DelosIconId;
import io.github.ggeorg.delos.javafx.icon.DelosIcons;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;

import java.util.Objects;

/** Shared toolbar helpers for Delos application chrome. */
public final class DelosToolBars {
    private DelosToolBars() {
    }

    public static void configure(ToolBar toolBar, String appStyleClass) {
        Objects.requireNonNull(toolBar, "toolBar");
        toolBar.getStyleClass().add("delos-toolbar");
        if (appStyleClass != null && !appStyleClass.isBlank()) {
            toolBar.getStyleClass().add(appStyleClass);
        }
        toolBar.setPadding(new Insets(5, 10, 5, 10));
    }

    public static Button button(CommandRegistry registry, String commandId, DelosIconId iconId, String displayText) {
        EditorCommand command = command(registry, commandId);
        Button button = new Button(label(displayText, command));
        configureCommandControl(button, command, iconId);
        return button;
    }

    public static ToggleButton toggleButton(CommandRegistry registry, String commandId, DelosIconId iconId, String displayText) {
        EditorCommand command = command(registry, commandId);
        ToggleButton button = new ToggleButton(label(displayText, command));
        configureCommandControl(button, command, iconId);
        return button;
    }

    public static Separator separator() {
        Separator separator = new Separator();
        separator.getStyleClass().add("delos-toolbar-separator");
        return separator;
    }

    public static void refresh(ToolBar toolBar) {
        Objects.requireNonNull(toolBar, "toolBar");
        toolBar.getItems().forEach(DelosToolBars::refreshNode);
    }

    private static void configureCommandControl(Control control, EditorCommand command, DelosIconId iconId) {
        control.setUserData(command);
        control.getStyleClass().add("delos-toolbar-button");
        control.setFocusTraversable(false);
        control.setAccessibleText(command.label());
        control.setTooltip(new Tooltip(tooltipText(command)));
        if (control instanceof ButtonBase button) {
            if (iconId != null) {
                button.setGraphic(DelosIcons.toolbarIcon(iconId));
                button.setContentDisplay(ContentDisplay.LEFT);
                button.setGraphicTextGap(5);
            }
            button.setOnAction(event -> {
                command.execute();
                refreshNode(button);
            });
        }
        refreshNode(control);
    }

    private static void refreshNode(Node node) {
        Object userData = node.getUserData();
        if (userData instanceof EditorCommand command) {
            node.setDisable(!command.isEnabled());
            if (node instanceof ToggleButton toggleButton) {
                toggleButton.setSelected(command.isActive());
            }
        }
        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(DelosToolBars::refreshNode);
        }
    }

    private static EditorCommand command(CommandRegistry registry, String commandId) {
        Objects.requireNonNull(registry, "registry");
        return registry.byId(commandId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown command: " + commandId));
    }

    private static String label(String displayText, EditorCommand command) {
        return displayText == null || displayText.isBlank() ? command.label() : displayText;
    }

    private static String tooltipText(EditorCommand command) {
        String acceleratorText = command.acceleratorText();
        return acceleratorText == null || acceleratorText.isBlank()
                ? command.label()
                : command.label() + " (" + acceleratorText + ")";
    }
}
