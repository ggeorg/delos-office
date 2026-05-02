package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.render.RenderFont;

/**
 * JavaFX-free layout policy for Writer pagination and text flow.
 * <p>
 * This type intentionally contains only values that affect the immutable layout
 * model. View chrome such as workspace padding, inter-page gaps, and page
 * shadows belongs to the JavaFX view theme.
 */
public record LayoutTheme(
        RenderFont titleFont,
        RenderFont bodyFont,
        double titleGap,
        double titleLineGap,
        double separatorGap,
        double paragraphSpacing,
        double bodyLineGap
) {
    public static LayoutTheme defaultTheme() {
        return new LayoutTheme(
                new RenderFont("System", 24.0, false, false),
                new RenderFont("Serif", 13.5, false, false),
                12.0,
                5.0,
                10.0,
                8.0,
                5.5
        );
    }
}
