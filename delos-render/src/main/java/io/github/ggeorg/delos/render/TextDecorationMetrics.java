package io.github.ggeorg.delos.render;

/**
 * Font-relative decoration geometry for one laid-out text run.
 *
 * <p>Offsets are expressed in page coordinates relative to the text baseline:
 * underline offsets are positive below the baseline, while strikethrough offsets
 * are positive above the baseline.</p>
 */
public record TextDecorationMetrics(
        double underlineOffset,
        double strikethroughOffset,
        double thickness
) {
    public TextDecorationMetrics {
        underlineOffset = positiveOrDefault(underlineOffset, 0.0);
        strikethroughOffset = positiveOrDefault(strikethroughOffset, 0.0);
        thickness = positiveOrDefault(thickness, 1.0);
    }

    public static TextDecorationMetrics fromFont(RenderFont font) {
        double size = font == null ? 12.0 : font.size();
        return new TextDecorationMetrics(size * 0.10, size * 0.30, Math.max(0.5, size / 18.0));
    }

    private static double positiveOrDefault(double value, double fallback) {
        return Double.isFinite(value) && value >= 0.0 ? value : fallback;
    }
}
