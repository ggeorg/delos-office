package io.github.ggeorg.delos.writer.document;

/**
 * Selection/address of a table cell in top-level {@link Document#blocks()} space.
 *
 * <p>The block index points at a {@link TableBlock}. Row and column are zero-based.
 * v71 keeps table-cell editing deliberately coarse-grained: the cell is selected as
 * a unit, and the editor can replace the cell's paragraph text without introducing
 * a nested caret model yet.</p>
 */
public record TableCellSelection(
        int blockIndex,
        int rowIndex,
        int columnIndex
) {
    public TableCellSelection {
        if (blockIndex < 0) {
            throw new IllegalArgumentException("blockIndex must be >= 0");
        }
        if (rowIndex < 0) {
            throw new IllegalArgumentException("rowIndex must be >= 0");
        }
        if (columnIndex < 0) {
            throw new IllegalArgumentException("columnIndex must be >= 0");
        }
    }
}
