package io.github.ggeorg.delos.calc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SheetTest {
    @Test
    void storesCellsSparselyAndImmutably() {
        Sheet empty = Sheet.named("Sheet1");
        Sheet updated = empty.withInput(CellAddress.parse("B2"), "42");

        assertEquals(0, empty.usedCellCount());
        assertEquals(1, updated.usedCellCount());
        assertTrue(empty.cellAt(CellAddress.parse("B2")).isBlank());
        assertEquals(CellContent.Type.NUMBER, updated.cellAt(CellAddress.parse("B2")).content().type());
    }

    @Test
    void blankContentRemovesCellsFromSparseStore() {
        Sheet sheet = Sheet.named("Sheet1")
                .withInput(CellAddress.parse("A1"), "hello")
                .clear(CellAddress.parse("A1"));

        assertEquals(0, sheet.usedCellCount());
        assertFalse(sheet.findCell(CellAddress.parse("A1")).isPresent());
    }

    @Test
    void validatesSheetNames() {
        assertThrows(IllegalArgumentException.class, () -> Sheet.named(""));
        assertThrows(IllegalArgumentException.class, () -> Sheet.named("Bad/Name"));
    }
}
