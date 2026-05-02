package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RenderTarget;
import io.github.ggeorg.delos.render.RenderTheme;

import io.github.ggeorg.delos.writer.layout.CaretGeometry;

/** Paints the insertion caret. */
final class CaretRenderer {
    private static final double CARET_WIDTH = 1.25;

    void paint(RenderTarget target, RenderTheme theme, double pageX, double pageY, CaretGeometry caret) {
        if (caret == null) {
            return;
        }
        target.setStroke(theme.bodyText());
        target.setLineWidth(CARET_WIDTH);
        double x = pageX + caret.x();
        double y1 = pageY + caret.y();
        double y2 = y1 + caret.height();
        target.strokeLine(x, y1, x, y2);
        target.setLineWidth(1.0);
    }
}
