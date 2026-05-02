package io.github.ggeorg.delos.writer.render;

/**
 * Describes why a page is being rendered.
 *
 * <p>The destination makes editor-only overlays explicit. This keeps the live
 * editor, preview, image export, and future PDF export on the same page-render
 * pipeline without letting caret/selection/chrome accidentally leak into final
 * output.</p>
 */
public enum PageRenderDestination {
    EDITOR(true, true, true),
    PRINT_PREVIEW(false, false, true),
    PDF_EXPORT(false, false, false),
    IMAGE_EXPORT(false, false, false);

    private final boolean defaultDrawSelection;
    private final boolean defaultDrawCaret;
    private final boolean defaultDrawPageChrome;

    PageRenderDestination(boolean defaultDrawSelection, boolean defaultDrawCaret, boolean defaultDrawPageChrome) {
        this.defaultDrawSelection = defaultDrawSelection;
        this.defaultDrawCaret = defaultDrawCaret;
        this.defaultDrawPageChrome = defaultDrawPageChrome;
    }

    public boolean defaultDrawSelection() {
        return defaultDrawSelection;
    }

    public boolean defaultDrawCaret() {
        return defaultDrawCaret;
    }

    public boolean defaultDrawPageChrome() {
        return defaultDrawPageChrome;
    }

    public boolean allowsSelectionOverlay() {
        return this == EDITOR;
    }

    public boolean allowsCaretOverlay() {
        return this == EDITOR;
    }

    public boolean allowsPageChrome() {
        return this == EDITOR || this == PRINT_PREVIEW;
    }
}
