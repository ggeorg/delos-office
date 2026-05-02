package io.github.ggeorg.delos.writer.ui;

import io.github.ggeorg.delos.render.RenderColor;
import io.github.ggeorg.delos.render.RenderTheme;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;

/**
 * JavaFX view/chrome policy for the Writer viewport.
 * <p>
 * This type owns scene-graph spacing, page chrome geometry, and render colors.
 * Pagination receives only {@link LayoutTheme}.
 */
public record ViewTheme(
        RenderColor workspaceBackground,
        RenderColor pageShadow,
        RenderColor pageBackground,
        RenderColor pageBorder,
        RenderColor separatorColor,
        RenderColor titleText,
        RenderColor bodyText,
        RenderColor selectionFill,
        LayoutTheme layoutTheme,
        double outerPadding,
        double interPageGap,
        double pageCornerRadius,
        double pageShadowOffsetX,
        double pageShadowOffsetY
) {
    public static ViewTheme defaultTheme() {
        return new ViewTheme(
                RenderColor.rgb(238, 241, 246),
                RenderColor.rgba(0, 0, 0, 0.0),
                RenderColor.rgb(255, 255, 255),
                RenderColor.rgb(214, 219, 227),
                RenderColor.rgb(229, 233, 239),
                RenderColor.rgb(31, 37, 46),
                RenderColor.rgb(52, 58, 66),
                RenderColor.rgba(86, 134, 245, 0.24),
                LayoutTheme.defaultTheme(),
                18.0,
                28.0,
                0.0,
                0.0,
                0.0
        );
    }

    public RenderTheme renderTheme() {
        return new RenderTheme(
                workspaceBackground(),
                pageShadow(),
                pageBackground(),
                pageBorder(),
                separatorColor(),
                titleText(),
                bodyText(),
                selectionFill(),
                layoutTheme().titleFont(),
                layoutTheme().bodyFont(),
                pageCornerRadius(),
                pageShadowOffsetX(),
                pageShadowOffsetY()
        );
    }

    public double shadowExtentX() {
        return Math.abs(pageShadowOffsetX()) + pageCornerRadius();
    }

    public double shadowExtentY() {
        return Math.abs(pageShadowOffsetY()) + pageCornerRadius();
    }
}
