package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RenderImage;
import io.github.ggeorg.delos.render.RenderImageResolver;
import io.github.ggeorg.delos.render.RenderTarget;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.render.RenderTheme;
import io.github.ggeorg.delos.writer.layout.LaidOutImageBlock;

import java.util.Optional;

/** Paints block-level images, falling back to a conservative placeholder. */
final class ImageBlockRenderer {
    private static final double LABEL_PADDING = 8.0;

    void paint(RenderTarget target,
               LaidOutImageBlock imageBlock,
               RenderTheme theme,
               RenderTextMeasurer measurer,
               RenderImageResolver imageResolver,
               double offsetX,
               double offsetY) {
        double x = offsetX + imageBlock.x();
        double y = offsetY + imageBlock.y();
        target.save();
        try {
            Optional<RenderImage> image = imageResolver.resolve(imageBlock.source());
            if (image.isPresent() && target.drawImage(image.get(), x, y, imageBlock.width(), imageBlock.height())) {
                return;
            }
            paintPlaceholder(target, imageBlock, theme, x, y);
        } finally {
            target.restore();
        }
    }

    private void paintPlaceholder(RenderTarget target,
                                  LaidOutImageBlock imageBlock,
                                  RenderTheme theme,
                                  double x,
                                  double y) {
        target.setStroke(theme.separatorColor());
        target.setLineWidth(1.0);
        target.strokeRoundRect(x, y, imageBlock.width(), imageBlock.height(), 6.0, 6.0);

        String label = imageBlock.altText().isBlank() ? imageBlock.source() : imageBlock.altText();
        if (!label.isBlank() && imageBlock.height() >= 24.0) {
            target.setFill(theme.bodyText());
            target.setFont(theme.bodyFont());
            target.fillText(label, x + LABEL_PADDING, y + Math.min(imageBlock.height() - LABEL_PADDING, 18.0));
        }
    }
}
