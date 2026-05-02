package io.github.ggeorg.delos.writer.layout;

/**
 * Positioned horizontal separator rule.
 */
public record LaidOutSeparator(
        int sourceBlockIndex,
        double x,
        double y,
        double width,
        double height
) implements LaidOutAtomicBlock {
    public LaidOutSeparator(double x, double y, double width) {
        this(-1, x, y, width, 1.0);
    }

    public LaidOutSeparator {
        width = Math.max(0.0, width);
        height = Math.max(0.0, height);
    }

    @Override
    public LaidOutSeparator withY(double y) {
        return new LaidOutSeparator(sourceBlockIndex, x, y, width, height);
    }
}
