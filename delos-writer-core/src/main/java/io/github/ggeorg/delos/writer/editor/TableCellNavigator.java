package io.github.ggeorg.delos.writer.editor;

import io.github.ggeorg.delos.writer.document.Block;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TableCellSelection;

/**
 * Navigation helper for table-cell selections.
 *
 * <p>A table cell can be selected as an atomic editing target while richer
 * in-cell caret placement evolves independently.</p>
 */
public final class TableCellNavigator {
    public TableCellSelection nextCell(Document document, TableCellSelection current) {
        TableBlock table = tableAt(document, current);
        if (table == null) {
            return null;
        }
        int nextColumn = current.columnIndex() + 1;
        int nextRow = current.rowIndex();
        if (nextColumn >= table.columnCount()) {
            nextColumn = 0;
            nextRow += 1;
        }
        if (nextRow >= table.rowCount()) {
            return null;
        }
        return new TableCellSelection(current.blockIndex(), nextRow, nextColumn);
    }

    public TableCellSelection previousCell(Document document, TableCellSelection current) {
        TableBlock table = tableAt(document, current);
        if (table == null) {
            return null;
        }
        int previousColumn = current.columnIndex() - 1;
        int previousRow = current.rowIndex();
        if (previousColumn < 0) {
            previousRow -= 1;
            previousColumn = table.columnCount() - 1;
        }
        if (previousRow < 0) {
            return null;
        }
        return new TableCellSelection(current.blockIndex(), previousRow, previousColumn);
    }

    public TableCellSelection leftCell(Document document, TableCellSelection current) {
        return move(document, current, 0, -1);
    }

    public TableCellSelection rightCell(Document document, TableCellSelection current) {
        return move(document, current, 0, 1);
    }

    public TableCellSelection aboveCell(Document document, TableCellSelection current) {
        return move(document, current, -1, 0);
    }

    public TableCellSelection belowCell(Document document, TableCellSelection current) {
        return move(document, current, 1, 0);
    }

    public boolean isValid(Document document, TableCellSelection current) {
        return tableAt(document, current) != null;
    }

    private TableCellSelection move(Document document, TableCellSelection current, int rowDelta, int columnDelta) {
        TableBlock table = tableAt(document, current);
        if (table == null) {
            return null;
        }
        int row = current.rowIndex() + rowDelta;
        int column = current.columnIndex() + columnDelta;
        if (row < 0 || row >= table.rowCount() || column < 0 || column >= table.columnCount()) {
            return null;
        }
        return new TableCellSelection(current.blockIndex(), row, column);
    }

    private TableBlock tableAt(Document document, TableCellSelection current) {
        if (document == null || current == null || current.blockIndex() >= document.blocks().size()) {
            return null;
        }
        Block block = document.blocks().get(current.blockIndex());
        if (!(block instanceof TableBlock table)) {
            return null;
        }
        if (current.rowIndex() >= table.rowCount() || current.columnIndex() >= table.columnCount()) {
            return null;
        }
        return table;
    }
}
