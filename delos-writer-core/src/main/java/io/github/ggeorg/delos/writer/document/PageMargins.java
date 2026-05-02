package io.github.ggeorg.delos.writer.document;

/**
 * Page margin box expressed in PDF points.
 */
public record PageMargins(
        double top,
        double right,
        double bottom,
        double left
) {
    public PageMargins {
        requireNonNegativeFinite("top", top);
        requireNonNegativeFinite("right", right);
        requireNonNegativeFinite("bottom", bottom);
        requireNonNegativeFinite("left", left);
    }

    public static PageMargins of(double allSides) {
        return new PageMargins(allSides, allSides, allSides, allSides);
    }

    public static PageMargins oneInch() {
        return of(72.0);
    }

    /**
     * Keeps the existing Delos Writer default geometry stable.
     */
    public static PageMargins writerDefault() {
        return new PageMargins(68.0, 72.0, 72.0, 72.0);
    }

    private static void requireNonNegativeFinite(String field, double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException("Page margin " + field + " must be a non-negative finite value");
        }
    }
}
