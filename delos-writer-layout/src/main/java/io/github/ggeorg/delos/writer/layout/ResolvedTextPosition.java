package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.TextPosition;

/**
 * Resolved caret position tied to concrete layout objects.
 */
public record ResolvedTextPosition(
        LaidOutPage page,
        int pageIndex,
        LaidOutTextBlock block,
        int blockIndex,
        LaidOutLine line,
        int lineIndex,
        int columnIndex,
        TextPosition position
) {
}
