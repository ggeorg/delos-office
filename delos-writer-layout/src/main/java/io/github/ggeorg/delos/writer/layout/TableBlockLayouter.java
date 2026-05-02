package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TableCell;
import io.github.ggeorg.delos.writer.document.TableColumnSpec;
import io.github.ggeorg.delos.writer.document.TableRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lays out an atomic table block inside the page content area.
 * <p>
 * This class keeps tables as one rich block for now, but it now carries the
 * reporting-facing metadata needed by the next pagination step: weighted column
 * widths, header-row identity, and cell backgrounds.
 */
final class TableBlockLayouter {
    private static final double MIN_ROW_HEIGHT = 24.0;

    private final ParagraphLayouter paragraphLayouter;

    TableBlockLayouter(ParagraphLayouter paragraphLayouter) {
        this.paragraphLayouter = Objects.requireNonNull(paragraphLayouter, "paragraphLayouter");
    }

    LaidOutTableBlock layout(
        int sourceBlockIndex,
        TableBlock tableBlock,
        double x,
        double y,
        double width,
        LayoutTheme theme
    ) {
        Objects.requireNonNull(tableBlock, "tableBlock");
        Objects.requireNonNull(theme, "theme");

        int columns = Math.max(1, tableBlock.columnCount());
        double tableWidth = Math.max(1.0, width * tableBlock.style().widthFraction());
        double cellPadding = tableBlock.style().cellPadding();
        double[] columnWidths = resolveColumnWidths(tableBlock.columns(), tableWidth, columns);
        double[] columnOffsets = resolveColumnOffsets(columnWidths);
        List<LaidOutTableRow> rows = new ArrayList<>(tableBlock.rows().size());
        double rowY = 0.0;

        for (int rowIndex = 0; rowIndex < tableBlock.rows().size(); rowIndex++) {
            TableRow row = tableBlock.rows().get(rowIndex);
            boolean headerRow = tableBlock.isHeaderRow(rowIndex);
            List<CellContentLayout> cellContents = new ArrayList<>(columns);
            double rowHeight = MIN_ROW_HEIGHT;
            for (int column = 0; column < columns; column++) {
                TableCell cell = column < row.cells().size() ? row.cells().get(column) : TableCell.blank();
                CellContentLayout content = layoutCellContent(cell, columnWidths[column], cellPadding, theme);
                cellContents.add(content);
                rowHeight = Math.max(rowHeight, content.height() + cellPadding * 2.0);
            }

            List<LaidOutTableCell> cells = new ArrayList<>(columns);
            for (int column = 0; column < columns; column++) {
                TableCell cell = column < row.cells().size() ? row.cells().get(column) : TableCell.blank();
                cells.add(new LaidOutTableCell(
                    columnOffsets[column],
                    rowY,
                    columnWidths[column],
                    rowHeight,
                    cellContents.get(column).textBlocks(),
                    headerRow,
                    cell.style().backgroundColor()
                ));
            }
            rows.add(new LaidOutTableRow(rowY, rowHeight, cells));
            rowY += rowHeight;
        }

        return new LaidOutTableBlock(sourceBlockIndex, x, y, tableWidth, rowY, rows, tableBlock.style().bordersEnabled());
    }

    private CellContentLayout layoutCellContent(TableCell cell, double cellWidth, double cellPadding, LayoutTheme theme) {
        double contentWidth = Math.max(1.0, cellWidth - cellPadding * 2.0);
        List<LaidOutTextBlock> textBlocks = new ArrayList<>();
        double cursorY = cellPadding;

        int paragraphIndex = 0;
        for (Paragraph paragraph : cell.paragraphs()) {
            List<LaidOutLine> lines = paragraphLayouter.layoutLines(
                paragraph,
                theme.bodyFont(),
                contentWidth,
                theme.bodyLineGap()
            );
            double height = blockHeight(lines);
            textBlocks.add(new LaidOutTextBlock(
                BlockRole.TABLE_CELL,
                cellPadding,
                cursorY,
                contentWidth,
                height,
                paragraphIndex,
                0,
                true,
                true,
                lines
            ));
            cursorY += height + Math.max(0.0, paragraph.style().spacingAfter());
            paragraphIndex += 1;
        }

        double contentHeight = Math.max(0.0, cursorY - cellPadding);
        return new CellContentLayout(textBlocks, contentHeight);
    }

    private static double[] resolveColumnWidths(List<TableColumnSpec> specs, double tableWidth, int columns) {
        double totalWeight = 0.0;
        for (int index = 0; index < columns; index++) {
            totalWeight += specs.get(index).widthWeight();
        }

        double[] widths = new double[columns];
        double assigned = 0.0;
        for (int index = 0; index < columns; index++) {
            if (index == columns - 1) {
                widths[index] = Math.max(0.0, tableWidth - assigned);
            } else {
                widths[index] = tableWidth * (specs.get(index).widthWeight() / totalWeight);
                assigned += widths[index];
            }
        }
        return widths;
    }

    private static double[] resolveColumnOffsets(double[] columnWidths) {
        double[] offsets = new double[columnWidths.length];
        double cursor = 0.0;
        for (int index = 0; index < columnWidths.length; index++) {
            offsets[index] = cursor;
            cursor += columnWidths[index];
        }
        return offsets;
    }

    private static double blockHeight(List<LaidOutLine> lines) {
        if (lines.isEmpty()) {
            return 0;
        }

        LaidOutLine last = lines.get(lines.size() - 1);
        return last.y() + last.height();
    }

    private record CellContentLayout(List<LaidOutTextBlock> textBlocks, double height) {
        CellContentLayout {
            textBlocks = List.copyOf(textBlocks);
        }
    }
}
