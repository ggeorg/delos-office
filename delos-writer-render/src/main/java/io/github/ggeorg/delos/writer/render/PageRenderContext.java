package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RenderImageResolver;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.render.RenderTheme;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;

import java.util.Objects;

/**
 * Immutable parameters for rendering one page.
 *
 * <p>This follows the page-render-parameters pattern used by mature page
 * renderers: the caller states the page, theme, measurement source,
 * destination, scale, asset resolver, and overlay policy in one value object
 * instead of scattering those flags across rendering methods.</p>
 */
public record PageRenderContext(
        LaidOutPage page,
        RenderTheme theme,
        RenderTextMeasurer measurer,
        RenderImageResolver imageResolver,
        PageRenderState state,
        PageRenderDestination destination,
        double scale,
        boolean drawSelection,
        boolean drawCaret,
        boolean drawPageChrome
) {
    public PageRenderContext {
        page = Objects.requireNonNull(page, "page");
        theme = Objects.requireNonNull(theme, "theme");
        measurer = Objects.requireNonNull(measurer, "measurer");
        imageResolver = imageResolver == null ? RenderImageResolver.empty() : imageResolver;
        state = state == null ? PageRenderState.EMPTY : state;
        destination = destination == null ? PageRenderDestination.EDITOR : destination;
        scale = Double.isFinite(scale) && scale > 0.0 ? scale : 1.0;
        drawSelection = drawSelection && destination.allowsSelectionOverlay();
        drawCaret = drawCaret && destination.allowsCaretOverlay();
        drawPageChrome = drawPageChrome && destination.allowsPageChrome();
    }

    public PageRenderContext(
            LaidOutPage page,
            RenderTheme theme,
            RenderTextMeasurer measurer,
            PageRenderState state,
            PageRenderDestination destination,
            double scale,
            boolean drawSelection,
            boolean drawCaret,
            boolean drawPageChrome
    ) {
        this(
                page,
                theme,
                measurer,
                RenderImageResolver.empty(),
                state,
                destination,
                scale,
                drawSelection,
                drawCaret,
                drawPageChrome
        );
    }

    /**
     * Backwards-compatible constructor for callers that only distinguished page
     * chrome from final output. Overlay policy is derived from the destination.
     */
    public PageRenderContext(
            LaidOutPage page,
            RenderTheme theme,
            RenderTextMeasurer measurer,
            PageRenderState state,
            PageRenderDestination destination,
            double scale,
            boolean drawPageChrome
    ) {
        this(
                page,
                theme,
                measurer,
                RenderImageResolver.empty(),
                state,
                destination,
                scale,
                destinationOrEditor(destination).defaultDrawSelection(),
                destinationOrEditor(destination).defaultDrawCaret(),
                drawPageChrome
        );
    }

    public static PageRenderContext editor(
            LaidOutPage page,
            RenderTheme theme,
            RenderTextMeasurer measurer,
            PageRenderState state
    ) {
        return forDestination(page, theme, measurer, state, PageRenderDestination.EDITOR);
    }

    public static PageRenderContext printPreview(
            LaidOutPage page,
            RenderTheme theme,
            RenderTextMeasurer measurer
    ) {
        return forDestination(page, theme, measurer, PageRenderState.EMPTY, PageRenderDestination.PRINT_PREVIEW);
    }

    public static PageRenderContext pdfExport(
            LaidOutPage page,
            RenderTheme theme,
            RenderTextMeasurer measurer
    ) {
        return forDestination(page, theme, measurer, PageRenderState.EMPTY, PageRenderDestination.PDF_EXPORT);
    }

    public static PageRenderContext imageExport(
            LaidOutPage page,
            RenderTheme theme,
            RenderTextMeasurer measurer
    ) {
        return forDestination(page, theme, measurer, PageRenderState.EMPTY, PageRenderDestination.IMAGE_EXPORT);
    }

    public static PageRenderContext forDestination(
            LaidOutPage page,
            RenderTheme theme,
            RenderTextMeasurer measurer,
            PageRenderState state,
            PageRenderDestination destination
    ) {
        PageRenderDestination safeDestination = destinationOrEditor(destination);
        return new PageRenderContext(
                page,
                theme,
                measurer,
                RenderImageResolver.empty(),
                state,
                safeDestination,
                1.0,
                safeDestination.defaultDrawSelection(),
                safeDestination.defaultDrawCaret(),
                safeDestination.defaultDrawPageChrome()
        );
    }

    public PageRenderContext withImageResolver(RenderImageResolver resolver) {
        return new PageRenderContext(
                page,
                theme,
                measurer,
                resolver,
                state,
                destination,
                scale,
                drawSelection,
                drawCaret,
                drawPageChrome
        );
    }

    public boolean drawsEditorOverlays() {
        return drawSelection || drawCaret;
    }

    private static PageRenderDestination destinationOrEditor(PageRenderDestination destination) {
        return destination == null ? PageRenderDestination.EDITOR : destination;
    }
}
