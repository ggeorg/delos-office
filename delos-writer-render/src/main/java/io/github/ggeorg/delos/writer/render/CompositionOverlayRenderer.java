package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.RenderTarget;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.render.RenderTheme;

/** Paints temporary IME composition text near the insertion caret. */
final class CompositionOverlayRenderer {
    private static final double UNDERLINE_OFFSET = 1.5;

    void paint(RenderTarget target,
               RenderTheme theme,
               RenderTextMeasurer measurer,
               double pageX,
               double pageY,
               CompositionTextState composition) {
        if (composition == null || composition.isEmpty()) {
            return;
        }

        RenderFont font = theme.bodyFont();
        double x = pageX + composition.caret().x();
        double baselineY = pageY + composition.caret().y() + measurer.baseline(font);
        double underlineY = pageY + composition.caret().y() + composition.caret().height() + UNDERLINE_OFFSET;
        double width = measurer.textWidth(composition.text(), font);

        target.setFont(font);
        target.setFill(theme.bodyText());
        target.fillText(composition.text(), x, baselineY);

        if (width > 0.0) {
            target.setStroke(theme.bodyText());
            target.strokeLine(x, underlineY, x + width, underlineY);
        }
    }
}
