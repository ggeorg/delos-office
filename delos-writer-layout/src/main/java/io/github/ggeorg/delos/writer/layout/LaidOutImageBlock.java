package io.github.ggeorg.delos.writer.layout;

/**
 * Positioned image placeholder block.
 *
 * <p>v62 keeps images block-level and selectable. Real bitmap painting and
 * resizing come later; this layout block preserves the flow rectangle and the
 * source document block identity for hit testing.</p>
 */
public record LaidOutImageBlock(
        int sourceBlockIndex,
        double x,
        double y,
        double width,
        double height,
        String source,
        String altText
) implements LaidOutAtomicBlock {
    public LaidOutImageBlock {
        width = Math.max(0.0, width);
        height = Math.max(0.0, height);
        source = source == null ? "" : source;
        altText = altText == null ? "" : altText;
    }

    @Override
    public LaidOutImageBlock withY(double y) {
        return new LaidOutImageBlock(sourceBlockIndex, x, y, width, height, source, altText);
    }
}
