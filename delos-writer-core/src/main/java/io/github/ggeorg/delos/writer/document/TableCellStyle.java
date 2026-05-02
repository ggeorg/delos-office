package io.github.ggeorg.delos.writer.document;

/**
 * Passive table-cell styling used by layout/export/reporting.
 *
 * <p>Colors are stored as CSS-style hex strings (for example {@code #F3F4F6})
 * so the document model stays renderer-neutral and server-safe.</p>
 */
public record TableCellStyle(String backgroundColor) {
    public static final TableCellStyle NONE = new TableCellStyle(null);

    public TableCellStyle {
        backgroundColor = normalize(backgroundColor);
    }

    public static TableCellStyle none() {
        return NONE;
    }

    public static TableCellStyle background(String color) {
        return new TableCellStyle(color);
    }

    public boolean hasBackground() {
        return backgroundColor != null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
