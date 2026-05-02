package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RenderTarget;
import io.github.ggeorg.delos.render.RenderTheme;

import io.github.ggeorg.delos.writer.layout.LaidOutSeparator;

/** Paints separator blocks in the page flow. */
final class SeparatorRenderer {
    void paint(RenderTarget target, LaidOutSeparator separator, RenderTheme theme, double pageX, double pageY) {
        target.setStroke(theme.separatorColor());
        double lineY = pageY + separator.y() + separator.height() / 2.0;
        target.strokeLine(
                pageX + separator.x(),
                lineY,
                pageX + separator.x() + separator.width(),
                lineY
        );
    }
}
