package io.github.ggeorg.delos.writer.document;

import java.util.Objects;

/**
 * Logical selection expressed as anchor/focus positions.
 * <p>
 * The selection is modeled in the same way editors typically work: the anchor
 * stays fixed while the focus moves. The normalized start/end ordering is
 * derived when needed.
 */
public record SelectionRange(
        TextPosition anchor,
        TextPosition focus
) {
    public SelectionRange {
        anchor = Objects.requireNonNull(anchor, "anchor");
        focus = Objects.requireNonNull(focus, "focus");
    }

    public TextPosition start() {
        return TextPosition.min(anchor, focus);
    }

    public TextPosition end() {
        return TextPosition.max(anchor, focus);
    }

    public boolean isCollapsed() {
        return anchor.compareTo(focus) == 0;
    }
}
