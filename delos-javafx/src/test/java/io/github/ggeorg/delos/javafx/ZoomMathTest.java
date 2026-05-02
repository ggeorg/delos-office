package io.github.ggeorg.delos.javafx;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ZoomMathTest {
    @Test
    void fitWidthZoomClampsIntoSupportedRange() {
        assertEquals(1.0, ZoomMath.fitWidthZoom(500, 500), 0.0001);
        assertEquals(4.0, ZoomMath.fitWidthZoom(5000, 500), 0.0001);
        assertEquals(0.5, ZoomMath.fitWidthZoom(100, 500), 0.0001);
    }

    @Test
    void scaleBoundsMultipliesDocumentSpaceIntoViewSpace() {
        Bounds scaled = ZoomMath.scaleBounds(new BoundingBox(10, 20, 30, 40), 1.5);
        assertEquals(15.0, scaled.getMinX(), 0.0001);
        assertEquals(30.0, scaled.getMinY(), 0.0001);
        assertEquals(45.0, scaled.getWidth(), 0.0001);
        assertEquals(60.0, scaled.getHeight(), 0.0001);
    }
}
