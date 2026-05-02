package io.github.ggeorg.delos.writer.app;

import io.github.ggeorg.delos.javafx.chrome.DelosToolBars;
import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.command.EditorCommand;
import io.github.ggeorg.delos.javafx.icon.DelosIconId;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;

import java.util.Objects;

final class WriterToolBar extends ToolBar {
    private final CommandRegistry commandRegistry;
    private final ZoomPresetPicker zoomPresetPicker;

    WriterToolBar(CommandRegistry commandRegistry) {
        this.commandRegistry = Objects.requireNonNull(commandRegistry, "commandRegistry");
        DelosToolBars.configure(this, "writer-toolbar");
        zoomPresetPicker = new ZoomPresetPicker(commandRegistry);
        getItems().setAll(
                button("edit.undo", DelosIconId.UNDO, "Undo"),
                button("edit.redo", DelosIconId.REDO, "Redo"),
                button("file.print", DelosIconId.PRINT, "Print"),
                separator(),
                zoomPresetPicker,
                separator(),
                button("insert.image", DelosIconId.IMAGE, "Image"),
                button("insert.table", DelosIconId.TABLE, "Table"),
                button("insert.formula", DelosIconId.FORMULA, "Formula"),
                separator(),
                toggleButton("view.toggleRuler", DelosIconId.RULER, "Ruler"),
                toggleButton("view.toggleInspector", DelosIconId.INSPECTOR, "Inspector")
        );
        refreshFromCommands();
    }

    void refreshFromCommands() {
        DelosToolBars.refresh(this);
        zoomPresetPicker.refreshFromCommands();
    }

    private Node button(String commandId, DelosIconId iconId, String displayText) {
        Node button = DelosToolBars.button(commandRegistry, commandId, iconId, displayText);
        button.getStyleClass().add("writer-toolbar-button");
        return button;
    }

    private Node toggleButton(String commandId, DelosIconId iconId, String displayText) {
        Node button = DelosToolBars.toggleButton(commandRegistry, commandId, iconId, displayText);
        button.getStyleClass().add("writer-toolbar-button");
        return button;
    }

    private static Separator separator() {
        Separator separator = DelosToolBars.separator();
        separator.getStyleClass().add("writer-toolbar-separator");
        return separator;
    }

    private static final class ZoomPresetPicker extends ComboBox<ZoomPreset> {
        private final CommandRegistry commandRegistry;
        private boolean refreshing;

        ZoomPresetPicker(CommandRegistry commandRegistry) {
            super(FXCollections.observableArrayList(
                    new ZoomPreset("view.zoomFitWidth", "Fit"),
                    new ZoomPreset("view.zoom50", "50%"),
                    new ZoomPreset("view.zoom75", "75%"),
                    new ZoomPreset("view.zoom90", "90%"),
                    new ZoomPreset("view.zoom100", "100%"),
                    new ZoomPreset("view.zoom125", "125%"),
                    new ZoomPreset("view.zoom150", "150%"),
                    new ZoomPreset("view.zoom200", "200%")
            ));
            this.commandRegistry = Objects.requireNonNull(commandRegistry, "commandRegistry");
            getStyleClass().add("writer-toolbar-zoom-combo");
            setFocusTraversable(false);
            setTooltip(new Tooltip("Zoom"));
            valueProperty().addListener((obs, oldValue, newValue) -> {
                if (refreshing || newValue == null) {
                    return;
                }
                commandRegistry.byId(newValue.commandId()).ifPresent(EditorCommand::execute);
                refreshFromCommands();
            });
        }

        void refreshFromCommands() {
            refreshing = true;
            try {
                getItems().stream()
                        .filter(preset -> commandRegistry.byId(preset.commandId()).map(EditorCommand::isActive).orElse(false))
                        .findFirst()
                        .ifPresentOrElse(getSelectionModel()::select, () -> getSelectionModel().clearSelection());
            } finally {
                refreshing = false;
            }
        }
    }

    private record ZoomPreset(String commandId, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
