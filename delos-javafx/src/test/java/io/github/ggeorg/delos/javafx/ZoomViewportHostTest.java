package io.github.ggeorg.delos.javafx;

import javafx.geometry.BoundingBox;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ZoomViewportHostTest extends JavaFxTestSupport {
    @Test
    void preferredWidthUsesScaledContentWidth() {
        Region content = onFxThread(() -> contentRegion(800.0, 600.0));
        ZoomViewportHost host = onFxThread(() -> new ZoomViewportHost(content));

        onFxThread(() -> {
            host.setViewportBounds(new BoundingBox(0.0, 0.0, 900.0, 600.0));
            host.setZoomFactor(1.5);
        });

        assertEquals(1200.0, onFxThread(() -> host.prefWidth(-1)), 0.0001);
    }

    @Test
    void layoutCentersTheScaledVisualBoundsWhenTheyFitInTheViewport() {
        Region content = onFxThread(() -> {
            Region region = contentRegion(600.0, 400.0);
            region.getTransforms().add(new Scale(1.5, 1.5));
            return region;
        });
        ZoomViewportHost host = onFxThread(() -> new ZoomViewportHost(content));

        onFxThread(() -> {
            host.setViewportBounds(new BoundingBox(0.0, 0.0, 1000.0, 600.0));
            host.setZoomFactor(1.5);
            host.resize(1000.0, 600.0);
            host.layout();
        });

        // Scaled width = 600 * 1.5 = 900, so the visible left edge should be
        // centered with 50 px of workspace on each side.
        assertEquals(50.0, onFxThread(() -> content.getBoundsInParent().getMinX()), 0.0001);
    }

    private static Region contentRegion(double width, double height) {
        Region region = new Region();
        region.setMinSize(width, height);
        region.setPrefSize(width, height);
        region.resize(width, height);
        return region;
    }
}
