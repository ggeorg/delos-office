package io.github.ggeorg.delos.writer.layout;

/**
 * Positioned top-level block that behaves as one selectable object in the editor.
 *
 * <p>Images, formulas, tables, and horizontal rules participate in page flow as
 * normal blocks, but hit-testing and selection use their whole rectangle rather
 * than an inner caret model.</p>
 */
public sealed interface LaidOutAtomicBlock extends LaidOutBlock
        permits LaidOutImageBlock, LaidOutFormulaBlock, LaidOutTableBlock, LaidOutSeparator {
    int sourceBlockIndex();

    double x();

    double y();

    double width();

    double height();

    LaidOutAtomicBlock withY(double y);

    default boolean selectable() {
        return sourceBlockIndex() >= 0;
    }

    default boolean contains(double pageLocalX, double pageLocalY) {
        return pageLocalX >= x()
                && pageLocalX <= x() + width()
                && pageLocalY >= y()
                && pageLocalY <= y() + height();
    }
}
