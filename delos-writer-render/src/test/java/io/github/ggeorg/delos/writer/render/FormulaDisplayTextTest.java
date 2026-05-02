package io.github.ggeorg.delos.writer.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FormulaDisplayTextTest {
    @Test
    void convertsCommonLatexFragmentsToReadablePreviewText() {
        assertEquals("E = mc²", FormulaDisplayText.preview("E = mc^2"));
        assertEquals("α₁ + β²", FormulaDisplayText.preview("\\alpha_1 + \\beta^2"));
        assertEquals("√(x) + (a)/(b)", FormulaDisplayText.preview("\\sqrt{x} + \\frac{a}{b}"));
        assertEquals("xᵢ₊₁ ≤ ∞", FormulaDisplayText.preview("x_{i+1} \\leq \\infty"));
    }

    @Test
    void fallsBackForBlankSource() {
        assertEquals("Formula", FormulaDisplayText.preview("  \n  "));
        assertEquals("Formula source", FormulaDisplayText.compactSource(null));
    }

    @Test
    void abbreviatesLongTextWithEllipsis() {
        assertEquals("abcd…", FormulaDisplayText.abbreviate("abcdef", 5));
        assertEquals("abc", FormulaDisplayText.abbreviate("abc", 5));
    }
}
