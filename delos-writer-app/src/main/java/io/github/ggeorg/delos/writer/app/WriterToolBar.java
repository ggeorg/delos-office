package io.github.ggeorg.delos.writer.app;

import io.github.ggeorg.delos.javafx.chrome.DelosMenus;
import io.github.ggeorg.delos.javafx.chrome.DelosToolBars;
import io.github.ggeorg.delos.javafx.chrome.DelosToolbarGroup;
import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.command.EditorCommand;
import io.github.ggeorg.delos.javafx.icon.DelosIconId;
import io.github.ggeorg.delos.javafx.icon.DelosIcons;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ContextMenu;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class WriterToolBar extends ToolBar {
    private final CommandRegistry commandRegistry;
    private final ZoomPresetPicker zoomPresetPicker;
    private final FileTitleButton fileTitleButton;

    WriterToolBar(
            CommandRegistry commandRegistry,
            Consumer<String> selectInspectorTab,
            Supplier<String> selectedInspectorTab,
            Supplier<String> displayName,
            BooleanSupplier dirty,
            Consumer<String> renameDocumentTitle
    ) {
        this.commandRegistry = Objects.requireNonNull(commandRegistry, "commandRegistry");
        Objects.requireNonNull(selectInspectorTab, "selectInspectorTab");
        Objects.requireNonNull(selectedInspectorTab, "selectedInspectorTab");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(dirty, "dirty");
        Objects.requireNonNull(renameDocumentTitle, "renameDocumentTitle");
        DelosToolBars.configure(this, "writer-toolbar");
        zoomPresetPicker = new ZoomPresetPicker(commandRegistry);
        fileTitleButton = new FileTitleButton(commandRegistry, displayName, dirty, renameDocumentTitle);
        getItems().setAll(
                documentCluster(),
                spacer(),
                toolbarGroup(
                        button("edit.undo", DelosIconId.UNDO, "Undo"),
                        button("edit.redo", DelosIconId.REDO, "Redo")
                ),
                toolbarGroup(
                        button("edit.cut", DelosIconId.CUT, "Cut"),
                        button("edit.copy", DelosIconId.COPY, "Copy"),
                        button("edit.paste", DelosIconId.PASTE, "Paste")
                ),
                toolbarGroup(
                        button("insert.image", DelosIconId.IMAGE, "Image"),
                        button("insert.table", DelosIconId.TABLE, "Table"),
                        formulaButton()
                ),
                toolbarGroup(toggleButton("view.toggleRuler", DelosIconId.RULER, "Ruler")),
                toolbarGroup(shareButton()),
                toolbarGroup(zoomPresetPicker),
                toolbarGroup(
                        toggleButton("view.toggleInspector", DelosIconId.INSPECTOR, "Inspector"),
                        pageLayoutButton(selectInspectorTab)
                )
        );
        refreshFromCommands();
    }

    void refreshFromCommands() {
        DelosToolBars.refresh(this);
        zoomPresetPicker.refreshFromCommands();
        fileTitleButton.refresh();
    }

    private Node button(String commandId, DelosIconId iconId, String displayText) {
        Node button = DelosToolBars.button(commandRegistry, commandId, iconId, displayText);
        configureToolbarCommand(button);
        return button;
    }

    private Node toggleButton(String commandId, DelosIconId iconId, String displayText) {
        Node button = DelosToolBars.toggleButton(commandRegistry, commandId, iconId, displayText);
        configureToolbarCommand(button);
        return button;
    }

    private Button shareButton() {
        ContextMenu menu = new ContextMenu(
                menuItem("file.exportPdf"),
                menuItem("export.html"),
                menuItem("export.markdown")
        );

        Button share = localIconButton(DelosIconId.SHARE, "Share / Export");
        share.getStyleClass().add("writer-toolbar-share-button");
        share.setOnAction(event -> {
            if (menu.isShowing()) {
                menu.hide();
            } else {
                menu.show(share, javafx.geometry.Side.BOTTOM, 0.0, 2.0);
            }
        });
        return share;
    }

    private MenuItem menuItem(String commandId) {
        return DelosMenus.item(commandRegistry, commandId);
    }

    private Button localIconButton(DelosIconId iconId, String tooltip) {
        Button button = new Button();
        button.getStyleClass().addAll("writer-toolbar-button", "writer-toolbar-local-button");
        button.setFocusTraversable(false);
        button.setTooltip(new Tooltip(tooltip));
        button.setGraphic(DelosIcons.toolbarIcon(iconId));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setMinSize(34.0, 34.0);
        button.setPrefSize(34.0, 34.0);
        button.setMaxSize(34.0, 34.0);
        return button;
    }

    private Button pageLayoutButton(Consumer<String> selectInspectorTab) {
        Button button = localIconButton(DelosIconId.PAGE_LAYOUT, "Page Layout");
        button.setOnAction(event -> selectInspectorTab.accept("layout"));
        return button;
    }

    private Node documentCluster() {
        HBox cluster = new HBox(10.0, toolbarGroup(localIconButton(DelosIconId.LEFT_SIDEBAR, "Navigation sidebar")), fileTitleButton);
        cluster.getStyleClass().add("writer-toolbar-document-cluster");
        cluster.setAlignment(Pos.CENTER_LEFT);
        return cluster;
    }

    private Button formulaButton() {
        EditorCommand command = commandRegistry.byId("insert.formula")
                .orElseThrow(() -> new IllegalArgumentException("Unknown command: insert.formula"));
        Button button = new Button("fx");
        button.setUserData(command);
        button.getStyleClass().addAll("writer-toolbar-button", "writer-toolbar-fx-button");
        button.setFocusTraversable(false);
        button.setAccessibleText(command.label());
        button.setTooltip(new Tooltip(command.label()));
        button.setOnAction(event -> {
            command.execute();
            refreshFromCommands();
        });
        button.setContentDisplay(ContentDisplay.TEXT_ONLY);
        button.setMinSize(34.0, 34.0);
        button.setPrefSize(34.0, 34.0);
        button.setMaxSize(34.0, 34.0);
        return button;
    }

    private static DelosToolbarGroup toolbarGroup(Node... nodes) {
        DelosToolbarGroup group = new DelosToolbarGroup(nodes);
        group.getStyleClass().add("writer-toolbar-button-group");
        return group;
    }

    private static void configureToolbarCommand(Node node) {
        node.getStyleClass().add("writer-toolbar-button");
        if (node instanceof ButtonBase button) {
            button.setText("");
            button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            button.setMinSize(34.0, 34.0);
            button.setPrefSize(34.0, 34.0);
            button.setMaxSize(34.0, 34.0);
        }
    }

    private static Region spacer() {
        Region spacer = new Region();
        spacer.getStyleClass().add("writer-toolbar-spacer");
        HBoxGrow.setAlways(spacer);
        return spacer;
    }

    /** Small adapter keeps the JavaFX static call out of the main toolbar list. */
    private static final class HBoxGrow {
        private static void setAlways(Region node) {
            javafx.scene.layout.HBox.setHgrow(node, Priority.ALWAYS);
        }
    }

    private final class FileTitleButton extends Button {
        private final Supplier<String> displayName;
        private final BooleanSupplier dirty;
        private final Label title = new Label();

        FileTitleButton(
                CommandRegistry commandRegistry,
                Supplier<String> displayName,
                BooleanSupplier dirty,
                Consumer<String> renameDocumentTitle
        ) {
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            this.dirty = Objects.requireNonNull(dirty, "dirty");
            Objects.requireNonNull(renameDocumentTitle, "renameDocumentTitle");
            getStyleClass().add("writer-toolbar-file-title-button");
            setFocusTraversable(false);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setTooltip(new Tooltip("Save As…"));

            title.getStyleClass().add("writer-toolbar-file-title");
            title.setMaxWidth(360.0);
            title.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
            setGraphic(title);
            setOnAction(event -> commandRegistry.byId("file.saveAs").ifPresent(EditorCommand::execute));
            refresh();
        }

        void refresh() {
            String name = normalizedDisplayName();
            title.setText(dirty.getAsBoolean() ? name + " *" : name);
        }

        private String normalizedDisplayName() {
            return normalizeTitleName(displayName.get(), "Untitled");
        }

        private static String normalizeTitleName(String value, String fallback) {
            String normalized = value == null ? "" : value.trim();
            if (normalized.isEmpty()) {
                normalized = fallback == null || fallback.isBlank() ? "Untitled" : fallback.trim();
            }
            return stripWriterExtension(normalized);
        }

        private static String stripWriterExtension(String value) {
            String extension = ".dlw";
            return value.length() > extension.length()
                    && value.toLowerCase(java.util.Locale.ROOT).endsWith(extension)
                    ? value.substring(0, value.length() - extension.length())
                    : value;
        }
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
