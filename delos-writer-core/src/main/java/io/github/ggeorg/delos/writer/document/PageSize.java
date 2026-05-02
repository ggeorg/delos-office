package io.github.ggeorg.delos.writer.document;

import java.util.Objects;

/**
 * Named physical page size expressed in PDF points.
 *
 * <p>The stored width and height are the portrait dimensions. Orientation is
 * applied separately by {@link PageStyle#of(PageSize, PageOrientation, PageMargins)}
 * so server-side report code can choose a standard page deliberately instead of
 * passing unexplained raw numbers around.</p>
 */
public record PageSize(String name, double width, double height) {
    public static final PageSize A4 = new PageSize("A4", 595.0, 842.0);
    public static final PageSize LETTER = new PageSize("Letter", 612.0, 792.0);
    public static final PageSize LEGAL = new PageSize("Legal", 612.0, 1008.0);

    public PageSize {
        name = Objects.requireNonNullElse(name, "Custom").trim();
        if (name.isEmpty()) {
            name = "Custom";
        }
        requirePositiveFinite("width", width);
        requirePositiveFinite("height", height);
    }

    double widthFor(PageOrientation orientation) {
        return orientation == PageOrientation.LANDSCAPE ? Math.max(width, height) : Math.min(width, height);
    }

    double heightFor(PageOrientation orientation) {
        return orientation == PageOrientation.LANDSCAPE ? Math.min(width, height) : Math.max(width, height);
    }

    private static void requirePositiveFinite(String field, double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException("Page size " + field + " must be a positive finite value");
        }
    }
}
