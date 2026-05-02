package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RenderColor;
import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.RenderTheme;

/** Test-only render theme that keeps delos-writer-render tests JavaFX-free. */
final class TestRenderThemes {
    private TestRenderThemes() {
    }

    static RenderTheme defaultTheme() {
        return new RenderTheme(
                RenderColor.rgb(238, 241, 246),
                RenderColor.rgba(0, 0, 0, 0.07),
                RenderColor.rgb(255, 255, 255),
                RenderColor.rgb(214, 219, 227),
                RenderColor.rgb(229, 233, 239),
                RenderColor.rgb(31, 37, 46),
                RenderColor.rgb(52, 58, 66),
                RenderColor.rgba(86, 134, 245, 0.24),
                new RenderFont("System", 22.0, true, false),
                new RenderFont("System", 15.0, false, false),
                10.0,
                7.0,
                9.0
        );
    }
}
