package io.github.ggeorg.delos.javafx;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Region;

import java.util.Objects;

/**
 * ScrollPane host for zoomed document content.
 * <p>
 * The host reports the <em>scaled</em> content size to the ScrollPane and
 * centers that scaled content whenever it is narrower than the viewport. The
 * content node itself stays in document coordinates; the view transform is
 * applied outside the layout engine.
 */
public final class ZoomViewportHost extends Region {
    private final Node content;
    private double viewportWidth;
    private double viewportHeight;
    private double zoomFactor = 1.0;

    public ZoomViewportHost(Node content) {
        this.content = Objects.requireNonNull(content, "content");
        getStyleClass().add("zoom-viewport-host");
        getChildren().add(content);
    }

    public void setViewportBounds(Bounds viewportBounds) {
        if (viewportBounds == null) {
            return;
        }
        viewportWidth = Math.max(0.0, viewportBounds.getWidth());
        viewportHeight = Math.max(0.0, viewportBounds.getHeight());
        requestLayout();
    }

    public void setZoomFactor(double zoomFactor) {
        double clamped = ZoomMath.clampZoom(zoomFactor);
        if (Double.compare(this.zoomFactor, clamped) == 0) {
            return;
        }
        this.zoomFactor = clamped;
        requestLayout();
        if (getParent() != null) {
            getParent().requestLayout();
        }
    }

    public double zoomFactor() {
        return zoomFactor;
    }

    @Override
    protected double computePrefWidth(double height) {
        return Math.max(scaledContentWidth(), viewportWidth);
    }

    @Override
    protected double computePrefHeight(double width) {
        return Math.max(scaledContentHeight(), viewportHeight);
    }

    @Override
    protected double computeMinWidth(double height) {
        return computePrefWidth(height);
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    @Override
    protected void layoutChildren() {
        Bounds bounds = content.getLayoutBounds();
        double scaledWidth = bounds.getWidth() * zoomFactor;
        double scaledLeft = Math.max((getWidth() - scaledWidth) / 2.0, 0.0);

        // The Scale transform is applied in the child coordinate system. Place
        // the child so the scaled visual bounds, not the unscaled layout bounds,
        // are centered in the host. Without this, zooming grows the page to the
        // right and the document column drifts off-center.
        content.setLayoutX(snapPositionX(scaledLeft - bounds.getMinX() * zoomFactor));
        content.setLayoutY(snapPositionY(-bounds.getMinY() * zoomFactor));
    }

    private double scaledContentWidth() {
        return content.getLayoutBounds().getWidth() * zoomFactor;
    }

    private double scaledContentHeight() {
        return content.getLayoutBounds().getHeight() * zoomFactor;
    }
}
