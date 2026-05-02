package io.github.ggeorg.delos.writer.document;

/**
 * Passive table-wide styling used by layout, rendering, and export.
 *
 * <p>The values are deliberately renderer-neutral. Width is stored as a fraction
 * of the available text area, cell padding is stored in points, and borders are
 * represented as a simple table-wide on/off flag.</p>
 */
public record TableStyle(
        double widthFraction,
        double cellPadding,
        boolean bordersEnabled
) {
    public static final double MIN_WIDTH_FRACTION = 0.25;
    public static final double MAX_WIDTH_FRACTION = 1.0;
    public static final double MIN_CELL_PADDING = 0.0;
    public static final double MAX_CELL_PADDING = 48.0;
    public static final TableStyle DEFAULT = new TableStyle(1.0, 5.0, true);

    public TableStyle {
        widthFraction = clamp(widthFraction, MIN_WIDTH_FRACTION, MAX_WIDTH_FRACTION, 1.0);
        cellPadding = clamp(cellPadding, MIN_CELL_PADDING, MAX_CELL_PADDING, 5.0);
    }

    public static TableStyle defaults() {
        return DEFAULT;
    }

    public TableStyle withWidthFraction(double widthFraction) {
        return new TableStyle(widthFraction, cellPadding, bordersEnabled);
    }

    public TableStyle withCellPadding(double cellPadding) {
        return new TableStyle(widthFraction, cellPadding, bordersEnabled);
    }

    public TableStyle withBordersEnabled(boolean bordersEnabled) {
        return new TableStyle(widthFraction, cellPadding, bordersEnabled);
    }

    private static double clamp(double value, double min, double max, double fallback) {
        if (!Double.isFinite(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }
}
