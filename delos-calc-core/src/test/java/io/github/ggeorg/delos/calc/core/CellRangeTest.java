package io.github.ggeorg.delos.calc.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CellRangeTest {
    @Test
    void normalizesCornersAndFormatsA1Range() {
        CellRange range = CellRange.between(CellAddress.parse("C3"), CellAddress.parse("A1"));

        assertEquals(CellAddress.parse("A1"), range.first());
        assertEquals(CellAddress.parse("C3"), range.last());
        assertEquals("A1:C3", range.toA1());
        assertEquals(3, range.rowCount());
        assertEquals(3, range.columnCount());
        assertEquals(9, range.cellCount());
    }

    @Test
    void exposesRowMajorAddresses() {
        CellRange range = CellRange.between(CellAddress.parse("A1"), CellAddress.parse("B2"));

        assertEquals(
                List.of(CellAddress.parse("A1"), CellAddress.parse("B1"), CellAddress.parse("A2"), CellAddress.parse("B2")),
                range.addresses().toList()
        );
    }

    @Test
    void detectsContainmentAndSingleCells() {
        CellRange single = CellRange.single(CellAddress.parse("B2"));

        assertTrue(single.isSingleCell());
        assertTrue(single.contains(CellAddress.parse("B2")));
        assertFalse(single.contains(CellAddress.parse("B3")));
        assertEquals("B2", single.toString());
    }
}
