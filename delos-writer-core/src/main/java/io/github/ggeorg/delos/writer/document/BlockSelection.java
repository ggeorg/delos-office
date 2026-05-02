package io.github.ggeorg.delos.writer.document;

/**
 * Selection of a whole top-level non-text block such as an image or table.
 *
 * <p>The index is expressed in {@link Document#blocks()} space, not paragraph
 * projection space. Text selection remains modeled separately as
 * {@link SelectionRange}.</p>
 */
public record BlockSelection(int blockIndex) {
    public BlockSelection {
        if (blockIndex < 0) {
            throw new IllegalArgumentException("blockIndex must be >= 0");
        }
    }
}
