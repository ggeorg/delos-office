package io.github.ggeorg.delos.writer.ui.ruler;

import io.github.ggeorg.delos.writer.document.PageStyle;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * Visual horizontal page ruler for the writer workspace.
 *
 * <p>The ruler is read-only in v1. It mirrors the document viewport's zoom,
 * horizontal scroll, page width, and left/right margins. Interaction such as
 * dragging margins, indents, and tab stops belongs to later document-model and
 * native-format phases.</p>
 */
public final class HorizontalRuler extends RulerCanvasPane {
    private static final double HEIGHT = 28.0;
    private static final double MAJOR_TICK_HEIGHT = 12.0;
    private static final double HALF_TICK_HEIGHT = 8.0;
    private static final double QUARTER_TICK_HEIGHT = 5.0;
    private static final double MARKER_HEIGHT = 6.0;

    private final DoubleProperty zoomFactor = new SimpleDoubleProperty(this, "zoomFactor", 1.0);
    private final DoubleProperty visibleContentX = new SimpleDoubleProperty(this, "visibleContentX", 0.0);
    private final DoubleProperty viewportWidth = new SimpleDoubleProperty(this, "viewportWidth", 0.0);
    private final DoubleProperty pageWidth = new SimpleDoubleProperty(this, "pageWidth", 595.0);
    private final DoubleProperty marginLeft = new SimpleDoubleProperty(this, "marginLeft", 72.0);
    private final DoubleProperty marginRight = new SimpleDoubleProperty(this, "marginRight", 72.0);

    public HorizontalRuler() {
        super("horizontal-ruler", 0.0, HEIGHT);
        redrawWhenChanged(
                zoomFactor,
                visibleContentX,
                viewportWidth,
                pageWidth,
                marginLeft,
                marginRight
        );
    }

    public DoubleProperty zoomFactorProperty() {
        return zoomFactor;
    }

    public DoubleProperty visibleContentXProperty() {
        return visibleContentX;
    }

    public DoubleProperty viewportWidthProperty() {
        return viewportWidth;
    }

    public void setPageStyle(PageStyle pageStyle) {
        PageStyle safeStyle = Objects.requireNonNull(pageStyle, "pageStyle");
        pageWidth.set(Math.max(1.0, safeStyle.width()));
        marginLeft.set(RulerMetrics.clamp(safeStyle.marginLeft(), 0.0, safeStyle.width()));
        marginRight.set(RulerMetrics.clamp(safeStyle.marginRight(), 0.0, safeStyle.width()));
    }

    @Override
    protected void redraw() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        double zoom = RulerMetrics.safeZoom(zoomFactor.get());
        double pageWidthValue = Math.max(1.0, pageWidth.get());
        double availableViewportWidth = viewportWidth.get() > 0.0 ? viewportWidth.get() : width;
        double pageLeft = RulerMetrics.horizontalPageLeft(availableViewportWidth, pageWidthValue, zoom, visibleContentX.get());
        double pageRight = pageLeft + pageWidthValue * zoom;
        double pixelsPerInch = RulerMetrics.pixelsPerInch(zoom);

        GraphicsContext gc = canvas().getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        gc.setFill(Color.web("#f8fafc"));
        gc.fillRect(0, 0, width, height);
        gc.setStroke(Color.web("#cbd5e1"));
        gc.strokeLine(0, height - 0.5, width, height - 0.5);

        drawPageBand(gc, pageLeft, pageRight, height);
        drawTicks(gc, pageLeft, pageRight, pixelsPerInch, height);
        drawMargins(gc, pageLeft, zoom, height);
    }

    private void drawPageBand(GraphicsContext gc, double pageLeft, double pageRight, double height) {
        double visibleLeft = RulerMetrics.clamp(pageLeft, 0.0, getWidth());
        double visibleRight = RulerMetrics.clamp(pageRight, 0.0, getWidth());
        if (visibleRight <= visibleLeft) {
            return;
        }
        gc.setFill(Color.web("#ffffff"));
        gc.fillRect(visibleLeft, 0, visibleRight - visibleLeft, height);
        gc.setStroke(Color.web("#d7dee8"));
        strokeVertical(gc, pageLeft, 0.0, height);
        strokeVertical(gc, pageRight, 0.0, height);
    }

    private void drawTicks(GraphicsContext gc, double pageLeft, double pageRight, double pixelsPerInch, double height) {
        gc.setFill(Color.web("#475569"));
        gc.setStroke(Color.web("#94a3b8"));
        gc.setLineWidth(1.0);
        boolean showLabels = pixelsPerInch >= 36.0;
        int maxTick = RulerMetrics.quarterTickCount(pageRight - pageLeft, pixelsPerInch);
        for (int tick = 0; tick <= maxTick; tick++) {
            double x = pageLeft + tick * pixelsPerInch / 4.0;
            if (x < -4.0 || x > getWidth() + 4.0) {
                continue;
            }
            int mod = tick % 4;
            double tickHeight = mod == 0 ? MAJOR_TICK_HEIGHT : mod == 2 ? HALF_TICK_HEIGHT : QUARTER_TICK_HEIGHT;
            strokeVertical(gc, x, height - tickHeight, height - 2.0);
            if (showLabels && mod == 0) {
                int inch = tick / 4;
                gc.fillText(Integer.toString(inch), x + 3.0, 11.0);
            }
        }
    }

    private void drawMargins(GraphicsContext gc, double pageLeft, double zoom, double height) {
        double leftMarginX = pageLeft + marginLeft.get() * zoom;
        double rightMarginX = pageLeft + (pageWidth.get() - marginRight.get()) * zoom;
        gc.setStroke(Color.web("#2563eb"));
        gc.setFill(Color.web("#2563eb"));
        drawMarginMarker(gc, leftMarginX, height);
        drawMarginMarker(gc, rightMarginX, height);
    }

    private void drawMarginMarker(GraphicsContext gc, double x, double height) {
        if (x < -8.0 || x > getWidth() + 8.0) {
            return;
        }
        double snapped = Math.round(x) + 0.5;
        gc.strokeLine(snapped, height - MAJOR_TICK_HEIGHT - 2.0, snapped, height - 2.0);
        double y = 2.0;
        gc.fillPolygon(
                new double[] { snapped - MARKER_HEIGHT, snapped + MARKER_HEIGHT, snapped },
                new double[] { y, y, y + MARKER_HEIGHT },
                3
        );
    }

    private static void strokeVertical(GraphicsContext gc, double x, double y1, double y2) {
        double snapped = Math.round(x) + 0.5;
        gc.strokeLine(snapped, y1, snapped, y2);
    }
}
