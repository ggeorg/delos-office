package io.github.ggeorg.delos.writer.app.inspector;

import io.github.ggeorg.delos.javafx.command.CommandRegistry;
import io.github.ggeorg.delos.javafx.command.EditorCommand;
import io.github.ggeorg.delos.javafx.icon.DelosIconId;
import io.github.ggeorg.delos.javafx.icon.DelosIconSize;
import io.github.ggeorg.delos.javafx.icon.DelosIcons;
import io.github.ggeorg.delos.javafx.inspector.InspectorSection;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.ui.control.DelosEditor;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Report-focused table inspector v1.
 *
 * <p>Every visible control delegates to document-model commands/properties. No
 * merged cells, style gallery, formulas, or floating table layout are exposed in
 * this first pass.</p>
 */
final class WriterTableInspector extends VBox {
    private static final double MIN_WIDTH_PERCENT = 25.0;
    private static final double MAX_WIDTH_PERCENT = 100.0;
    private static final double DEFAULT_WIDTH_PERCENT = 100.0;
    private static final double MIN_CELL_PADDING = 0.0;
    private static final double MAX_CELL_PADDING = 48.0;
    private static final double DEFAULT_CELL_PADDING = 5.0;

    private final DelosEditor editor;
    private final List<Button> commandButtons = new ArrayList<>();
    private final Label emptyState = new Label("Select a table cell to edit rows, columns, and table properties.");
    private final Spinner<Double> widthPercent = tableSpinner(MIN_WIDTH_PERCENT, MAX_WIDTH_PERCENT, DEFAULT_WIDTH_PERCENT, 1.0);
    private final Spinner<Double> cellPadding = tableSpinner(MIN_CELL_PADDING, MAX_CELL_PADDING, DEFAULT_CELL_PADDING, 1.0);
    private final CheckBox headerRow = new CheckBox("Header row");
    private final CheckBox borders = new CheckBox("Borders");
    private final List<Node> tableControls = List.of(widthPercent, cellPadding, headerRow, borders);

    private boolean refreshing;

    WriterTableInspector(DelosEditor editor, CommandRegistry commandRegistry) {
        this.editor = Objects.requireNonNull(editor, "editor");
        Objects.requireNonNull(commandRegistry, "commandRegistry");

        getStyleClass().add("writer-table-inspector");
        setPadding(new Insets(0, 14, 18, 14));
        setSpacing(10);

        configureControls();
        getChildren().setAll(tableSection(commandRegistry), cellSection());
    }

    void refresh() {
        TableBlock selected = editor.selectedTableBlock();
        boolean hasTable = selected != null;

        refreshing = true;
        try {
            emptyState.setManaged(!hasTable);
            emptyState.setVisible(!hasTable);
            for (Button button : commandButtons) {
                Object userData = button.getUserData();
                button.setDisable(!(userData instanceof EditorCommand command) || !command.isEnabled());
            }
            for (Node control : tableControls) {
                control.setDisable(!hasTable);
            }
            if (!hasTable) {
                return;
            }

            setSpinnerValue(widthPercent, selected.style().widthFraction() * 100.0);
            setSpinnerValue(cellPadding, selected.style().cellPadding());
            headerRow.setSelected(selected.headerRowCount() > 0);
            borders.setSelected(selected.style().bordersEnabled());
        } finally {
            refreshing = false;
        }
    }

    boolean hasSelectedTable() {
        return editor.hasSelectedTable();
    }

    private Node tableSection(CommandRegistry commandRegistry) {
        InspectorSection section = new InspectorSection("Table");
        section.addAll(
                emptyState,
                InspectorSection.row("Rows", actionStrip(
                        commandButton(commandRegistry, "table.insertRowAbove", DelosIconId.ROW, "Above"),
                        commandButton(commandRegistry, "table.insertRowBelow", DelosIconId.ROW, "Below"),
                        commandButton(commandRegistry, "table.deleteRow", DelosIconId.DELETE, "Delete")
                )),
                InspectorSection.row("Columns", actionStrip(
                        commandButton(commandRegistry, "table.insertColumnLeft", DelosIconId.COLUMN, "Left"),
                        commandButton(commandRegistry, "table.insertColumnRight", DelosIconId.COLUMN, "Right"),
                        commandButton(commandRegistry, "table.deleteColumn", DelosIconId.DELETE, "Delete")
                )),
                InspectorSection.row("Header", headerRow),
                InspectorSection.row("Width", widthPercent)
        );
        return section;
    }

    private Node cellSection() {
        InspectorSection section = new InspectorSection("Cell");
        section.addAll(
                InspectorSection.row("Padding", cellPadding),
                InspectorSection.row("Border", borders)
        );
        return section;
    }

    private void configureControls() {
        headerRow.getStyleClass().add("delos-inspector-check");
        headerRow.setOnAction(event -> {
            if (refreshing) {
                return;
            }
            editor.setSelectedTableHeaderRow(headerRow.isSelected());
            editor.focusEditor();
            refresh();
        });

        borders.getStyleClass().add("delos-inspector-check");
        borders.setOnAction(event -> commitTableProperties());

        configureSpinner(widthPercent, true);
        configureSpinner(cellPadding, false);
    }

    private void configureSpinner(Spinner<Double> spinner, boolean percent) {
        spinner.getStyleClass().add("delos-inspector-number-field");
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
        spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!refreshing && newValue != null) {
                commitTableProperties();
            }
        });
        spinner.getEditor().setOnAction(event -> commitSpinnerEditor(spinner, percent));
        spinner.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                commitSpinnerEditor(spinner, percent);
            }
        });
    }

    private Button commandButton(CommandRegistry commandRegistry, String commandId, DelosIconId iconId, String fallbackText) {
        EditorCommand command = commandRegistry.byId(commandId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown command: " + commandId));
        Button button = new Button(fallbackText);
        button.getStyleClass().add("delos-inspector-action-button");
        button.setUserData(command);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setTooltip(new Tooltip(command.label()));
        button.setGraphic(DelosIcons.icon(iconId, DelosIconSize.TOOLBAR));
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setOnAction(event -> {
            if (!command.isEnabled()) {
                return;
            }
            command.execute();
            editor.focusEditor();
            refresh();
        });
        commandButtons.add(button);
        return button;
    }

    private HBox actionStrip(Button... buttons) {
        HBox strip = new HBox(6, buttons);
        strip.getStyleClass().add("delos-inspector-command-strip");
        for (Button button : buttons) {
            HBox.setHgrow(button, Priority.ALWAYS);
            button.setMaxWidth(Double.MAX_VALUE);
        }
        return strip;
    }

    private Spinner<Double> tableSpinner(double min, double max, double initialValue, double step) {
        Spinner<Double> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, initialValue, step));
        return spinner;
    }

    private void commitTableProperties() {
        if (refreshing || !editor.hasSelectedTable()) {
            return;
        }
        editor.updateSelectedTableProperties(
                spinnerValue(widthPercent) / 100.0,
                spinnerValue(cellPadding),
                borders.isSelected()
        );
        editor.focusEditor();
    }

    private void commitSpinnerEditor(Spinner<Double> spinner, boolean percent) {
        String text = spinner.getEditor().getText();
        if (text == null || text.isBlank()) {
            setSpinnerValue(spinner, spinnerValue(spinner));
            return;
        }
        String normalized = percent ? text.trim().replace("%", "") : text.trim();
        try {
            setSpinnerValue(spinner, Double.parseDouble(normalized));
        } catch (NumberFormatException ignored) {
            setSpinnerValue(spinner, spinnerValue(spinner));
        }
    }

    private double spinnerValue(Spinner<Double> spinner) {
        Double value = spinner.getValue();
        return value == null ? 0.0 : value;
    }

    private void setSpinnerValue(Spinner<Double> spinner, double value) {
        spinner.getValueFactory().setValue(value);
    }
}
