package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RenderColor;
import io.github.ggeorg.delos.render.RenderTarget;
import io.github.ggeorg.delos.render.RenderTheme;

import java.util.List;

/** Paints temporary editor decorations such as search highlights and spelling underlines. */
final class DecorationOverlayRenderer {
    private static final double HIGHLIGHT_ALPHA = 0.28;
    private static final double COMMENT_ALPHA = 0.18;
    private static final double GUIDE_ALPHA = 0.45;
    private static final double UNDERLINE_ALPHA = 0.70;
    private static final double REVISION_ALPHA = 0.85;
    private static final double UNDERLINE_WIDTH = 1.0;
    private static final double REVISION_WIDTH = 1.5;

    void paint(RenderTarget target,
               RenderTheme theme,
               List<PageDecoration> decorations,
               DecorationLayer layer) {
        if (decorations == null || decorations.isEmpty()) {
            return;
        }

        for (PageDecoration decoration : decorations) {
            if (decoration == null || decoration.layer() != layer || decoration.isEmpty()) {
                continue;
            }
            paintDecoration(target, theme, decoration);
        }
    }

    private void paintDecoration(RenderTarget target, RenderTheme theme, PageDecoration decoration) {
        switch (decoration.layer()) {
            case BEHIND_TEXT -> paintBehindText(target, theme, decoration);
            case ABOVE_TEXT -> paintAboveText(target, theme, decoration);
        }
    }

    private void paintBehindText(RenderTarget target, RenderTheme theme, PageDecoration decoration) {
        target.save();
        try {
            target.setGlobalAlpha(alphaForBehindText(decoration.kind()));
            target.setFill(fillFor(theme, decoration.kind()));
            target.fillRect(decoration.x(), decoration.y(), decoration.width(), decoration.height());
        } finally {
            target.restore();
        }
    }

    private void paintAboveText(RenderTarget target, RenderTheme theme, PageDecoration decoration) {
        target.save();
        try {
            target.setGlobalAlpha(decoration.kind() == DecorationKind.REVISION_MARK ? REVISION_ALPHA : UNDERLINE_ALPHA);
            target.setStroke(strokeFor(theme, decoration.kind()));
            target.setLineWidth(decoration.kind() == DecorationKind.REVISION_MARK ? REVISION_WIDTH : UNDERLINE_WIDTH);
            target.strokeLine(decoration.x(), decoration.y(), decoration.x() + decoration.width(), decoration.y());
        } finally {
            target.restore();
        }
    }

    private double alphaForBehindText(DecorationKind kind) {
        return switch (kind) {
            case COMMENT_HIGHLIGHT -> COMMENT_ALPHA;
            case PAGE_GUIDE -> GUIDE_ALPHA;
            default -> HIGHLIGHT_ALPHA;
        };
    }

    private RenderColor fillFor(RenderTheme theme, DecorationKind kind) {
        return kind == DecorationKind.PAGE_GUIDE ? theme.separatorColor() : theme.selectionFill();
    }

    private RenderColor strokeFor(RenderTheme theme, DecorationKind kind) {
        return switch (kind) {
            case PAGE_GUIDE -> theme.separatorColor();
            case SEARCH_HIGHLIGHT, COMMENT_HIGHLIGHT -> theme.selectionFill();
            default -> theme.bodyText();
        };
    }
}
