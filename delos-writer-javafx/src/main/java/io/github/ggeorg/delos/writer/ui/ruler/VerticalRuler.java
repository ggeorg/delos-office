package io.github.ggeorg.delos.writer.ui.ruler;

import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.ui.ViewTheme;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * Visual vertical page ruler for the writer workspace.
 *
 * <p>The ruler is intentionally read-only for now. It mirrors the editor
 * viewport's zoom and vertical scroll position, and shows page height plus
 * top/bottom margin markers. Margin editing belongs to a later page-setup
 * phase once the document model and native file format can persist it.</p>
 */
public final class VerticalRuler extends RulerCanvasPane {
    private static final double WIDTH = 28.0;
    private static final double MAJOR_TICK_WIDTH = 12.0;
    private static final double HALF_TICK_WIDTH = 8.0;
    private static final double QUARTER_TICK_WIDTH = 5.0;
    private static final double MARKER_SIZE = 6.0;

    private final DoubleProperty zoomFactor = new SimpleDoubleProperty(this, "zoomFactor", 1.0);
    private final DoubleProperty visibleContentY = new SimpleDoubleProperty(this, "visibleContentY", 0.0);
    private final DoubleProperty viewportHeight = new SimpleDoubleProperty(this, "viewportHeight", 0.0);
    private final DoubleProperty pageHeight = new SimpleDoubleProperty(this, "pageHeight", 842.0);
    private final DoubleProperty marginTop = new SimpleDoubleProperty(this, "marginTop", 72.0);
    private final DoubleProperty marginBottom = new SimpleDoubleProperty(this, "marginBottom", 72.0);
    private final DoubleProperty outerPadding = new SimpleDoubleProperty(this, "outerPadding", ViewTheme.defaultTheme().outerPadding());
    private final DoubleProperty interPageGap = new SimpleDoubleProperty(this, "interPageGap", ViewTheme.defaultTheme().interPageGap());

    public VerticalRuler() {
        super("vertical-ruler", WIDTH, 0.0);
        redrawWhenChanged(
                zoomFactor,
                visibleContentY,
                viewportHeight,
                pageHeight,
                marginTop,
                marginBottom,
                outerPadding,
                interPageGap
        );
    }

    public DoubleProperty zoomFactorProperty() {
        return zoomFactor;
    }

    public DoubleProperty visibleContentYProperty() {
        return visibleContentY;
    }

    public DoubleProperty viewportHeightProperty() {
        return viewportHeight;
    }

    public void setPageStyle(PageStyle pageStyle) {
        PageStyle safeStyle = Objects.requireNonNull(pageStyle, "pageStyle");
        pageHeight.set(Math.max(1.0, safeStyle.height()));
        marginTop.set(RulerMetrics.clamp(safeStyle.marginTop(), 0.0, safeStyle.height()));
        marginBottom.set(RulerMetrics.clamp(safeStyle.marginBottom(), 0.0, safeStyle.height()));
    }

    public void setViewTheme(ViewTheme theme) {
        ViewTheme safeTheme = Objects.requireNonNull(theme, "theme");
        outerPadding.set(Math.max(0.0, safeTheme.outerPadding()));
        interPageGap.set(Math.max(0.0, safeTheme.interPageGap()));
    }

    @Override
    protected void redraw() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0.0 || height <= 0.0) {
            return;
        }

        double zoom = RulerMetrics.safeZoom(zoomFactor.get());
        double pageHeightValue = Math.max(1.0, pageHeight.get());
        double firstVisibleY = Math.max(0.0, visibleContentY.get());
        double viewportHeightValue = viewportHeight.get() > 0.0 ? viewportHeight.get() : height;
        RulerMetrics.PageRange visiblePages = RulerMetrics.visiblePageRange(
                firstVisibleY,
                viewportHeightValue,
                pageHeightValue,
                interPageGap.get(),
                outerPadding.get(),
                zoom
        );
        double pixelsPerInch = RulerMetrics.pixelsPerInch(zoom);

        GraphicsContext gc = canvas().getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        gc.setFill(Color.web("#f8fafc"));
        gc.fillRect(0, 0, width, height);
        gc.setStroke(Color.web("#cbd5e1"));
        gc.strokeLine(width - 0.5, 0, width - 0.5, height);

        for (int pageIndex = visiblePages.firstPage(); pageIndex <= visiblePages.lastPage(); pageIndex++) {
            double pageTop = RulerMetrics.pageTopInViewport(
                    pageIndex,
                    pageHeightValue,
                    interPageGap.get(),
                    outerPadding.get(),
                    firstVisibleY,
                    zoom
            );
            double pageBottom = pageTop + pageHeightValue * zoom;
            if (pageBottom < -4.0 || pageTop > height + 4.0) {
                continue;
            }
            drawPageBand(gc, pageTop, pageBottom, width, height);
            drawTicks(gc, pageTop, pageBottom, pixelsPerInch, width, height);
            drawMargins(gc, pageTop, zoom, width, height);
        }
    }

    private void drawPageBand(GraphicsContext gc, double pageTop, double pageBottom, double width, double height) {
        double visibleTop = RulerMetrics.clamp(pageTop, 0.0, height);
        double visibleBottom = RulerMetrics.clamp(pageBottom, 0.0, height);
        if (visibleBottom <= visibleTop) {
            return;
        }
        gc.setFill(Color.web("#ffffff"));
        gc.fillRect(0.0, visibleTop, width, visibleBottom - visibleTop);
        gc.setStroke(Color.web("#d7dee8"));
        strokeHorizontal(gc, pageTop, 0.0, width);
        strokeHorizontal(gc, pageBottom, 0.0, width);
    }

    private void drawTicks(GraphicsContext gc, double pageTop, double pageBottom, double pixelsPerInch, double width, double height) {
        gc.setFill(Color.web("#475569"));
        gc.setStroke(Color.web("#94a3b8"));
        gc.setLineWidth(1.0);
        boolean showLabels = pixelsPerInch >= 36.0;
        int maxTick = RulerMetrics.quarterTickCount(pageBottom - pageTop, pixelsPerInch);
        for (int tick = 0; tick <= maxTick; tick++) {
            double y = pageTop + tick * pixelsPerInch / 4.0;
            if (y < -4.0 || y > height + 4.0) {
                continue;
            }
            int mod = tick % 4;
            double tickWidth = mod == 0 ? MAJOR_TICK_WIDTH : mod == 2 ? HALF_TICK_WIDTH : QUARTER_TICK_WIDTH;
            strokeHorizontal(gc, y, width - tickWidth, width - 2.0);
            if (showLabels && mod == 0) {
                int inch = tick / 4;
                gc.fillText(Integer.toString(inch), 3.0, y + 10.0);
            }
        }
    }

    private void drawMargins(GraphicsContext gc, double pageTop, double zoom, double width, double height) {
        double topMarginY = pageTop + marginTop.get() * zoom;
        double bottomMarginY = pageTop + (pageHeight.get() - marginBottom.get()) * zoom;
        gc.setStroke(Color.web("#2563eb"));
        gc.setFill(Color.web("#2563eb"));
        drawMarginMarker(gc, topMarginY, width, height);
        drawMarginMarker(gc, bottomMarginY, width, height);
    }

    private void drawMarginMarker(GraphicsContext gc, double y, double width, double height) {
        if (y < -8.0 || y > height + 8.0) {
            return;
        }
        double snapped = Math.round(y) + 0.5;
        gc.strokeLine(width - MAJOR_TICK_WIDTH - 2.0, snapped, width - 2.0, snapped);
        double x = 2.0;
        gc.fillPolygon(
                new double[] { x, x, x + MARKER_SIZE },
                new double[] { snapped - MARKER_SIZE, snapped + MARKER_SIZE, snapped },
                3
        );
    }

    private static void strokeHorizontal(GraphicsContext gc, double y, double x1, double x2) {
        double snapped = Math.round(y) + 0.5;
        gc.strokeLine(x1, snapped, x2, snapped);
    }
}
