package io.github.ggeorg.delos.javafx.command;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;

public final class CommandPalette extends VBox {
    private final CommandRegistry registry;
    private final TextField searchField;
    private final ListView<EditorCommand> resultList;
    private final Label countLabel;
    private Runnable onCloseRequested = () -> { };
    private Runnable onCommandExecuted = () -> { };

    public CommandPalette(CommandRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
        getStyleClass().add("command-palette");
        setManaged(false);
        setVisible(false);
        setMaxWidth(480);
        setPrefWidth(480);
        setPadding(new Insets(4));
        setSpacing(4);

        searchField = new TextField();
        searchField.getStyleClass().add("palette-search");
        searchField.setPromptText("Type a command...");
        searchField.textProperty().addListener((obs, oldValue, newValue) -> updateResults(newValue));
        searchField.setOnKeyPressed(this::handleSearchKey);

        resultList = new ListView<>();
        resultList.getStyleClass().add("palette-results");
        resultList.setCellFactory(list -> new CommandCell());
        resultList.setFixedCellSize(32);
        resultList.setPrefHeight(224);
        resultList.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 2) {
                executeSelected();
            }
        });
        resultList.addEventFilter(KeyEvent.KEY_PRESSED, this::redirectPrintableListKeysToSearch);
        resultList.setOnKeyPressed(this::handleListKey);

        countLabel = new Label();
        countLabel.getStyleClass().add("palette-count");

        getChildren().addAll(searchField, resultList, countLabel);
        updateResults("");
    }

    public void setOnCloseRequested(Runnable onCloseRequested) { this.onCloseRequested = Objects.requireNonNull(onCloseRequested); }
    public void setOnCommandExecuted(Runnable onCommandExecuted) { this.onCommandExecuted = Objects.requireNonNull(onCommandExecuted); }

    public void showPalette() {
        setVisible(true);
        setManaged(true);
        searchField.clear();
        updateResults("");
        searchField.requestFocus();
    }

    public void hidePalette() {
        setVisible(false);
        setManaged(false);
    }

    public boolean isOpen() { return isVisible(); }

    private void updateResults(String query) {
        List<EditorCommand> matches = registry.search(query);
        resultList.getItems().setAll(matches);
        if (!matches.isEmpty()) {
            resultList.getSelectionModel().selectFirst();
        }
        countLabel.setText(matches.size() + " of " + registry.all().size() + " commands");
    }

    private void handleSearchKey(KeyEvent event) {
        switch (event.getCode()) {
            case ESCAPE -> { onCloseRequested.run(); event.consume(); }
            case ENTER -> { executeSelected(); event.consume(); }
            case DOWN -> {
                if (!resultList.getItems().isEmpty()) {
                    resultList.requestFocus();
                    int next = Math.min(resultList.getItems().size() - 1, Math.max(0, resultList.getSelectionModel().getSelectedIndex() + 1));
                    resultList.getSelectionModel().select(next);
                    resultList.scrollTo(next);
                    event.consume();
                }
            }
            case UP -> {
                if (!resultList.getItems().isEmpty()) {
                    resultList.requestFocus();
                    int current = resultList.getSelectionModel().getSelectedIndex();
                    int previous = current <= 0 ? 0 : current - 1;
                    resultList.getSelectionModel().select(previous);
                    resultList.scrollTo(previous);
                    event.consume();
                }
            }
            default -> { }
        }
    }

    private void handleListKey(KeyEvent event) {
        switch (event.getCode()) {
            case ESCAPE -> { onCloseRequested.run(); event.consume(); }
            case ENTER -> { executeSelected(); event.consume(); }
            default -> { }
        }
    }

    private void redirectPrintableListKeysToSearch(KeyEvent event) {
        if (isListNavigationKey(event.getCode()) || event.isShortcutDown() || event.isAltDown()) {
            return;
        }

        String text = event.getText();
        if (text == null || text.isEmpty() || Character.isISOControl(text.charAt(0))) {
            return;
        }

        searchField.requestFocus();
        searchField.appendText(text);
        event.consume();
    }

    private boolean isListNavigationKey(KeyCode code) {
        return code == KeyCode.UP
                || code == KeyCode.DOWN
                || code == KeyCode.PAGE_UP
                || code == KeyCode.PAGE_DOWN
                || code == KeyCode.HOME
                || code == KeyCode.END
                || code == KeyCode.ENTER
                || code == KeyCode.ESCAPE;
    }

    private void executeSelected() {
        EditorCommand selected = resultList.getSelectionModel().getSelectedItem();
        if (selected == null || !selected.isEnabled()) {
            return;
        }
        selected.execute();
        onCommandExecuted.run();
    }

    public TextField searchField() { return searchField; }
    public ListView<EditorCommand> resultList() { return resultList; }
    public Label countLabel() { return countLabel; }
    void applyQuery(String query) { searchField.setText(query == null ? "" : query); updateResults(searchField.getText()); }

    private final class CommandCell extends ListCell<EditorCommand> {
        private final HBox root = new HBox(8);
        private final Label label = new Label();
        private final Region spacer = new Region();
        private final Label shortcut = new Label();

        private CommandCell() {
            root.getStyleClass().add("palette-item");
            label.getStyleClass().add("palette-item-label");
            shortcut.getStyleClass().add("palette-item-shortcut");
            HBox.setHgrow(spacer, Priority.ALWAYS);
            root.getChildren().addAll(label, spacer, shortcut);
        }

        @Override protected void updateItem(EditorCommand item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null); setText(null); return;
            }
            label.setText(item.label());
            shortcut.setText(item.acceleratorText());
            root.setOpacity(item.isEnabled() ? 1.0 : 0.45);
            setGraphic(root);
            setText(null);
        }
    }
}
