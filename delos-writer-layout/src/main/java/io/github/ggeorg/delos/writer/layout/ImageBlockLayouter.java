package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.ImageBlock;

import java.util.Objects;

/**
 * Lays out atomic image blocks for paginated Writer flow.
 *
 * <p>Images remain block-level placeholders in this phase. This class owns the
 * sizing policy so the page-flow engine only decides where the block belongs.</p>
 */
public final class ImageBlockLayouter {
    private static final double DEFAULT_WIDTH = 240.0;
    private static final double DEFAULT_HEIGHT = 160.0;

    public LaidOutImageBlock layout(
        int sourceBlockIndex,
        ImageBlock imageBlock,
        double x,
        double y,
        double maxWidth
    ) {
        Objects.requireNonNull(imageBlock, "imageBlock");
        double availableWidth = Math.max(0.0, maxWidth);
        double width = imageBlock.width() > 0.0 ? imageBlock.width() : DEFAULT_WIDTH;
        double height = imageBlock.height() > 0.0 ? imageBlock.height() : DEFAULT_HEIGHT;

        if (availableWidth > 0.0 && width > availableWidth) {
            double scale = availableWidth / width;
            width = availableWidth;
            height = height * scale;
        }

        return new LaidOutImageBlock(
            sourceBlockIndex,
            x,
            y,
            width,
            height,
            imageBlock.source(),
            imageBlock.altText()
        );
    }
}
