package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TableCell;
import io.github.ggeorg.delos.writer.document.TableRow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableBlockLayouterTest {
    @Test
    void laysOutCellsUsingParagraphLayouterAndTableGeometry() {
        TableBlock table = new TableBlock(List.of(
            new TableRow(List.of(cell("A1"), cell("B1"))),
            new TableRow(List.of(cell("A2"), cell("B2")))
        ));

        TableBlockLayouter layouter = new TableBlockLayouter(
            new GreedyParagraphLayouter(new ApproximateTextMeasurer())
        );

        LaidOutTableBlock laidOut = layouter.layout(
            3,
            table,
            30.0,
            40.0,
            240.0,
            LayoutTheme.defaultTheme()
        );

        assertEquals(3, laidOut.sourceBlockIndex());
        assertEquals(30.0, laidOut.x(), 0.001);
        assertEquals(40.0, laidOut.y(), 0.001);
        assertEquals(240.0, laidOut.width(), 0.001);
        assertEquals(2, laidOut.rowCount());
        assertEquals(2, laidOut.columnCount());
        assertEquals(120.0, laidOut.rows().getFirst().cells().getFirst().width(), 0.001);
        assertEquals(BlockRole.TABLE_CELL, laidOut.rows().getFirst().cells().getFirst().textBlocks().getFirst().role());
        assertEquals("A1", laidOut.rows().getFirst().cells().getFirst().textBlocks().getFirst().lines().getFirst().text());
    }

    private static TableCell cell(String text) {
        return new TableCell(List.of(Paragraph.of(text)));
    }
}
