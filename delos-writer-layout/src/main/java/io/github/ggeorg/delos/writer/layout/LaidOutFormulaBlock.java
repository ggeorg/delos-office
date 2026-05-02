package io.github.ggeorg.delos.writer.layout;

/**
 * Positioned formula placeholder block.
 */
public record LaidOutFormulaBlock(
        int sourceBlockIndex,
        double x,
        double y,
        double width,
        double height,
        String sourceFormat,
        String source,
        String altText
) implements LaidOutAtomicBlock {
    public LaidOutFormulaBlock {
        width = Math.max(0.0, width);
        height = Math.max(0.0, height);
        sourceFormat = sourceFormat == null || sourceFormat.isBlank() ? "latex" : sourceFormat;
        source = source == null ? "" : source;
        altText = altText == null ? "" : altText;
    }

    @Override
    public LaidOutFormulaBlock withY(double y) {
        return new LaidOutFormulaBlock(sourceBlockIndex, x, y, width, height, sourceFormat, source, altText);
    }
}
