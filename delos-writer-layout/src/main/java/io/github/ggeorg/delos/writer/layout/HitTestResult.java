package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.BlockSelection;
import io.github.ggeorg.delos.writer.document.CaretPosition;
import io.github.ggeorg.delos.writer.document.TableCellSelection;
import io.github.ggeorg.delos.writer.document.TextPosition;

/**
 * Result of hit testing a page surface.
 *
 * <p>Text hits expose a logical caret position. Whole-block hits expose a
 * {@link BlockSelection}. Table-cell hits expose a {@link TableCellSelection}
 * and may also expose a story-aware caret position inside the cell's own story.</p>
 */
public record HitTestResult(
        TextPosition position,
        CaretGeometry caret,
        BlockSelection blockSelection,
        TableCellSelection tableCellSelection,
        CaretPosition storyPosition
) {
    public HitTestResult(TextPosition position, CaretGeometry caret) {
        this(position, caret, null, null, null);
    }

    public HitTestResult(TextPosition position, CaretGeometry caret, BlockSelection blockSelection) {
        this(position, caret, blockSelection, null, null);
    }

    public HitTestResult(TextPosition position, CaretGeometry caret, BlockSelection blockSelection, TableCellSelection tableCellSelection) {
        this(position, caret, blockSelection, tableCellSelection, null);
    }

    public HitTestResult {
        if (position == null && blockSelection == null && tableCellSelection == null && storyPosition == null) {
            throw new IllegalArgumentException("hit test must contain a text position, block selection, table cell selection, or story position");
        }
        if (caret == null) {
            caret = new CaretGeometry(0.0, 0.0, 0.0);
        }
    }

    public static HitTestResult block(BlockSelection selection, CaretGeometry caret) {
        return new HitTestResult(null, caret, selection, null, null);
    }

    public static HitTestResult tableCell(TableCellSelection selection, CaretGeometry caret) {
        return new HitTestResult(null, caret, null, selection, null);
    }

    public static HitTestResult tableCellCaret(TableCellSelection selection, CaretPosition storyPosition, CaretGeometry caret) {
        return new HitTestResult(null, caret, null, selection, storyPosition);
    }

    public boolean isBlockHit() {
        return blockSelection != null;
    }

    public boolean isTableCellHit() {
        return tableCellSelection != null;
    }

    public boolean isStoryHit() {
        return storyPosition != null;
    }
}
