package io.github.ggeorg.delos.writer.document;

/**
 * Selection address for a whole table cell.
 *
 * <p>The cell is selected as an atomic region inside a top-level table block.
 * Rich in-cell story editing builds on {@link TableCellStoryPath} without
 * changing this selection address.</p>
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
