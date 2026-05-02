package io.github.ggeorg.delos.writer.ui;

import io.github.ggeorg.delos.writer.layout.HitTestResult;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.layout.PageHitTester;
import io.github.ggeorg.delos.writer.render.PageRenderContext;
import io.github.ggeorg.delos.writer.render.PageRenderState;
import io.github.ggeorg.delos.writer.render.PageRenderer;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.writer.render.fx.JavaFxRenderTarget;
import io.github.ggeorg.delos.writer.render.fx.JavaFxRenderTextMeasurer;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Region;

/**
 * One page node in the scene graph.
 * <p>
 * The page view is intentionally dumb: it exposes hit testing and receives an
 * externally computed render state.
 */
public final class PageView extends Region {
    private LaidOutPage page;
    private final ViewTheme theme;
    private final PageRenderer renderer;
    private final PageHitTester hitTester;
    private final RenderTextMeasurer renderTextMeasurer;
    private final Canvas canvas;
    private final JavaFxRenderTarget renderTarget;
    private PageRenderState renderState = PageRenderState.EMPTY;
    private boolean dirty = true;

    public PageView(LaidOutPage page, ViewTheme theme, PageRenderer renderer) {
        this(page, theme, renderer, new PageHitTester());
    }

    public PageView(
            LaidOutPage page,
            ViewTheme theme,
            PageRenderer renderer,
            PageHitTester hitTester
    ) {
        this.page = page;
        this.theme = theme;
        this.renderer = renderer;
        this.hitTester = hitTester;
        this.renderTextMeasurer = new JavaFxRenderTextMeasurer();
        this.canvas = new Canvas();
        this.renderTarget = new JavaFxRenderTarget(canvas.getGraphicsContext2D());
        getChildren().add(canvas);
    }

    public LaidOutPage page() {
        return page;
    }

    public void setPage(LaidOutPage page) {
        if (page == null || this.page == page) {
            return;
        }
        this.page = page;
        markDirty();
    }

    public HitTestResult hitTest(double localX, double localY) {
        double pageLocalX = localX - theme.shadowExtentX();
        double pageLocalY = localY - theme.shadowExtentY();
        return hitTester.hitTest(page, pageLocalX, pageLocalY);
    }

    public void setRenderState(PageRenderState renderState) {
        PageRenderState safeState = renderState == null ? PageRenderState.EMPTY : renderState;
        if (this.renderState.equals(safeState)) {
            return;
        }
        this.renderState = safeState;
        markDirty();
    }

    @Override
    protected double computePrefWidth(double height) {
        return page.width() + theme.shadowExtentX() * 2;
    }

    @Override
    protected double computePrefHeight(double width) {
        return page.height() + theme.shadowExtentY() * 2;
    }

    @Override
    protected void layoutChildren() {
        double width = computePrefWidth(-1);
        double height = computePrefHeight(-1);

        if (canvas.getWidth() != width) {
            canvas.setWidth(width);
            dirty = true;
        }
        if (canvas.getHeight() != height) {
            canvas.setHeight(height);
            dirty = true;
        }
        canvas.relocate(0, 0);

        if (dirty) {
            paint();
            dirty = false;
        }
    }

    private void markDirty() {
        dirty = true;
        requestLayout();
    }

    private void paint() {
        renderer.renderPage(
                renderTarget,
                PageRenderContext.editor(page, theme.renderTheme(), renderTextMeasurer, renderState)
        );
    }
}
