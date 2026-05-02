package io.github.ggeorg.delos.calc.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CellSelectionTest {
    @Test
    void tracksAnchorFocusAndRange() {
        CellSelection selection = CellSelection.single("Sheet1", CellAddress.parse("A1"))
                .extendTo(CellAddress.parse("C2"));

        assertEquals("Sheet1", selection.sheetName());
        assertEquals(CellAddress.parse("A1"), selection.anchor());
        assertEquals(CellAddress.parse("C2"), selection.focus());
        assertEquals("A1:C2", selection.range().toA1());
        assertFalse(selection.isSingleCell());
    }

    @Test
    void moveToCollapsesSelection() {
        CellSelection selection = CellSelection.single("Sheet1", CellAddress.parse("A1"))
                .extendTo(CellAddress.parse("C2"))
                .moveTo(CellAddress.parse("B2"));

        assertEquals(CellAddress.parse("B2"), selection.anchor());
        assertEquals(CellAddress.parse("B2"), selection.focus());
        assertTrue(selection.isSingleCell());
    }

    @Test
    void rejectsBlankSheetName() {
        assertThrows(IllegalArgumentException.class, () -> CellSelection.single(" ", CellAddress.parse("A1")));
    }
}
