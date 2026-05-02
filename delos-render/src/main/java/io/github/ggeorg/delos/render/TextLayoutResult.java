package io.github.ggeorg.delos.render;

import java.util.List;
import java.util.Objects;

/**
 * Immutable text geometry for one measured text run.
 *
 * <p>This is the shared text-fidelity seam: layout code and render code should
 * consume the same width, baseline, line-height, caret stops, and decoration
 * metrics instead of recomputing or approximating those values independently.</p>
 */
public record TextLayoutResult(
        String text,
        RenderFont font,
        double width,
        double lineHeight,
        double baseline,
        List<Double> caretStops,
        TextDecorationMetrics decorations
) {
    public TextLayoutResult(
            String text,
            RenderFont font,
            double width,
            double lineHeight,
            double baseline,
            List<Double> caretStops
    ) {
        this(text, font, width, lineHeight, baseline, caretStops, TextDecorationMetrics.fromFont(font));
    }

    public TextLayoutResult {
        text = Objects.requireNonNullElse(text, "");
        font = Objects.requireNonNull(font, "font");
        caretStops = List.copyOf(Objects.requireNonNull(caretStops, "caretStops"));
        decorations = decorations == null ? TextDecorationMetrics.fromFont(font) : decorations;
        if (caretStops.isEmpty()) {
            throw new IllegalArgumentException("caretStops must not be empty");
        }
        if (Math.abs(caretStops.getFirst()) > 0.0001) {
            throw new IllegalArgumentException("first caret stop must be 0.0");
        }
        if (width < 0.0) {
            throw new IllegalArgumentException("width must be >= 0.0");
        }
        if (lineHeight < 0.0) {
            throw new IllegalArgumentException("lineHeight must be >= 0.0");
        }
        if (baseline < 0.0) {
            throw new IllegalArgumentException("baseline must be >= 0.0");
        }
    }

    public double endCaretStop() {
        return caretStops.getLast();
    }
}
