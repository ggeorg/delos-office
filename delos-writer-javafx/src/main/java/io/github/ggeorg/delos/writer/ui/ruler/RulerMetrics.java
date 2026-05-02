package io.github.ggeorg.delos.writer.ui.ruler;

/**
 * Pure ruler geometry helpers.
 *
 * <p>Rulers are visual chrome around the editor viewport. They must derive
 * their positions from the same zoom/scroll/page metrics as the document view,
 * without adding their own padding or layout policy. Keeping the math here
 * makes ruler synchronization testable and prevents another app-shell/editor
 * geometry leak.</p>
 */
public final class RulerMetrics {
    public static final double BASE_PIXELS_PER_INCH = 72.0;
    public static final double MIN_ZOOM = 0.1;

    private RulerMetrics() {
    }

    public static double safeZoom(double zoom) {
        return Math.max(MIN_ZOOM, zoom);
    }

    public static double pixelsPerInch(double zoom) {
        return BASE_PIXELS_PER_INCH * safeZoom(zoom);
    }

    public static double horizontalPageLeft(double viewportWidth, double pageWidth, double zoom, double visibleContentX) {
        double z = safeZoom(zoom);
        double scaledPageWidth = Math.max(1.0, pageWidth) * z;
        return centeredInset(Math.max(0.0, viewportWidth), scaledPageWidth) - Math.max(0.0, visibleContentX) * z;
    }

    public static double pageTopInViewport(int pageIndex,
                                           double pageHeight,
                                           double interPageGap,
                                           double outerPadding,
                                           double visibleContentY,
                                           double zoom) {
        if (pageIndex < 0) {
            throw new IllegalArgumentException("pageIndex must be >= 0");
        }
        double stride = pageStride(pageHeight, interPageGap);
        double pageTopInContent = Math.max(0.0, outerPadding) + pageIndex * stride;
        return (pageTopInContent - Math.max(0.0, visibleContentY)) * safeZoom(zoom);
    }

    public static PageRange visiblePageRange(double visibleContentY,
                                             double viewportHeight,
                                             double pageHeight,
                                             double interPageGap,
                                             double outerPadding,
                                             double zoom) {
        double z = safeZoom(zoom);
        double unscaledViewportHeight = Math.max(0.0, viewportHeight) / z;
        double flowStart = Math.max(0.0, Math.max(0.0, visibleContentY) - Math.max(0.0, outerPadding));
        double flowEnd = Math.max(flowStart, Math.max(0.0, visibleContentY) + unscaledViewportHeight - Math.max(0.0, outerPadding));
        double stride = pageStride(pageHeight, interPageGap);
        int firstPage = Math.max(0, (int) Math.floor(flowStart / stride));
        int lastPage = Math.max(firstPage, (int) Math.ceil(flowEnd / stride));
        return new PageRange(firstPage, lastPage);
    }

    public static int quarterTickCount(double scaledLength, double pixelsPerInch) {
        double quarterStep = Math.max(1.0, pixelsPerInch / 4.0);
        return (int) Math.ceil(Math.max(0.0, scaledLength) / quarterStep);
    }

    public static double centeredInset(double viewportSize, double scaledContentSize) {
        return Math.max((Math.max(0.0, viewportSize) - Math.max(0.0, scaledContentSize)) / 2.0, 0.0);
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double pageStride(double pageHeight, double interPageGap) {
        return Math.max(1.0, pageHeight) + Math.max(0.0, interPageGap);
    }

    public record PageRange(int firstPage, int lastPage) {
        public PageRange {
            if (firstPage < 0) {
                throw new IllegalArgumentException("firstPage must be >= 0");
            }
            if (lastPage < firstPage) {
                throw new IllegalArgumentException("lastPage must be >= firstPage");
            }
        }
    }
}
