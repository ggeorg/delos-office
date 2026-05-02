package io.github.ggeorg.delos.writer.ui.control;

import io.github.ggeorg.delos.javafx.ZoomMath;
import io.github.ggeorg.delos.javafx.ZoomViewportHost;
import io.github.ggeorg.delos.writer.ui.ScrollIntoViewCoordinator;
import javafx.application.Platform;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.transform.Scale;

import java.util.Objects;

final class WriterDocumentViewZoomController {
    private static final double FIT_WIDTH_GUARD_PX = 2.0;

    private final WriterDocumentView documentView;
    private final DelosEditor editor;
    private final ScrollPane scrollPane;
    private final ZoomViewportHost zoomHost;
    private final Scale zoomScale = new Scale(1.0, 1.0);
    private boolean fitWidthMode;

    WriterDocumentViewZoomController(WriterDocumentView documentView, ScrollPane scrollPane, ZoomViewportHost zoomHost) {
        this.documentView = Objects.requireNonNull(documentView, "documentView");
        this.editor = documentView.editor();
        this.scrollPane = Objects.requireNonNull(scrollPane, "scrollPane");
        this.zoomHost = Objects.requireNonNull(zoomHost, "zoomHost");

        editor.zoomProperty().addListener((obs, oldValue, newValue) -> {
            applyZoom(newValue.doubleValue());
            refreshVisibleViewport();
        });
        scrollPane.hvalueProperty().addListener((obs, oldValue, newValue) -> refreshVisibleViewport());
        scrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> refreshVisibleViewport());
        scrollPane.viewportBoundsProperty().addListener((obs, oldValue, newValue) -> {
            zoomHost.setViewportBounds(newValue);
            if (fitWidthMode) {
                Platform.runLater(this::zoomToFitWidth);
            } else {
                refreshVisibleViewport();
            }
        });
        applyZoom(editor.zoom());
        refreshVisibleViewport();
    }

    Scale zoomScale() {
        return zoomScale;
    }

    void handleZoomScroll(ScrollEvent event) {
        if (!event.isShortcutDown()) {
            return;
        }
        event.consume();
        double delta = event.getDeltaY() > 0 ? 1.10 : 0.90;
        setZoomFactor(editor.zoom() * delta);
    }

    void setZoomFactor(double factor) {
        setZoomFactor(factor, false);
    }

    void zoomToFitWidth() {
        fitWidthMode = true;
        setZoomFactor(computeFitWidthZoom(), true);
    }

    boolean isFitWidthMode() {
        return fitWidthMode;
    }

    void ensureVisible(Bounds targetBounds) {
        if (targetBounds == null) {
            return;
        }

        Bounds viewportBounds = scrollPane.getViewportBounds();
        if (viewportBounds == null) {
            return;
        }

        double zoom = editor.zoom();
        double contentHeight = editor.scrollableContentHeight() * zoom;
        double contentWidth = editor.scrollableContentWidth() * zoom;

        double targetMinY = targetBounds.getMinY() * zoom;
        double targetMaxY = targetBounds.getMaxY() * zoom;
        double currentVOffset = ScrollIntoViewCoordinator.currentOffset(scrollPane.getVvalue(), viewportBounds.getHeight(), contentHeight);
        double adjustedVOffset = ScrollIntoViewCoordinator.adjustedOffset(currentVOffset, viewportBounds.getHeight(), contentHeight, targetMinY, targetMaxY, 24.0);
        scrollPane.setVvalue(ScrollIntoViewCoordinator.normalizedValue(adjustedVOffset, viewportBounds.getHeight(), contentHeight));

        double targetMinX = targetBounds.getMinX() * zoom;
        double targetMaxX = targetBounds.getMaxX() * zoom;
        double currentHOffset = ScrollIntoViewCoordinator.currentOffset(scrollPane.getHvalue(), viewportBounds.getWidth(), contentWidth);
        double adjustedHOffset = ScrollIntoViewCoordinator.adjustedOffset(currentHOffset, viewportBounds.getWidth(), contentWidth, targetMinX, targetMaxX, 12.0);
        scrollPane.setHvalue(ScrollIntoViewCoordinator.normalizedValue(adjustedHOffset, viewportBounds.getWidth(), contentWidth));
        refreshVisibleViewport();
    }

    private void setZoomFactor(double factor, boolean keepFitWidthMode) {
        if (!keepFitWidthMode) {
            fitWidthMode = false;
        }
        ViewportCenter center = captureViewportCenter();
        double clamped = ZoomMath.clampZoom(factor);
        if (Double.compare(editor.zoom(), clamped) != 0) {
            editor.setZoom(clamped);
        } else {
            applyZoom(clamped);
            refreshVisibleViewport();
        }
        restoreViewportCenter(center);
        Platform.runLater(() -> restoreViewportCenter(center));
    }

    private double computeFitWidthZoom() {
        Bounds viewportBounds = scrollPane.getViewportBounds();
        double viewportWidth = viewportBounds == null ? 0.0 : viewportBounds.getWidth();
        double contentWidth = editor.scrollableContentWidth();
        return ZoomMath.fitWidthZoom(Math.max(0.0, viewportWidth - FIT_WIDTH_GUARD_PX), contentWidth);
    }

    private void refreshVisibleViewport() {
        Bounds viewportBounds = scrollPane.getViewportBounds();
        if (viewportBounds == null) {
            return;
        }

        double zoom = Math.max(editor.zoom(), 0.0001);
        double scaledContentHeight = editor.scrollableContentHeight() * zoom;
        double scaledContentWidth = editor.scrollableContentWidth() * zoom;

        double visibleX = unscaledVisibleStart(scrollPane.getHvalue(), viewportBounds.getWidth(), scaledContentWidth, zoom, true);
        double visibleY = unscaledVisibleStart(scrollPane.getVvalue(), viewportBounds.getHeight(), scaledContentHeight, zoom, false);
        double visibleWidth = viewportBounds.getWidth() / zoom;
        double visibleHeight = viewportBounds.getHeight() / zoom;
        documentView.updateVisibleViewport(visibleX, visibleY, viewportBounds.getWidth(), viewportBounds.getHeight());
        editor.setVisibleViewport(new BoundingBox(visibleX, visibleY, visibleWidth, visibleHeight));
    }

    private ViewportCenter captureViewportCenter() {
        Bounds viewportBounds = scrollPane.getViewportBounds();
        if (viewportBounds == null) {
            return null;
        }

        double zoom = Math.max(editor.zoom(), 0.0001);
        double contentWidth = editor.scrollableContentWidth();
        double contentHeight = editor.scrollableContentHeight();
        double scaledContentWidth = contentWidth * zoom;
        double scaledContentHeight = contentHeight * zoom;

        double centerX = unscaledVisibleCenter(scrollPane.getHvalue(), viewportBounds.getWidth(), scaledContentWidth, zoom, true);
        double centerY = unscaledVisibleCenter(scrollPane.getVvalue(), viewportBounds.getHeight(), scaledContentHeight, zoom, false);
        return new ViewportCenter(clamp(centerX, 0.0, contentWidth), clamp(centerY, 0.0, contentHeight));
    }

    private void restoreViewportCenter(ViewportCenter center) {
        if (center == null) {
            return;
        }
        Bounds viewportBounds = scrollPane.getViewportBounds();
        if (viewportBounds == null) {
            return;
        }

        double zoom = Math.max(editor.zoom(), 0.0001);
        double scaledContentWidth = editor.scrollableContentWidth() * zoom;
        double scaledContentHeight = editor.scrollableContentHeight() * zoom;

        double xOffset = scaledOffsetForCenter(center.x(), viewportBounds.getWidth(), scaledContentWidth, zoom, true);
        double yOffset = scaledOffsetForCenter(center.y(), viewportBounds.getHeight(), scaledContentHeight, zoom, false);

        scrollPane.setHvalue(ScrollIntoViewCoordinator.normalizedValue(xOffset, viewportBounds.getWidth(), scaledContentWidth));
        scrollPane.setVvalue(ScrollIntoViewCoordinator.normalizedValue(yOffset, viewportBounds.getHeight(), scaledContentHeight));
        refreshVisibleViewport();
    }

    private static double unscaledVisibleStart(double scrollValue, double viewportSize, double scaledContentSize, double zoom, boolean centerWhenFitting) {
        double offset = ScrollIntoViewCoordinator.currentOffset(scrollValue, viewportSize, scaledContentSize);
        double inset = centeredInset(viewportSize, scaledContentSize, centerWhenFitting);
        return Math.max(0.0, (offset - inset) / zoom);
    }

    private static double unscaledVisibleCenter(double scrollValue, double viewportSize, double scaledContentSize, double zoom, boolean centerWhenFitting) {
        double offset = ScrollIntoViewCoordinator.currentOffset(scrollValue, viewportSize, scaledContentSize);
        double inset = centeredInset(viewportSize, scaledContentSize, centerWhenFitting);
        return Math.max(0.0, (offset + viewportSize / 2.0 - inset) / zoom);
    }

    private static double scaledOffsetForCenter(double unscaledCenter, double viewportSize, double scaledContentSize, double zoom, boolean centerWhenFitting) {
        double inset = centeredInset(viewportSize, scaledContentSize, centerWhenFitting);
        double offset = unscaledCenter * zoom + inset - viewportSize / 2.0;
        double maxOffset = Math.max(scaledContentSize - viewportSize, 0.0);
        return clamp(offset, 0.0, maxOffset);
    }

    private static double centeredInset(double viewportSize, double scaledContentSize, boolean centerWhenFitting) {
        return centerWhenFitting ? Math.max((viewportSize - scaledContentSize) / 2.0, 0.0) : 0.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void applyZoom(double requestedZoom) {
        double z = ZoomMath.clampZoom(requestedZoom);
        zoomScale.setX(z);
        zoomScale.setY(z);
        zoomHost.setZoomFactor(z);
    }

    private record ViewportCenter(double x, double y) {
    }
}
