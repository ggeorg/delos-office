package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.TableBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Paginates laid-out table blocks into page-sized row fragments.
 *
 * <p>This is intentionally behavior-preserving: rows are still indivisible and
 * oversized rows may overflow a fresh page. Table row-splitting belongs to a
 * later correctness unit; this class only moves the existing pagination rules
 * out of the main document paginator.</p>
 */
final class TablePaginator {
    private static final double TABLE_BLOCK_SPACING_AFTER = 6.0;

    private final TableBlockLayouter tableBlockLayouter;

    TablePaginator(TableBlockLayouter tableBlockLayouter) {
        this.tableBlockLayouter = Objects.requireNonNull(tableBlockLayouter, "tableBlockLayouter");
    }

    void appendTable(
        PageStyle pageStyle,
        LayoutTheme theme,
        List<LaidOutPage> pages,
        PageFlowState state,
        int sourceBlockIndex,
        TableBlock tableBlock
    ) {
        Objects.requireNonNull(pageStyle, "pageStyle");
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(pages, "pages");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(tableBlock, "tableBlock");

        double contentBottom = pageStyle.height() - pageStyle.marginBottom();
        double pageBodyHeight = Math.max(0.0, contentBottom - pageStyle.marginTop());
        LaidOutTableBlock fullTable = tableBlockLayouter.layout(
            sourceBlockIndex,
            tableBlock,
            pageStyle.marginLeft(),
            0.0,
            pageStyle.contentWidth(),
            theme
        );

        int headerRowCount = Math.min(tableBlock.headerRowCount(), fullTable.rows().size());
        int nextRowIndex = 0;
        boolean firstFragment = true;

        while (nextRowIndex < fullTable.rows().size()) {
            double minimumFragmentHeight = minimumTableFragmentHeight(fullTable, headerRowCount, nextRowIndex, firstFragment);
            if (!state.currentBlocks().isEmpty() && state.cursorY() + minimumFragmentHeight > contentBottom) {
                pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
                state.advanceToNextPage(pageStyle);
                continue;
            }

            double availableHeight = Math.max(0.0, contentBottom - state.cursorY());
            TableFragment fragment = tableFragment(fullTable, headerRowCount, nextRowIndex, firstFragment, availableHeight);
            if (fragment.rows().isEmpty()) {
                if (!state.currentBlocks().isEmpty()) {
                    pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
                    state.advanceToNextPage(pageStyle);
                    continue;
                }
                fragment = forcedSingleRowTableFragment(fullTable, headerRowCount, nextRowIndex, firstFragment);
            }

            state.currentBlocks().add(new LaidOutTableBlock(
                sourceBlockIndex,
                fullTable.x(),
                state.cursorY(),
                fullTable.width(),
                fragment.height(),
                fragment.rows(),
                fullTable.bordersEnabled(),
                fragment.verticallyOverflowing(pageBodyHeight)
            ));
            state.cursorY(state.cursorY() + fragment.height());
            nextRowIndex = fragment.nextRowIndex();
            firstFragment = false;

            if (nextRowIndex < fullTable.rows().size()) {
                pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
                state.advanceToNextPage(pageStyle);
            }
        }

        if (state.cursorY() + TABLE_BLOCK_SPACING_AFTER <= contentBottom) {
            state.cursorY(state.cursorY() + TABLE_BLOCK_SPACING_AFTER);
        }
    }

    private TableFragment tableFragment(
        LaidOutTableBlock fullTable,
        int headerRowCount,
        int nextRowIndex,
        boolean firstFragment,
        double availableHeight
    ) {
        List<LaidOutTableRow> rows = new ArrayList<>();
        double rowY = 0.0;

        if (!firstFragment && headerRowCount > 0) {
            for (int rowIndex = 0; rowIndex < headerRowCount; rowIndex++) {
                LaidOutTableRow headerRow = fullTable.rows().get(rowIndex);
                rows.add(tableRowAtY(headerRow, rowY));
                rowY += headerRow.height();
            }
        }

        int sourceRowIndex = firstFragment ? nextRowIndex : Math.max(nextRowIndex, headerRowCount);
        int placedSourceRows = 0;
        while (sourceRowIndex < fullTable.rows().size()) {
            LaidOutTableRow row = fullTable.rows().get(sourceRowIndex);
            if (placedSourceRows > 0 && rowY + row.height() > availableHeight) {
                break;
            }
            if (placedSourceRows == 0 && rowY + row.height() > availableHeight && !rows.isEmpty()) {
                break;
            }
            if (placedSourceRows == 0 && rows.isEmpty() && rowY + row.height() > availableHeight) {
                rows.add(tableRowAtY(row, rowY));
                rowY += row.height();
                sourceRowIndex += 1;
                placedSourceRows += 1;
                break;
            }

            rows.add(tableRowAtY(row, rowY));
            rowY += row.height();
            sourceRowIndex += 1;
            placedSourceRows += 1;
        }

        if (placedSourceRows == 0 && !firstFragment && nextRowIndex >= fullTable.rows().size()) {
            return new TableFragment(rows, nextRowIndex, rowY);
        }
        if (placedSourceRows == 0) {
            return new TableFragment(List.of(), nextRowIndex, 0.0);
        }
        return new TableFragment(rows, sourceRowIndex, rowY);
    }

    private TableFragment forcedSingleRowTableFragment(
        LaidOutTableBlock fullTable,
        int headerRowCount,
        int nextRowIndex,
        boolean firstFragment
    ) {
        List<LaidOutTableRow> rows = new ArrayList<>();
        double rowY = 0.0;
        if (!firstFragment && headerRowCount > 0) {
            for (int rowIndex = 0; rowIndex < headerRowCount; rowIndex++) {
                LaidOutTableRow headerRow = fullTable.rows().get(rowIndex);
                rows.add(tableRowAtY(headerRow, rowY));
                rowY += headerRow.height();
            }
        }

        int sourceRowIndex = firstFragment ? nextRowIndex : Math.max(nextRowIndex, headerRowCount);
        if (sourceRowIndex < fullTable.rows().size()) {
            LaidOutTableRow row = fullTable.rows().get(sourceRowIndex);
            rows.add(tableRowAtY(row, rowY));
            rowY += row.height();
            sourceRowIndex += 1;
        }
        return new TableFragment(rows, sourceRowIndex, rowY);
    }

    private double minimumTableFragmentHeight(
        LaidOutTableBlock fullTable,
        int headerRowCount,
        int nextRowIndex,
        boolean firstFragment
    ) {
        int sourceRowIndex = firstFragment ? nextRowIndex : Math.max(nextRowIndex, headerRowCount);
        double height = 0.0;
        if (firstFragment && sourceRowIndex < headerRowCount) {
            for (int rowIndex = sourceRowIndex; rowIndex < headerRowCount; rowIndex++) {
                height += fullTable.rows().get(rowIndex).height();
            }
            if (headerRowCount < fullTable.rows().size()) {
                height += fullTable.rows().get(headerRowCount).height();
            }
            return height;
        }
        if (!firstFragment) {
            for (int rowIndex = 0; rowIndex < headerRowCount; rowIndex++) {
                height += fullTable.rows().get(rowIndex).height();
            }
        }
        if (sourceRowIndex < fullTable.rows().size()) {
            height += fullTable.rows().get(sourceRowIndex).height();
        }
        return height;
    }

    private LaidOutTableRow tableRowAtY(LaidOutTableRow row, double y) {
        double deltaY = y - row.y();
        List<LaidOutTableCell> cells = new ArrayList<>(row.cells().size());
        for (LaidOutTableCell cell : row.cells()) {
            cells.add(new LaidOutTableCell(
                cell.x(),
                cell.y() + deltaY,
                cell.width(),
                cell.height(),
                cell.textBlocks(),
                cell.header(),
                cell.backgroundColor()
            ));
        }
        return new LaidOutTableRow(y, row.height(), cells);
    }

    private record TableFragment(List<LaidOutTableRow> rows, int nextRowIndex, double height) {
        TableFragment {
            rows = List.copyOf(rows);
        }

        boolean verticallyOverflowing(double pageBodyHeight) {
            if (height > pageBodyHeight + 0.001) {
                return true;
            }
            for (LaidOutTableRow row : rows) {
                if (row.height() > pageBodyHeight + 0.001) {
                    return true;
                }
            }
            return false;
        }
    }
}
