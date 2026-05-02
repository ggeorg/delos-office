package io.github.ggeorg.delos.calc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CellContentTest {
    @Test
    void parsesCommonUserInput() {
        assertEquals(CellContent.Type.BLANK, CellContent.parseInput("  ").type());
        assertEquals(CellContent.Type.NUMBER, CellContent.parseInput("42.5").type());
        assertEquals(CellContent.Type.BOOLEAN, CellContent.parseInput("true").type());
        assertEquals(CellContent.Type.FORMULA, CellContent.parseInput("=A1+B1").type());
        assertEquals(CellContent.Type.TEXT, CellContent.parseInput("hello").type());
    }

    @Test
    void exposesTypedValuesSafely() {
        CellContent number = CellContent.parseInput("12");
        assertTrue(number.numberValue().isPresent());
        assertEquals(12.0, number.numberValue().orElseThrow());

        CellContent bool = CellContent.parseInput("FALSE");
        assertFalse(bool.booleanValue());
        assertThrows(IllegalStateException.class, number::booleanValue);
    }

    @Test
    void preservesFormulaDisplayWithLeadingEquals() {
        CellContent formula = CellContent.formula("=A1+B1");
        assertEquals("A1+B1", formula.text());
        assertEquals("=A1+B1", formula.displayText());
    }
}
