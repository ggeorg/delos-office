package io.github.ggeorg.delos.calc.app;

import io.github.ggeorg.delos.calc.core.CellAddress;
import io.github.ggeorg.delos.calc.core.Workbook;
import io.github.ggeorg.delos.javafx.chrome.DelosStatusLine;
import javafx.scene.control.Label;

import java.util.Objects;

/** Bottom status line for spreadsheet state. */
final class CalcStatusBar extends DelosStatusLine {
    private final Label readyLabel = statusItem();
    private final Label cellLabel = statusItem();
    private final Label sheetLabel = statusItem();
    private final Label usedCellsLabel = statusItem();
    private final Label dirtyLabel = statusItem("calc-status-dirty");

    CalcStatusBar() {
        getStyleClass().addAll("status-bar", "calc-status-bar");
        readyLabel.setText("Ready");
        getChildren().setAll(readyLabel, cellLabel, sheetLabel, usedCellsLabel, spacer(), dirtyLabel);
    }

    void update(Workbook workbook, CellAddress selectedCell, boolean dirty) {
        Workbook safeWorkbook = Objects.requireNonNullElseGet(workbook, Workbook::blank);
        CellAddress safeCell = Objects.requireNonNullElseGet(selectedCell, () -> CellAddress.of(0, 0));
        cellLabel.setText("Cell " + safeCell.toA1());
        sheetLabel.setText(safeWorkbook.firstSheet().name());
        usedCellsLabel.setText(safeWorkbook.firstSheet().usedCellCount() + " used cells");
        dirtyLabel.setText(dirty ? "Modified" : "Saved");
    }
}
