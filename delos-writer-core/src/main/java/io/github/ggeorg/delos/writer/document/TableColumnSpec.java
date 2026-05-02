package io.github.ggeorg.delos.writer.document;

/**
 * Column sizing hint for a table.
 *
 * <p>The width weight is relative to the table's available width. A table with
 * weights {@code 2, 1, 1} therefore gives the first column half the width and
 * each remaining column a quarter.</p>
 */
public record TableColumnSpec(double widthWeight) {
    public TableColumnSpec {
        if (Double.isNaN(widthWeight) || Double.isInfinite(widthWeight) || widthWeight <= 0.0) {
            throw new IllegalArgumentException("widthWeight must be finite and > 0");
        }
    }

    public static TableColumnSpec equal() {
        return new TableColumnSpec(1.0);
    }
}
