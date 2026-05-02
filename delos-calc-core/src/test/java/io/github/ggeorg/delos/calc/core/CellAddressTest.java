package io.github.ggeorg.delos.calc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CellAddressTest {
    @Test
    void parsesAndFormatsA1Addresses() {
        assertEquals(CellAddress.of(0, 0), CellAddress.parse("A1"));
        assertEquals(CellAddress.of(0, 25), CellAddress.parse("Z1"));
        assertEquals(CellAddress.of(0, 26), CellAddress.parse("AA1"));
        assertEquals(CellAddress.of(9, 27), CellAddress.parse("ab10"));
        assertEquals("AB10", CellAddress.of(9, 27).toA1());
    }

    @Test
    void rejectsInvalidAddresses() {
        assertThrows(IllegalArgumentException.class, () -> CellAddress.parse(""));
        assertThrows(IllegalArgumentException.class, () -> CellAddress.parse("A0"));
        assertThrows(IllegalArgumentException.class, () -> CellAddress.parse("1A"));
        assertThrows(IllegalArgumentException.class, () -> CellAddress.parse("A"));
        assertThrows(IllegalArgumentException.class, () -> CellAddress.of(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> CellAddress.of(0, -1));
    }

    @Test
    void sortsByRowThenColumn() {
        assertEquals(
                -1,
                Integer.signum(CellAddress.parse("A1").compareTo(CellAddress.parse("B1")))
        );
        assertEquals(
                -1,
                Integer.signum(CellAddress.parse("Z1").compareTo(CellAddress.parse("A2")))
        );
    }
}
