package io.github.ggeorg.delos.calc.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkbookEditorTest {
    @Test
    void editsCellsThroughWorkbookBoundary() {
        Workbook workbook = Workbook.blank();

        Workbook updated = WorkbookEditor.edit(workbook)
                .setCellInput("Sheet1", CellAddress.parse("B2"), "42")
                .workbook();

        assertEquals(0, workbook.firstSheet().usedCellCount());
        assertEquals("42.0", updated.firstSheet().cellAt(CellAddress.parse("B2")).content().displayText());
    }

    @Test
    void noOpEditKeepsSameEditorSnapshot() {
        Workbook workbook = Workbook.blank();
        WorkbookEditor editor = WorkbookEditor.edit(workbook);

        WorkbookEditor afterBlankClear = editor.clearCell("Sheet1", CellAddress.parse("A1"));

        assertSame(editor, afterBlankClear);
    }

    @Test
    void clearsRanges() {
        Workbook workbook = WorkbookEditor.edit(Workbook.blank())
                .setCellInput("Sheet1", CellAddress.parse("A1"), "one")
                .setCellInput("Sheet1", CellAddress.parse("B1"), "two")
                .setCellInput("Sheet1", CellAddress.parse("C1"), "three")
                .workbook();

        Workbook updated = WorkbookEditor.edit(workbook)
                .clearRange("Sheet1", CellRange.between(CellAddress.parse("A1"), CellAddress.parse("B1")))
                .workbook();

        assertEquals(1, updated.firstSheet().usedCellCount());
        assertEquals("three", updated.firstSheet().cellAt(CellAddress.parse("C1")).content().displayText());
    }

    @Test
    void rejectsUnknownSheetsAndHugeRangeCommands() {
        WorkbookEditor editor = WorkbookEditor.edit(Workbook.blank());

        assertThrows(IllegalArgumentException.class,
                () -> editor.setCellInput("Missing", CellAddress.parse("A1"), "x"));
        assertThrows(IllegalArgumentException.class,
                () -> editor.clearRange("Sheet1", CellRange.between(CellAddress.parse("A1"), CellAddress.parse("A100001"))));
    }
}
