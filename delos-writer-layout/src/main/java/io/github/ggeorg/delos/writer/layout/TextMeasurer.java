package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.TextLayoutResult;

import java.util.List;

/**
 * Platform-neutral text measurement contract used by layout code.
 *
 * <p>The layout engine must not know whether measurements come from JavaFX,
 * PDFBox, AWT, or a deterministic test double. JavaFX-specific measurement
 * belongs in an adapter such as {@code JavaFxTextMeasurer}.</p>
 */
public interface TextMeasurer {
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
     * Measures all geometry for a single layout text run in one place.
     *
     * <p>Layouters should prefer this method over independently asking for
     * width, baseline, line-height, and caret stops. That keeps line breaking,
     * caret geometry, underlines, PDF export, and JavaFX preview tied to the
     * same backend measurements.</p>
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
