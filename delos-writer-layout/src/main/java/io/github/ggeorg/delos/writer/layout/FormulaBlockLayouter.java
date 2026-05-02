package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.FormulaBlock;

import java.util.Objects;

/**
 * Lays out source-based formula blocks as atomic placeholders.
 *
 * <p>The formula source remains canonical document content. This layouter only
 * computes the placeholder rectangle used by editor, PDF, and hit testing.</p>
 */
public final class FormulaBlockLayouter {
    private static final double MIN_HEIGHT = 56.0;
    private static final double VERTICAL_PADDING = 32.0;

    public LaidOutFormulaBlock layout(
        int sourceBlockIndex,
        FormulaBlock formulaBlock,
        double x,
        double y,
        double width,
        LayoutTheme theme
    ) {
        Objects.requireNonNull(formulaBlock, "formulaBlock");
        Objects.requireNonNull(theme, "theme");
        double height = Math.max(MIN_HEIGHT, theme.bodyFont().size() + theme.bodyLineGap() + VERTICAL_PADDING);

        return new LaidOutFormulaBlock(
            sourceBlockIndex,
            x,
            y,
            Math.max(0.0, width),
            height,
            formulaBlock.sourceFormat().xmlValue(),
            formulaBlock.source(),
            formulaBlock.altText()
        );
    }
}
