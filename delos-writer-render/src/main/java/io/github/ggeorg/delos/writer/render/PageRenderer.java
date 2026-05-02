package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RenderTarget;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.render.RenderTheme;

import io.github.ggeorg.delos.writer.layout.LaidOutPage;

/**
 * Renders one laid-out page with destination-aware overlay policy.
 */
public interface PageRenderer {
    default void renderPage(RenderTarget target, LaidOutPage page, RenderTheme theme, RenderTextMeasurer measurer) {
        renderPage(target, page, theme, measurer, PageRenderState.EMPTY);
    }

    default void renderPage(
            RenderTarget target,
            LaidOutPage page,
            RenderTheme theme,
            RenderTextMeasurer measurer,
            PageRenderState state
    ) {
        renderPage(target, PageRenderContext.editor(page, theme, measurer, state));
    }

    void renderPage(RenderTarget target, PageRenderContext context);
}
