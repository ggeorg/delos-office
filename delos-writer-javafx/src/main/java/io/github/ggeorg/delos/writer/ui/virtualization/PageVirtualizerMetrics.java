package io.github.ggeorg.delos.writer.ui.virtualization;

import io.github.ggeorg.delos.writer.ui.geometry.PageGeometryIndex;

/**
 * Computes the page window that should be materialized for a visible viewport.
 *
 * <p>Unlike RichTextFX/Flowless, Delos virtualizes pages, not arbitrary cells.
 * Page heights are known from the laid-out document, so the visible window can
 * be derived from absolute content coordinates without measuring scene graph
 * nodes or estimating row heights.</p>
 */
public final class PageVirtualizerMetrics {
    private final double overscanPages;

    public PageVirtualizerMetrics() {
        this(1.0);
    }

    public PageVirtualizerMetrics(double overscanPages) {
        this.overscanPages = Math.max(0.0, overscanPages);
    }

    public PageWindow visibleWindow(PageGeometryIndex geometry, double viewportMinY, double viewportHeight) {
        if (geometry == null || geometry.pageCount() == 0) {
            return PageWindow.empty();
        }

        double safeViewportMinY = finiteOrZero(viewportMinY);
        double safeViewportHeight = Math.max(0.0, finiteOrZero(viewportHeight));
        double overscan = averagePageStride(geometry) * overscanPages;
        double visibleTop = Math.max(0.0, safeViewportMinY - overscan);
        double visibleBottom = Math.min(geometry.contentHeight(), safeViewportMinY + safeViewportHeight + overscan);

        int first = geometry.pageAtContentY(visibleTop);
        int last = geometry.pageAtContentY(Math.max(visibleTop, visibleBottom));
        return PageWindow.of(first, last, geometry.pageCount());
    }

    private static double averagePageStride(PageGeometryIndex geometry) {
        if (geometry.pageCount() <= 1) {
            return geometry.contentHeight();
        }
        double firstTop = geometry.pageTopInContent(0);
        double lastTop = geometry.pageTopInContent(geometry.pageCount() - 1);
        return Math.max(1.0, (lastTop - firstTop) / (geometry.pageCount() - 1));
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    public record PageWindow(int firstPageIndex, int lastPageIndex) {
        public static PageWindow empty() {
            return new PageWindow(-1, -1);
        }

        public static PageWindow of(int firstPageIndex, int lastPageIndex, int pageCount) {
            if (pageCount <= 0 || firstPageIndex < 0 || lastPageIndex < 0) {
                return empty();
            }
            int first = Math.max(0, Math.min(firstPageIndex, pageCount - 1));
            int last = Math.max(first, Math.min(lastPageIndex, pageCount - 1));
            return new PageWindow(first, last);
        }

        public boolean isEmpty() {
            return firstPageIndex < 0 || lastPageIndex < firstPageIndex;
        }

        public boolean contains(int pageIndex) {
            return !isEmpty() && pageIndex >= firstPageIndex && pageIndex <= lastPageIndex;
        }
    }
}
