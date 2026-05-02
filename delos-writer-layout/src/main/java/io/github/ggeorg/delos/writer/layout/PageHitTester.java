package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.BlockSelection;
import io.github.ggeorg.delos.writer.document.CaretPosition;
import io.github.ggeorg.delos.writer.document.TableCellSelection;
import io.github.ggeorg.delos.writer.document.TextPosition;

/**
 * Maps a page-local mouse position to a logical text position or block selection.
 */
public final class PageHitTester {
    public HitTestResult hitTest(LaidOutPage page, double pageLocalX, double pageLocalY) {
        HitTestResult blockHit = hitBlock(page, pageLocalX, pageLocalY);
        if (blockHit != null) {
            return blockHit;
        }

        TextHit best = null;

        for (int blockIndex = 0; blockIndex < page.blocks().size(); blockIndex++) {
            if (!(page.blocks().get(blockIndex) instanceof LaidOutTextBlock textBlock)) {
                continue;
            }
            if (textBlock.role() != BlockRole.BODY) {
                continue;
            }

            double blockLocalY = pageLocalY - textBlock.y();
            LaidOutLine line = findNearestLine(textBlock, blockLocalY);
            if (line == null) {
                continue;
            }

            double verticalDistance = verticalDistance(line, blockLocalY);
            if (best == null || verticalDistance < best.verticalDistance()) {
                best = new TextHit(textBlock, line, verticalDistance);
            }
        }

        if (best == null) {
            return null;
        }

        double lineLocalX = pageLocalX - best.block().x() - best.line().x();
        int columnIndex = best.line().nearestColumn(lineLocalX);
        double caretX = best.block().x() + best.line().caretXForColumn(columnIndex);
        double caretY = best.block().y() + best.line().y();

        return new HitTestResult(
                new TextPosition(best.block().sourceParagraphIndex(), best.line().startOffset() + columnIndex),
                new CaretGeometry(caretX, caretY, best.line().height())
        );
    }

    private HitTestResult hitBlock(LaidOutPage page, double pageLocalX, double pageLocalY) {
        for (LaidOutBlock rawBlock : page.blocks()) {
            if (!(rawBlock instanceof LaidOutAtomicBlock atomicBlock)
                    || !atomicBlock.selectable()
                    || !atomicBlock.contains(pageLocalX, pageLocalY)) {
                continue;
            }

            if (atomicBlock instanceof LaidOutTableBlock tableBlock) {
                HitTestResult tableCellHit = hitTableCell(tableBlock, pageLocalX, pageLocalY);
                if (tableCellHit != null) {
                    return tableCellHit;
                }
            }

            return HitTestResult.block(
                    new BlockSelection(atomicBlock.sourceBlockIndex()),
                    new CaretGeometry(atomicBlock.x(), atomicBlock.y(), atomicBlock.height())
            );
        }
        return null;
    }

    private HitTestResult hitTableCell(LaidOutTableBlock tableBlock, double pageLocalX, double pageLocalY) {
        double tableLocalX = pageLocalX - tableBlock.x();
        double tableLocalY = pageLocalY - tableBlock.y();
        for (int rowIndex = 0; rowIndex < tableBlock.rows().size(); rowIndex++) {
            LaidOutTableRow row = tableBlock.rows().get(rowIndex);
            if (tableLocalY < row.y() || tableLocalY > row.y() + row.height()) {
                continue;
            }
            for (int columnIndex = 0; columnIndex < row.cells().size(); columnIndex++) {
                LaidOutTableCell cell = row.cells().get(columnIndex);
                if (tableLocalX >= cell.x() && tableLocalX <= cell.x() + cell.width()) {
                    TableCellSelection selection = new TableCellSelection(tableBlock.sourceBlockIndex(), rowIndex, columnIndex);
                    HitTestResult cellTextHit = hitTableCellText(tableBlock, cell, selection, tableLocalX - cell.x(), tableLocalY - cell.y());
                    if (cellTextHit != null) {
                        return cellTextHit;
                    }
                    return HitTestResult.tableCell(
                            selection,
                            new CaretGeometry(
                                    tableBlock.x() + cell.x(),
                                    tableBlock.y() + cell.y(),
                                    cell.height()
                            )
                    );
                }
            }
        }
        return null;
    }

    private HitTestResult hitTableCellText(
            LaidOutTableBlock tableBlock,
            LaidOutTableCell cell,
            TableCellSelection selection,
            double cellLocalX,
            double cellLocalY
    ) {
        TextHit best = null;
        for (LaidOutTextBlock textBlock : cell.textBlocks()) {
            double blockLocalY = cellLocalY - textBlock.y();
            LaidOutLine line = findNearestLine(textBlock, blockLocalY);
            if (line == null) {
                continue;
            }
            double verticalDistance = verticalDistance(line, blockLocalY);
            if (best == null || verticalDistance < best.verticalDistance()) {
                best = new TextHit(textBlock, line, verticalDistance);
            }
        }
        if (best == null) {
            return null;
        }

        double lineLocalX = cellLocalX - best.block().x() - best.line().x();
        int columnIndex = best.line().nearestColumn(lineLocalX);
        int offset = best.line().startOffset() + columnIndex;
        CaretPosition storyPosition = CaretPosition.tableCell(
                selection.blockIndex(),
                selection.rowIndex(),
                selection.columnIndex(),
                Math.max(0, best.block().sourceParagraphIndex()),
                offset
        );
        double caretX = tableBlock.x() + cell.x() + best.block().x() + best.line().caretXForColumn(columnIndex);
        double caretY = tableBlock.y() + cell.y() + best.block().y() + best.line().y();
        return HitTestResult.tableCellCaret(selection, storyPosition, new CaretGeometry(caretX, caretY, best.line().height()));
    }

    private LaidOutLine findNearestLine(LaidOutTextBlock block, double blockLocalY) {
        if (block.lines().isEmpty()) {
            return null;
        }

        LaidOutLine best = null;
        double bestDistance = Double.MAX_VALUE;

        for (LaidOutLine line : block.lines()) {
            double distance = verticalDistance(line, blockLocalY);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = line;
            }
        }

        return best;
    }

    private double verticalDistance(LaidOutLine line, double blockLocalY) {
        if (line.containsY(blockLocalY)) {
            return 0;
        }
        if (blockLocalY < line.y()) {
            return line.y() - blockLocalY;
        }
        return blockLocalY - (line.y() + line.height());
    }

    private record TextHit(
            LaidOutTextBlock block,
            LaidOutLine line,
            double verticalDistance
    ) {
    }
}
