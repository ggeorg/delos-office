package io.github.ggeorg.delos.javafx;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

public final class ZoomMath {
    private ZoomMath() {
    }

    public static double clampZoom(double factor) {
        return Math.max(0.50, Math.min(4.00, factor));
    }

    public static double percent(double factor) {
        return Math.round(clampZoom(factor) * 100.0);
    }

    public static double fitWidthZoom(double viewportWidth, double pageWidthWithPadding) {
        if (viewportWidth <= 0 || pageWidthWithPadding <= 0) {
            return 1.0;
        }
        return clampZoom(viewportWidth / pageWidthWithPadding);
    }

    public static Bounds scaleBounds(Bounds bounds, double zoomFactor) {
        if (bounds == null) {
            return null;
        }
        double z = clampZoom(zoomFactor);
        return new BoundingBox(
                bounds.getMinX() * z,
                bounds.getMinY() * z,
                bounds.getWidth() * z,
                bounds.getHeight() * z
        );
    }
}
