package io.github.ggeorg.delos.writer.render;

/**
 * Page-local rectangle for a temporary editor decoration.
 *
 * <p>Coordinates are page-local. The renderer paints only; it does not resolve
 * text ranges to geometry.</p>
 */
public record PageDecoration(
        DecorationKind kind,
        DecorationLayer layer,
        double x,
        double y,
        double width,
        double height
) {
    public PageDecoration {
        kind = kind == null ? DecorationKind.SEARCH_HIGHLIGHT : kind;
        layer = layer == null ? DecorationLayer.BEHIND_TEXT : layer;
        x = finiteOrZero(x);
        y = finiteOrZero(y);
        width = Math.max(0.0, finiteOrZero(width));
        height = Math.max(0.0, finiteOrZero(height));
    }

    public static PageDecoration highlight(DecorationKind kind, double x, double y, double width, double height) {
        return new PageDecoration(kind, DecorationLayer.BEHIND_TEXT, x, y, width, height);
    }

    public static PageDecoration underline(DecorationKind kind, double x, double y, double width) {
        return new PageDecoration(kind, DecorationLayer.ABOVE_TEXT, x, y, width, 0.0);
    }

    public boolean isEmpty() {
        return width <= 0.0 || (layer == DecorationLayer.BEHIND_TEXT && height <= 0.0);
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
