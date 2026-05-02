package io.github.ggeorg.delos.writer.ui;

import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.command.EditorCommand;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class TitleBar extends HBox {
    private final CommandRegistry commandRegistry;
    private final MenuButton hamburgerMenu;
    private final Label appNameLabel;
    private final Label documentTitleLabel;
    private final TextField documentTitleEditor;
    private final Label dirtyIndicatorLabel;
    private final Button undoButton;
    private final Button redoButton;
    private final ToggleButton inspectorToggle;
    private final StackPane titleEditorHost;
    private final List<CommandBoundMenuItem> menuItems = new ArrayList<>();
    private Consumer<String> documentTitleCommitHandler = title -> { };

    public TitleBar(CommandRegistry commandRegistry) {
        this.commandRegistry = Objects.requireNonNull(commandRegistry, "commandRegistry");
        getStyleClass().add("title-bar");
        setPadding(new Insets(4, 8, 4, 8));
        setSpacing(8);

        hamburgerMenu = new MenuButton("☰");
        hamburgerMenu.getStyleClass().add("hamburger-menu");
        hamburgerMenu.setFocusTraversable(false);
        populateHamburgerMenu();
        hamburgerMenu.setOnShowing(event -> refreshFromCommands());

        appNameLabel = new Label("Delos Writer");
        appNameLabel.getStyleClass().add("app-name");

        documentTitleLabel = new Label("Untitled");
        documentTitleLabel.getStyleClass().add("document-title");
        documentTitleLabel.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                beginTitleEdit();
            }
        });

        documentTitleEditor = new TextField();
        documentTitleEditor.getStyleClass().add("document-title-editor");
        documentTitleEditor.setManaged(false);
        documentTitleEditor.setVisible(false);
        documentTitleEditor.setOnAction(event -> commitTitleEdit());
        documentTitleEditor.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused && documentTitleEditor.isVisible()) {
                commitTitleEdit();
            }
        });
        documentTitleEditor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                cancelTitleEdit();
                event.consume();
            }
        });

        titleEditorHost = new StackPane(documentTitleLabel, documentTitleEditor);
        titleEditorHost.setMinWidth(120);
        titleEditorHost.setPrefWidth(180);
        titleEditorHost.setMaxWidth(280);
        StackPane.setAlignment(documentTitleLabel, javafx.geometry.Pos.CENTER_LEFT);
        StackPane.setAlignment(documentTitleEditor, javafx.geometry.Pos.CENTER_LEFT);

        dirtyIndicatorLabel = new Label();
        dirtyIndicatorLabel.getStyleClass().add("dirty-indicator");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        undoButton = createCommandButton("edit.undo", "⟲", false);
        undoButton.getStyleClass().add("titlebar-action-button");
        undoButton.setAccessibleText("Undo");

        redoButton = createCommandButton("edit.redo", "⟳", false);
        redoButton.getStyleClass().add("titlebar-action-button");
        redoButton.setAccessibleText("Redo");

        inspectorToggle = new ToggleButton("⚙");
        inspectorToggle.getStyleClass().add("titlebar-action-button");
        inspectorToggle.getStyleClass().add("inspector-toggle");
        inspectorToggle.setFocusTraversable(false);
        inspectorToggle.setAccessibleText("Toggle Inspector");
        inspectorToggle.setOnAction(event -> executeCommand("view.toggleInspector"));

        getChildren().addAll(
                hamburgerMenu,
                appNameLabel,
                titleEditorHost,
                dirtyIndicatorLabel,
                spacer,
                undoButton,
                redoButton,
                inspectorToggle
        );
        refreshFromCommands();
    }

    public void setDocumentTitle(String title) {
        String normalized = title == null || title.isBlank() ? "Untitled" : title;
        documentTitleLabel.setText(normalized);
        if (!documentTitleEditor.isFocused()) {
            documentTitleEditor.setText(normalized);
        }
    }

    public void setDirty(boolean dirty) {
        dirtyIndicatorLabel.setText(dirty ? "*" : "");
        dirtyIndicatorLabel.setManaged(dirty);
        dirtyIndicatorLabel.setVisible(dirty);
    }

    public void setOnDocumentTitleCommitted(Consumer<String> handler) {
        documentTitleCommitHandler = Objects.requireNonNull(handler, "handler");
    }

    public void refreshFromCommands() {
        refreshButton(undoButton, "edit.undo");
        refreshButton(redoButton, "edit.redo");
        commandRegistry.byId("view.toggleInspector").ifPresentOrElse(
                command -> {
                    inspectorToggle.setDisable(!command.isEnabled());
                    inspectorToggle.setSelected(command.isActive());
                },
                () -> {
                    inspectorToggle.setDisable(true);
                    inspectorToggle.setSelected(false);
                }
        );
        menuItems.forEach(CommandBoundMenuItem::refresh);
    }

    Label documentTitleLabel() {
        return documentTitleLabel;
    }

    Label dirtyIndicatorLabel() {
        return dirtyIndicatorLabel;
    }

    ToggleButton inspectorToggle() {
        return inspectorToggle;
    }

    TextField documentTitleEditor() {
        return documentTitleEditor;
    }

    private void populateHamburgerMenu() {
        hamburgerMenu.getItems().setAll(
                menuItemFor("file.new"),
                menuItemFor("file.open"),
                menuItemFor("file.save"),
                menuItemFor("file.saveAs"),
                new SeparatorMenuItem(),
                menuItemFor("export.html"),
                menuItemFor("export.markdown"),
                new SeparatorMenuItem(),
                menuItemFor("app.about")
        );
    }

    private MenuItem menuItemFor(String commandId) {
        EditorCommand command = commandRegistry.byId(commandId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown command: " + commandId));
        MenuItem item = new MenuItem(command.label());
        item.setAccelerator(command.accelerator());
        item.setOnAction(event -> command.execute());
        CommandBoundMenuItem bound = new CommandBoundMenuItem(command, item);
        menuItems.add(bound);
        bound.refresh();
        return item;
    }

    private Button createCommandButton(String commandId, String fallbackLabel, boolean defaultDisabled) {
        Button button = new Button(fallbackLabel);
        button.setFocusTraversable(false);
        button.setDisable(defaultDisabled);
        commandRegistry.byId(commandId).ifPresent(command -> {
            button.setText(fallbackLabel);
            button.setOnAction(event -> command.execute());
            button.setDisable(!command.isEnabled());
        });
        return button;
    }

    private void refreshButton(Button button, String commandId) {
        commandRegistry.byId(commandId).ifPresentOrElse(
                command -> button.setDisable(!command.isEnabled()),
                () -> button.setDisable(true)
        );
    }

    private void executeCommand(String commandId) {
        commandRegistry.byId(commandId).ifPresent(EditorCommand::execute);
        refreshFromCommands();
    }

    void beginTitleEdit() {
        documentTitleEditor.setText(documentTitleLabel.getText());
        documentTitleLabel.setManaged(false);
        documentTitleLabel.setVisible(false);
        documentTitleEditor.setManaged(true);
        documentTitleEditor.setVisible(true);
        documentTitleEditor.selectAll();
        documentTitleEditor.requestFocus();
    }

    void commitTitleEdit() {
        String normalized = documentTitleEditor.getText() == null ? "" : documentTitleEditor.getText().trim();
        if (!normalized.isEmpty() && !normalized.equals(documentTitleLabel.getText())) {
            documentTitleCommitHandler.accept(normalized);
        }
        endTitleEdit();
    }

    private void cancelTitleEdit() {
        documentTitleEditor.setText(documentTitleLabel.getText());
        endTitleEdit();
    }

    private void endTitleEdit() {
        documentTitleEditor.setManaged(false);
        documentTitleEditor.setVisible(false);
        documentTitleLabel.setManaged(true);
        documentTitleLabel.setVisible(true);
    }

    private record CommandBoundMenuItem(EditorCommand command, MenuItem menuItem) {
        private void refresh() {
            menuItem.setDisable(!command.isEnabled());
        }
    }
}
