package io.github.ggeorg.delos.writer.ui.geometry;

import io.github.ggeorg.delos.writer.layout.CaretGeometry;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.ui.ViewTheme;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

import java.util.List;
import java.util.Objects;

/**
 * Page-column geometry derived from the immutable layout model.
 *
 * <p>This is the view-side counterpart of the laid-out page tree: it centralizes
 * page offsets, total content dimensions, and caret bounds in content
 * coordinates. Keeping these calculations here avoids scattering view padding,
 * page gaps, and shadow extents across the viewport, synchronizer, and future
 * page virtualizer.</p>
 */
public final class PageGeometryIndex {
    private final List<LaidOutPage> pages;
    private final ViewTheme theme;
    private final double pageGap;
    private final double[] pageTopByIndex;
    private final double contentWidth;
    private final double contentHeight;

    private PageGeometryIndex(List<LaidOutPage> pages, ViewTheme theme, double pageGap) {
        this.pages = List.copyOf(Objects.requireNonNull(pages, "pages"));
        this.theme = Objects.requireNonNull(theme, "theme");
        this.pageGap = Math.max(0.0, pageGap);
        this.pageTopByIndex = buildPageTopIndex(this.pages, theme, this.pageGap);
        this.contentWidth = computeContentWidth(this.pages, theme);
        this.contentHeight = computeContentHeight(this.pages, theme, this.pageGap);
    }

    public static PageGeometryIndex from(LaidOutDocument layout, ViewTheme theme, double pageGap) {
        return new PageGeometryIndex(layout == null ? List.of() : layout.pages(), theme, pageGap);
    }

    public int pageCount() {
        return pages.size();
    }

    public LaidOutPage page(int pageIndex) {
        return pages.get(pageIndex);
    }

    public double pageTopInContent(int pageIndex) {
        return pageTopByIndex[pageIndex];
    }

    public double pageLeftInContent(int pageIndex) {
        checkPageIndex(pageIndex);
        return 0.0;
    }

    public double contentWidth() {
        return contentWidth;
    }

    public double contentHeight() {
        return contentHeight;
    }

    public Bounds caretBoundsInContent(int pageIndex, CaretGeometry caret) {
        Objects.requireNonNull(caret, "caret");
        double x = pageLeftInContent(pageIndex) + theme.shadowExtentX() + caret.x();
        double y = pageTopInContent(pageIndex) + theme.shadowExtentY() + caret.y();
        return new BoundingBox(x, y, 2.0, caret.height());
    }

    public int pageAtContentY(double contentY) {
        if (pages.isEmpty()) {
            return -1;
        }
        if (contentY <= pageTopByIndex[0]) {
            return 0;
        }
        for (int index = 0; index < pages.size(); index++) {
            double top = pageTopByIndex[index];
            double bottom = top + pageOuterHeight(pages.get(index), theme);
            if (contentY < bottom || index == pages.size() - 1) {
                return index;
            }
        }
        return pages.size() - 1;
    }

    private void checkPageIndex(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            throw new IndexOutOfBoundsException("pageIndex=" + pageIndex + ", pageCount=" + pages.size());
        }
    }

    private static double[] buildPageTopIndex(List<LaidOutPage> pages, ViewTheme theme, double pageGap) {
        double[] result = new double[pages.size()];
        double y = theme.outerPadding();
        for (int index = 0; index < pages.size(); index++) {
            result[index] = y;
            y += pageOuterHeight(pages.get(index), theme);
            if (index < pages.size() - 1) {
                y += pageGap;
            }
        }
        return result;
    }

    private static double computeContentWidth(List<LaidOutPage> pages, ViewTheme theme) {
        if (pages.isEmpty()) {
            return 0.0;
        }
        double maxPageWidth = 0.0;
        for (LaidOutPage page : pages) {
            maxPageWidth = Math.max(maxPageWidth, page.width());
        }
        return maxPageWidth + theme.shadowExtentX() * 2.0;
    }

    private static double computeContentHeight(List<LaidOutPage> pages, ViewTheme theme, double pageGap) {
        if (pages.isEmpty()) {
            return 0.0;
        }
        double height = theme.outerPadding() * 2.0;
        for (int index = 0; index < pages.size(); index++) {
            height += pageOuterHeight(pages.get(index), theme);
            if (index < pages.size() - 1) {
                height += pageGap;
            }
        }
        return height;
    }

    private static double pageOuterHeight(LaidOutPage page, ViewTheme theme) {
        return page.height() + theme.shadowExtentY() * 2.0;
    }
}
