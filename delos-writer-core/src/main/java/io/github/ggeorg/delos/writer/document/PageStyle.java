package io.github.ggeorg.delos.writer.document;

import java.util.Objects;

/**
 * Immutable page metrics in PDF points.
 *
 * <p>{@link PageSetup}, {@link PageSize}, {@link PageOrientation}, and
 * {@link PageMargins} are the higher-level production model. This type remains
 * the compact layout-facing value object used by pagination, rendering,
 * serialization, and PDF export.</p>
 */
public record PageStyle(
        double width,
        double height,
        double marginTop,
        double marginRight,
        double marginBottom,
        double marginLeft
) {
    public PageStyle {
        requirePositiveFinite("width", width);
        requirePositiveFinite("height", height);
        requireNonNegativeFinite("marginTop", marginTop);
        requireNonNegativeFinite("marginRight", marginRight);
        requireNonNegativeFinite("marginBottom", marginBottom);
        requireNonNegativeFinite("marginLeft", marginLeft);
        if (width - marginLeft - marginRight <= 0.0) {
            throw new IllegalArgumentException("Page horizontal margins leave no printable content width");
        }
        if (height - marginTop - marginBottom <= 0.0) {
            throw new IllegalArgumentException("Page vertical margins leave no printable content height");
        }
    }

    public static PageStyle a4Default() {
        return PageSetup.a4Default().toPageStyle();
    }

    public static PageStyle of(PageSize size, PageOrientation orientation, PageMargins margins) {
        PageSize safeSize = Objects.requireNonNull(size, "size");
        PageOrientation safeOrientation = Objects.requireNonNull(orientation, "orientation");
        PageMargins safeMargins = Objects.requireNonNull(margins, "margins");
        return new PageStyle(
                safeSize.widthFor(safeOrientation),
                safeSize.heightFor(safeOrientation),
                safeMargins.top(),
                safeMargins.right(),
                safeMargins.bottom(),
                safeMargins.left()
        );
    }

    public double contentWidth() {
        return width - marginLeft - marginRight;
    }

    public double contentHeight() {
        return height - marginTop - marginBottom;
    }

    public PageMargins margins() {
        return new PageMargins(marginTop, marginRight, marginBottom, marginLeft);
    }

    public PageOrientation orientation() {
        return width >= height ? PageOrientation.LANDSCAPE : PageOrientation.PORTRAIT;
    }

    public boolean isLandscape() {
        return orientation() == PageOrientation.LANDSCAPE;
    }

    public boolean isPortrait() {
        return orientation() == PageOrientation.PORTRAIT;
    }

    public PageStyle withMargins(PageMargins margins) {
        PageMargins safeMargins = Objects.requireNonNull(margins, "margins");
        return new PageStyle(width, height, safeMargins.top(), safeMargins.right(), safeMargins.bottom(), safeMargins.left());
    }

    public PageStyle withOrientation(PageOrientation orientation) {
        PageOrientation safeOrientation = Objects.requireNonNull(orientation, "orientation");
        double shortSide = Math.min(width, height);
        double longSide = Math.max(width, height);
        return safeOrientation == PageOrientation.LANDSCAPE
                ? new PageStyle(longSide, shortSide, marginTop, marginRight, marginBottom, marginLeft)
                : new PageStyle(shortSide, longSide, marginTop, marginRight, marginBottom, marginLeft);
    }

    public PageStyle portrait() {
        return withOrientation(PageOrientation.PORTRAIT);
    }

    public PageStyle landscape() {
        return withOrientation(PageOrientation.LANDSCAPE);
    }

    private static void requirePositiveFinite(String field, double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException("PageStyle " + field + " must be a positive finite value");
        }
    }

    private static void requireNonNegativeFinite(String field, double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException("PageStyle " + field + " must be a non-negative finite value");
        }
    }
}
