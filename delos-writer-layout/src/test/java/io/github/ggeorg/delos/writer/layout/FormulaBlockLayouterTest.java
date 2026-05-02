package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.writer.document.FormulaBlock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormulaBlockLayouterTest {
    private static final double EPSILON = 0.0001;

    @Test
    void laysOutFormulaAsFullWidthAtomicPlaceholder() {
        FormulaBlock formula = new FormulaBlock("E = mc^2", "mass energy");

        LaidOutFormulaBlock laidOut = new FormulaBlockLayouter()
            .layout(3, formula, 10, 20, 400, LayoutTheme.defaultTheme());

        assertEquals(3, laidOut.sourceBlockIndex());
        assertEquals(10, laidOut.x(), EPSILON);
        assertEquals(20, laidOut.y(), EPSILON);
        assertEquals(400, laidOut.width(), EPSILON);
        assertTrue(laidOut.height() >= 56.0);
        assertEquals("latex", laidOut.sourceFormat());
        assertEquals("E = mc^2", laidOut.source());
        assertEquals("mass energy", laidOut.altText());
    }

    @Test
    void enforcesMinimumFormulaHeight() {
        LayoutTheme tinyTheme = new LayoutTheme(
            new RenderFont("System", 8.0, false, false),
            new RenderFont("Serif", 8.0, false, false),
            0.0,
            0.0,
            0.0,
            0.0,
            1.0
        );

        LaidOutFormulaBlock laidOut = new FormulaBlockLayouter()
            .layout(0, new FormulaBlock("x"), 0, 0, 100, tinyTheme);

        assertEquals(56.0, laidOut.height(), EPSILON);
    }

    @Test
    void clampsNegativeWidthToZero() {
        LaidOutFormulaBlock laidOut = new FormulaBlockLayouter()
            .layout(0, new FormulaBlock("x"), 0, 0, -10, LayoutTheme.defaultTheme());

        assertEquals(0.0, laidOut.width(), EPSILON);
    }
}
