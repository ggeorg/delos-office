package io.github.ggeorg.delos.writer.document;

/**
 * Story path for a table cell.
 *
 * <p>The table is addressed by top-level document block index. Row and column
 * are zero-based indexes inside that table. This matches {@link TableCellSelection}
 * so the current coarse table-cell editor can be migrated without inventing a
 * second table addressing scheme.</p>
 */
public record TableCellStoryPath(
        int tableBlockIndex,
        int rowIndex,
        int columnIndex
) implements StoryPath {
    public TableCellStoryPath {
        if (tableBlockIndex < 0) {
            throw new IllegalArgumentException("tableBlockIndex must be >= 0");
        }
        if (rowIndex < 0) {
            throw new IllegalArgumentException("rowIndex must be >= 0");
        }
        if (columnIndex < 0) {
            throw new IllegalArgumentException("columnIndex must be >= 0");
        }
    }

    public static TableCellStoryPath from(TableCellSelection selection) {
        if (selection == null) {
            throw new NullPointerException("selection");
        }
        return new TableCellStoryPath(selection.blockIndex(), selection.rowIndex(), selection.columnIndex());
    }

    public TableCellSelection toTableCellSelection() {
        return new TableCellSelection(tableBlockIndex, rowIndex, columnIndex);
    }

    @Override
    public boolean isBody() {
        return false;
    }

    @Override
    public String toString() {
        return "tableCell[tableBlockIndex=" + tableBlockIndex
                + ", rowIndex=" + rowIndex
                + ", columnIndex=" + columnIndex + ']';
    }
}
