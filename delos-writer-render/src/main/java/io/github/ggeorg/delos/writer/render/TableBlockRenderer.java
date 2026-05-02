package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RenderColor;
import io.github.ggeorg.delos.render.RenderTarget;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.render.RenderTheme;
import io.github.ggeorg.delos.writer.layout.LaidOutTableBlock;
import io.github.ggeorg.delos.writer.layout.LaidOutTableCell;
import io.github.ggeorg.delos.writer.layout.LaidOutTableRow;
import io.github.ggeorg.delos.writer.layout.LaidOutTextBlock;

/** Paints table blocks: cell backgrounds, borders, and paragraph text. */
final class TableBlockRenderer {
    private static final double BORDER_WIDTH = 0.75;
    private static final RenderColor DEFAULT_HEADER_FILL = RenderColor.rgb(248, 250, 252);

    private final TextBlockRenderer textBlockRenderer;

    TableBlockRenderer(TextBlockRenderer textBlockRenderer) {
        this.textBlockRenderer = textBlockRenderer;
    }

    void paint(RenderTarget target,
               LaidOutTableBlock table,
               RenderTheme theme,
               RenderTextMeasurer measurer,
               double pageX,
               double pageY) {
        for (LaidOutTableRow row : table.rows()) {
            for (LaidOutTableCell cell : row.cells()) {
                double cellX = pageX + table.x() + cell.x();
                double cellY = pageY + table.y() + cell.y();
                paintCellBackground(target, cell, cellX, cellY);
                if (table.bordersEnabled()) {
                    paintCellBorder(target, theme, cellX, cellY, cell.width(), cell.height());
                }

                for (LaidOutTextBlock textBlock : cell.textBlocks()) {
                    textBlockRenderer.paint(
                            target,
                            textBlock,
                            theme,
                            measurer,
                            cellX,
                            cellY
                    );
                }
            }
        }
    }

    private void paintCellBackground(RenderTarget target, LaidOutTableCell cell, double x, double y) {
        RenderColor explicit = TableColorParser.parseOrNull(cell.backgroundColor());
        RenderColor fill = explicit != null ? explicit : (cell.header() ? DEFAULT_HEADER_FILL : null);
        if (fill == null) {
            return;
        }
        target.setFill(fill);
        target.fillRect(x, y, cell.width(), cell.height());
    }

    private void paintCellBorder(RenderTarget target, RenderTheme theme, double x, double y, double width, double height) {
        target.setStroke(theme.separatorColor());
        target.setLineWidth(BORDER_WIDTH);
        target.strokeRect(x, y, width, height);
    }
}
