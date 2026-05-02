package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.render.RenderFont;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic, platform-neutral fallback text measurer.
 *
 * <p>This class deliberately does not use JavaFX. Production JavaFX rendering
 * should inject {@code JavaFxTextMeasurer}; this fallback keeps the pure layout
 * package usable in tests and prevents layout strategies from depending on a
 * UI toolkit adapter.</p>
 */
public final class ApproximateTextMeasurer implements TextMeasurer {
    private static final double NORMAL_WIDTH_FACTOR = 0.55;
    private static final double SPACE_WIDTH_FACTOR = 0.33;
    private static final double NARROW_WIDTH_FACTOR = 0.30;
    private static final double WIDE_WIDTH_FACTOR = 0.85;
    private static final double BOLD_WIDTH_FACTOR = 1.06;
    private static final double ITALIC_WIDTH_FACTOR = 1.02;
    private static final double LINE_HEIGHT_FACTOR = 1.20;
    private static final double BASELINE_FACTOR = 0.86;

    @Override
    public RenderFont styledFont(RenderFont baseFont, boolean bold, boolean italic) {
        Objects.requireNonNull(baseFont, "baseFont");
        return new RenderFont(baseFont.family(), baseFont.size(), bold, italic);
    }

    @Override
    public double textWidth(String text, RenderFont font) {
        String safeText = text == null ? "" : text;
        double width = 0;
        for (int i = 0; i < safeText.length(); i++) {
            width += charWidth(safeText.charAt(i), font);
        }
        return width;
    }

    @Override
    public double charWidth(char ch, RenderFont font) {
        Objects.requireNonNull(font, "font");
        double factor;
        if (Character.isWhitespace(ch)) {
            factor = SPACE_WIDTH_FACTOR;
        } else if ("ilI.,'!|".indexOf(ch) >= 0) {
            factor = NARROW_WIDTH_FACTOR;
        } else if ("MW@#%&".indexOf(ch) >= 0) {
            factor = WIDE_WIDTH_FACTOR;
        } else {
            factor = NORMAL_WIDTH_FACTOR;
        }
        if (font.bold()) {
            factor *= BOLD_WIDTH_FACTOR;
        }
        if (font.italic()) {
            factor *= ITALIC_WIDTH_FACTOR;
        }
        return font.size() * factor;
    }

    @Override
    public double lineHeight(RenderFont font) {
        Objects.requireNonNull(font, "font");
        return font.size() * LINE_HEIGHT_FACTOR;
    }

    @Override
    public double baseline(RenderFont font) {
        Objects.requireNonNull(font, "font");
        return lineHeight(font) * BASELINE_FACTOR;
    }

    @Override
    public List<Double> caretStops(String text, RenderFont font) {
        String safeText = text == null ? "" : text;
        List<Double> stops = new ArrayList<>(safeText.length() + 1);
        stops.add(0.0);
        double x = 0;
        for (int i = 0; i < safeText.length(); i++) {
            x += charWidth(safeText.charAt(i), font);
            stops.add(x);
        }
        return stops;
    }
}
