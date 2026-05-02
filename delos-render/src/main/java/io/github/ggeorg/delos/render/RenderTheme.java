package io.github.ggeorg.delos.render;

/**
 * Platform-neutral rendering policy.
 *
 * <p>JavaFX-specific layout concerns such as viewport padding and page gaps stay
 * in the application/view layer; this record contains only values needed by
 * the page painter.</p>
 */
public record RenderTheme(
        RenderColor workspaceBackground,
        RenderColor pageShadow,
        RenderColor pageBackground,
        RenderColor pageBorder,
        RenderColor separatorColor,
        RenderColor titleText,
        RenderColor bodyText,
        RenderColor selectionFill,
        RenderFont titleFont,
        RenderFont bodyFont,
        double pageCornerRadius,
        double pageShadowOffsetX,
        double pageShadowOffsetY
) {
    public double shadowExtentX() {
        return Math.abs(pageShadowOffsetX()) + pageCornerRadius();
    }

    public double shadowExtentY() {
        return Math.abs(pageShadowOffsetY()) + pageCornerRadius();
    }
}
