package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.HorizontalRuleBlock;

import java.util.Objects;

/**
 * Lays out horizontal rules as atomic page-flow blocks.
 *
 * <p>The visual rule is painted inside this rectangle. Keeping a real block
 * height makes the rule selectable and gives it stable spacing in editor and
 * PDF output.</p>
 */
public final class HorizontalRuleBlockLayouter {
    private static final double DEFAULT_HEIGHT = 18.0;

    public LaidOutSeparator layout(
        int sourceBlockIndex,
        HorizontalRuleBlock horizontalRuleBlock,
        double x,
        double y,
        double width
    ) {
        Objects.requireNonNull(horizontalRuleBlock, "horizontalRuleBlock");
        return new LaidOutSeparator(sourceBlockIndex, x, y, Math.max(0.0, width), DEFAULT_HEIGHT);
    }
}
