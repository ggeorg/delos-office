package io.github.ggeorg.delos.calc.ui.control;

import io.github.ggeorg.delos.calc.core.CellAddress;
import io.github.ggeorg.delos.calc.core.CellSelection;
import io.github.ggeorg.delos.calc.core.Sheet;
import io.github.ggeorg.delos.calc.core.Workbook;
import io.github.ggeorg.delos.calc.core.WorkbookEditor;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

import java.util.Objects;

/**
 * Minimal JavaFX spreadsheet component backed by the immutable Calc core model.
 */
public final class DelosSpreadsheet extends BorderPane {
    private static final int DEFAULT_VISIBLE_ROWS = 50;
    private static final int DEFAULT_VISIBLE_COLUMNS = 26;
    private static final double ROW_HEADER_WIDTH = 48.0;
    private static final double CELL_WIDTH = 112.0;
    private static final double CELL_HEIGHT = 28.0;

    private final ObjectProperty<Workbook> workbook = new SimpleObjectProperty<>(this, "workbook", Workbook.blank());
    private final StringProperty activeSheetName = new SimpleStringProperty(this, "activeSheetName", Workbook.blank().firstSheet().name());
    private final ObjectProperty<CellSelection> selection = new SimpleObjectProperty<>(
            this,
            "selection",
            CellSelection.single(Workbook.blank().firstSheet().name(), CellAddress.of(0, 0))
    );
    private final ObjectProperty<CellAddress> selectedCell = new SimpleObjectProperty<>(this, "selectedCell", CellAddress.of(0, 0));
    private final int visibleRows;
    private final int visibleColumns;
    private final GridPane grid = new GridPane();
    private boolean refreshing;

    public DelosSpreadsheet() {
        this(Workbook.blank(), DEFAULT_VISIBLE_ROWS, DEFAULT_VISIBLE_COLUMNS);
    }

    public DelosSpreadsheet(Workbook workbook) {
        this(workbook, DEFAULT_VISIBLE_ROWS, DEFAULT_VISIBLE_COLUMNS);
    }

    public DelosSpreadsheet(Workbook workbook, int visibleRows, int visibleColumns) {
        this.visibleRows = positive(visibleRows, "visibleRows");
        this.visibleColumns = positive(visibleColumns, "visibleColumns");
        Workbook initialWorkbook = Objects.requireNonNullElseGet(workbook, Workbook::blank);
        this.workbook.set(initialWorkbook);
        this.activeSheetName.set(initialWorkbook.firstSheet().name());
        this.selection.set(CellSelection.single(initialWorkbook.firstSheet().name(), CellAddress.of(0, 0)));

        getStyleClass().add("delos-spreadsheet");
        configureGrid();

        ScrollPane scroller = new ScrollPane(grid);
        scroller.setFitToWidth(false);
        scroller.setFitToHeight(false);
        scroller.getStyleClass().add("delos-spreadsheet-scroll-pane");
        setCenter(scroller);

        this.workbook.addListener((ignored, oldWorkbook, newWorkbook) -> {
            ensureActiveSheetExists(newWorkbook);
            refreshGrid();
        });
        this.activeSheetName.addListener((ignored, oldSheetName, newSheetName) -> {
            selection.set(CellSelection.single(newSheetName, selectedCell.get()));
            refreshGrid();
        });
        this.selection.addListener((ignored, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedCell.set(newSelection.focus());
            }
        });
        refreshGrid();
    }

    public Workbook getWorkbook() {
        return workbook.get();
    }

    public void setWorkbook(Workbook workbook) {
        Workbook safeWorkbook = Objects.requireNonNullElseGet(workbook, Workbook::blank);
        this.workbook.set(safeWorkbook);
        ensureActiveSheetExists(safeWorkbook);
    }

    public ObjectProperty<Workbook> workbookProperty() {
        return workbook;
    }

    public String getActiveSheetName() {
        return activeSheetName.get();
    }

    public void setActiveSheetName(String sheetName) {
        String normalized = Objects.requireNonNullElse(sheetName, "").trim();
        if (getWorkbook().findSheet(normalized).isEmpty()) {
            throw new IllegalArgumentException("No sheet named: " + sheetName);
        }
        activeSheetName.set(normalized);
    }

    public ReadOnlyStringProperty activeSheetNameProperty() {
        return activeSheetName;
    }

    public CellAddress getSelectedCell() {
        return selectedCell.get();
    }

    public ReadOnlyObjectProperty<CellAddress> selectedCellProperty() {
        return selectedCell;
    }

    public CellSelection getSelection() {
        return selection.get();
    }

    public ReadOnlyObjectProperty<CellSelection> selectionProperty() {
        return selection;
    }

    public void select(CellAddress address) {
        CellAddress safeAddress = Objects.requireNonNull(address, "address");
        selection.set(CellSelection.single(getActiveSheetName(), safeAddress));
    }

    public void commitInput(CellAddress address, String input) {
        commit(address, input);
    }

    public int visibleRows() {
        return visibleRows;
    }

    public int visibleColumns() {
        return visibleColumns;
    }

    private void configureGrid() {
        grid.getStyleClass().add("delos-spreadsheet-grid");
        grid.setGridLinesVisible(true);
        grid.setPadding(new Insets(8));

        ColumnConstraints rowHeader = new ColumnConstraints(ROW_HEADER_WIDTH);
        rowHeader.setMinWidth(ROW_HEADER_WIDTH);
        rowHeader.setPrefWidth(ROW_HEADER_WIDTH);
        grid.getColumnConstraints().add(rowHeader);

        for (int column = 0; column < visibleColumns; column++) {
            ColumnConstraints cellColumn = new ColumnConstraints(CELL_WIDTH);
            cellColumn.setMinWidth(72.0);
            cellColumn.setPrefWidth(CELL_WIDTH);
            cellColumn.setHgrow(Priority.NEVER);
            grid.getColumnConstraints().add(cellColumn);
        }

        grid.getRowConstraints().add(fixedRow());
        for (int row = 0; row < visibleRows; row++) {
            grid.getRowConstraints().add(fixedRow());
        }
    }

    private void refreshGrid() {
        if (refreshing) {
            return;
        }
        refreshing = true;
        try {
            grid.getChildren().clear();
            addCornerHeader();
            addColumnHeaders();
            addRows();
        } finally {
            refreshing = false;
        }
    }

    private void addCornerHeader() {
        Label corner = headerLabel("");
        corner.getStyleClass().add("delos-spreadsheet-corner-header");
        grid.add(corner, 0, 0);
    }

    private void addColumnHeaders() {
        for (int column = 0; column < visibleColumns; column++) {
            Label label = headerLabel(CellAddress.columnName(column));
            label.getStyleClass().add("delos-spreadsheet-column-header");
            grid.add(label, column + 1, 0);
        }
    }

    private void addRows() {
        Sheet sheet = activeSheet();
        for (int row = 0; row < visibleRows; row++) {
            Label rowHeader = headerLabel(Integer.toString(row + 1));
            rowHeader.getStyleClass().add("delos-spreadsheet-row-header");
            grid.add(rowHeader, 0, row + 1);

            for (int column = 0; column < visibleColumns; column++) {
                CellAddress address = CellAddress.of(row, column);
                grid.add(createCellField(sheet, address), column + 1, row + 1);
            }
        }
    }

    private TextField createCellField(Sheet sheet, CellAddress address) {
        TextField field = new TextField(sheet.cellAt(address).content().displayText());
        field.getStyleClass().add("delos-spreadsheet-cell");
        if (address.equals(getSelectedCell())) {
            field.getStyleClass().add("delos-spreadsheet-cell-selected");
        }
        field.setMinSize(72.0, CELL_HEIGHT);
        field.setPrefSize(CELL_WIDTH, CELL_HEIGHT);
        field.setAlignment(Pos.CENTER_LEFT);
        field.setUserData(address);

        field.setOnMousePressed(event -> select(address));
        field.focusedProperty().addListener((ignored, wasFocused, isFocused) -> {
            if (isFocused) {
                select(address);
            } else if (wasFocused) {
                commit(address, field.getText());
            }
        });
        field.setOnAction(event -> commit(address, field.getText()));
        return field;
    }

    private void commit(CellAddress address, String input) {
        if (refreshing) {
            return;
        }
        Workbook current = getWorkbook();
        Workbook updatedWorkbook = WorkbookEditor.edit(current)
                .setCellInput(getActiveSheetName(), address, input)
                .workbook();
        if (!updatedWorkbook.equals(current)) {
            workbook.set(updatedWorkbook);
        }
    }

    private Sheet activeSheet() {
        return getWorkbook().findSheet(getActiveSheetName()).orElseGet(() -> getWorkbook().firstSheet());
    }

    private void ensureActiveSheetExists(Workbook newWorkbook) {
        String currentName = getActiveSheetName();
        if (newWorkbook.findSheet(currentName).isPresent()) {
            return;
        }
        String fallbackName = newWorkbook.firstSheet().name();
        activeSheetName.set(fallbackName);
        selection.set(CellSelection.single(fallbackName, CellAddress.of(0, 0)));
    }

    private static RowConstraints fixedRow() {
        RowConstraints row = new RowConstraints(CELL_HEIGHT);
        row.setMinHeight(CELL_HEIGHT);
        row.setPrefHeight(CELL_HEIGHT);
        return row;
    }

    private static Label headerLabel(String text) {
        Label label = new Label(text);
        label.setAlignment(Pos.CENTER);
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        label.getStyleClass().add("delos-spreadsheet-header");
        return label;
    }

    private static int positive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
        return value;
    }
}
