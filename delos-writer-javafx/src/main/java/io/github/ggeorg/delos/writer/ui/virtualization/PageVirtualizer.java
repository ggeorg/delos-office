package io.github.ggeorg.delos.writer.ui.virtualization;

import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.render.PageRenderer;
import io.github.ggeorg.delos.writer.ui.PageView;
import io.github.ggeorg.delos.writer.ui.ViewTheme;
import io.github.ggeorg.delos.writer.ui.geometry.PageGeometryIndex;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.writer.render.fx.JavaFxRenderTextMeasurer;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Virtualized page surface for Delos Writer.
 *
 * <p>The virtualizer materializes only the pages that intersect the visible
 * viewport plus a small overscan window. It deliberately virtualizes fixed-size
 * laid-out pages instead of paragraphs or arbitrary cells, so it does not need
 * RichTextFX/Flowless-style cell measurement or height estimation.</p>
 */
public final class PageVirtualizer extends Region {
    private final ViewTheme theme;
    private final PageViewPool pageViewPool;
    private final PageVirtualizerMetrics metrics;
    private final Map<Integer, PageView> activeViews = new TreeMap<>();

    private LaidOutDocument layout;
    private PageGeometryIndex geometry = PageGeometryIndex.from(null, ViewTheme.defaultTheme(), 0.0);
    private Bounds visibleViewportInContent = new BoundingBox(0.0, 0.0, 1.0, 1.0);

    public PageVirtualizer(ViewTheme theme, PageRenderer pageRenderer) {
        this(theme, pageRenderer, new JavaFxRenderTextMeasurer(), new PageVirtualizerMetrics());
    }

    public PageVirtualizer(ViewTheme theme, PageRenderer pageRenderer, PageVirtualizerMetrics metrics) {
        this(theme, pageRenderer, new JavaFxRenderTextMeasurer(), metrics);
    }

    public PageVirtualizer(ViewTheme theme, PageRenderer pageRenderer, RenderTextMeasurer renderTextMeasurer) {
        this(theme, pageRenderer, renderTextMeasurer, new PageVirtualizerMetrics());
    }

    public PageVirtualizer(ViewTheme theme, PageRenderer pageRenderer, RenderTextMeasurer renderTextMeasurer, PageVirtualizerMetrics metrics) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.pageViewPool = new PageViewPool(
                theme,
                Objects.requireNonNull(pageRenderer, "pageRenderer"),
                renderTextMeasurer
        );
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        setManaged(true);
    }

    public void setLayout(LaidOutDocument layout) {
        if (this.layout == layout) {
            return;
        }
        this.layout = layout;
        this.geometry = PageGeometryIndex.from(layout, theme, theme.interPageGap());
        materializeVisiblePages(true);
        requestLayout();
    }

    public void setVisibleViewport(Bounds visibleViewportInContent) {
        if (visibleViewportInContent == null) {
            return;
        }
        this.visibleViewportInContent = visibleViewportInContent;
        materializeVisiblePages(false);
        requestLayout();
    }

    public PageGeometryIndex geometryIndex() {
        return geometry;
    }

    public Collection<PageView> activePageViews() {
        return List.copyOf(activeViews.values());
    }

    public void forEachActivePageView(Consumer<PageView> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        activeViews.values().forEach(consumer);
    }

    public double contentWidth() {
        return geometry.contentWidth();
    }

    public double contentHeight() {
        return geometry.contentHeight();
    }

    public int materializedPageCount() {
        return activeViews.size();
    }

    @Override
    protected double computePrefWidth(double height) {
        return contentWidth();
    }

    @Override
    protected double computePrefHeight(double width) {
        return contentHeight();
    }

    @Override
    protected void layoutChildren() {
        materializeVisiblePages(false);
        for (Map.Entry<Integer, PageView> entry : activeViews.entrySet()) {
            int pageIndex = entry.getKey();
            PageView pageView = entry.getValue();
            double x = geometry.pageLeftInContent(pageIndex);
            double y = geometry.pageTopInContent(pageIndex);
            pageView.resizeRelocate(x, y, pageView.prefWidth(-1), pageView.prefHeight(-1));
        }
    }

    private void materializeVisiblePages(boolean refreshExistingPages) {
        PageVirtualizerMetrics.PageWindow window = visiblePageWindow();
        releasePagesOutside(window);
        if (refreshExistingPages) {
            refreshActivePageContent();
        }
        acquireMissingPages(window);
    }

    private void refreshActivePageContent() {
        if (geometry.pageCount() == 0) {
            return;
        }
        for (Map.Entry<Integer, PageView> entry : activeViews.entrySet()) {
            int pageIndex = entry.getKey();
            if (pageIndex >= 0 && pageIndex < geometry.pageCount()) {
                LaidOutPage page = geometry.page(pageIndex);
                if (entry.getValue().page() != page) {
                    entry.getValue().setPage(page);
                }
            }
        }
    }

    private PageVirtualizerMetrics.PageWindow visiblePageWindow() {
        if (layout == null || geometry.pageCount() == 0) {
            return PageVirtualizerMetrics.PageWindow.empty();
        }
        return metrics.visibleWindow(geometry, visibleViewportInContent.getMinY(), visibleViewportInContent.getHeight());
    }

    private void releasePagesOutside(PageVirtualizerMetrics.PageWindow window) {
        List<Integer> toRemove = new ArrayList<>();
        for (Integer pageIndex : activeViews.keySet()) {
            if (!window.contains(pageIndex)) {
                toRemove.add(pageIndex);
            }
        }
        for (Integer pageIndex : toRemove) {
            PageView removed = activeViews.remove(pageIndex);
            getChildren().remove(removed);
            pageViewPool.release(removed);
        }
    }

    private void acquireMissingPages(PageVirtualizerMetrics.PageWindow window) {
        if (window.isEmpty()) {
            return;
        }
        for (int pageIndex = window.firstPageIndex(); pageIndex <= window.lastPageIndex(); pageIndex++) {
            if (activeViews.containsKey(pageIndex)) {
                continue;
            }
            LaidOutPage page = geometry.page(pageIndex);
            PageView pageView = pageViewPool.acquire(page);
            activeViews.put(pageIndex, pageView);
            getChildren().add(pageView);
        }
    }
}
