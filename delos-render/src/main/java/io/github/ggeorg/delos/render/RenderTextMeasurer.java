package io.github.ggeorg.delos.render;

import java.util.List;

/**
 * Platform-neutral text measurement contract used by render-time decisions.
 *
 * <p>This deliberately does not depend on Writer layout packages. JavaFX, PDF,
 * SVG, and test implementations can implement this directly or share the same
 * concrete class with the layout text measurer.</p>
 */
public interface RenderTextMeasurer {
    RenderFont styledFont(RenderFont baseFont, boolean bold, boolean italic);

    double textWidth(String text, RenderFont font);

    double charWidth(char ch, RenderFont font);

    double lineHeight(RenderFont font);

    double baseline(RenderFont font);

    /**
     * Returns cumulative caret x-stops for the supplied text.
     * The first stop must be {@code 0.0}; the last stop is the run width.
     */
    List<Double> caretStops(String text, RenderFont font);

    /**
     * Measures all geometry for a single render text run in one place.
     *
     * <p>Renderers should prefer this method when they need more than one text
     * metric. The default implementation preserves existing implementations,
     * while concrete backends can override to avoid duplicate work or to keep
     * sanitized/fallback text handling internally consistent.</p>
     */
    default TextLayoutResult layoutText(String text, RenderFont font) {
        String safeText = text == null ? "" : text;
        return new TextLayoutResult(
                safeText,
                font,
                textWidth(safeText, font),
                lineHeight(font),
                baseline(font),
                caretStops(safeText, font)
        );
    }
}
