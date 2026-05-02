package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Block;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TableCell;
import io.github.ggeorg.delos.writer.document.TableRow;
import io.github.ggeorg.delos.writer.layout.ApproximateTextMeasurer;
import io.github.ggeorg.delos.writer.layout.BlockRole;
import io.github.ggeorg.delos.writer.layout.GreedyParagraphLayouter;
import io.github.ggeorg.delos.writer.layout.LaidOutBlock;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LaidOutTableBlock;
import io.github.ggeorg.delos.writer.layout.LaidOutTextBlock;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;
import io.github.ggeorg.delos.writer.layout.PaginatingDocumentLayoutEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableLayoutContractTest {
    @Test
    void laysOutTableBetweenParagraphBlocks() {
        Document document = Document.fromBlocks(
                "Tables",
                new PageStyle(300.0, 420.0, 30.0, 30.0, 30.0, 30.0),
                List.of(
                        ParagraphBlock.of(Paragraph.of("Before")),
                        table(
                                "A1", "B1",
                                "A2", "B2"
                        ),
                        ParagraphBlock.of(Paragraph.of("After"))
                )
        );

        LaidOutDocument layout = new PaginatingDocumentLayoutEngine(
                new GreedyParagraphLayouter(new ApproximateTextMeasurer())
        ).layout(document, LayoutTheme.defaultTheme());

        List<LaidOutBlock> blocks = layout.pages().getFirst().blocks();
        assertTrue(blocks.get(0) instanceof LaidOutTextBlock);
        assertTrue(blocks.get(1) instanceof LaidOutTableBlock);
        assertTrue(blocks.get(2) instanceof LaidOutTextBlock);

        LaidOutTableBlock laidOutTable = (LaidOutTableBlock) blocks.get(1);
        assertEquals(2, laidOutTable.rowCount());
        assertEquals(2, laidOutTable.columnCount());
        assertEquals(240.0, laidOutTable.width(), 0.001);
        assertTrue(laidOutTable.height() >= 48.0);
        assertEquals(2, laidOutTable.rows().getFirst().cells().size());
        assertEquals(BlockRole.TABLE_CELL, laidOutTable.rows().getFirst().cells().getFirst().textBlocks().getFirst().role());
        assertEquals("A1", laidOutTable.rows().getFirst().cells().getFirst().textBlocks().getFirst().lines().getFirst().text());

        LaidOutTextBlock after = (LaidOutTextBlock) blocks.get(2);
        assertEquals(1, after.sourceParagraphIndex());
    }
    
    @Test
    void paginatesLongTablesByRowsAndRepeatsHeaderRows() {
        TableBlock table = new TableBlock(List.of(
                new TableRow(List.of(cell("H1").withBackground("#E5E7EB"), cell("H2"))),
                new TableRow(List.of(cell("R1C1"), cell("R1C2"))),
                new TableRow(List.of(cell("R2C1"), cell("R2C2"))),
                new TableRow(List.of(cell("R3C1"), cell("R3C2"))),
                new TableRow(List.of(cell("R4C1"), cell("R4C2"))),
                new TableRow(List.of(cell("R5C1"), cell("R5C2"))),
                new TableRow(List.of(cell("R6C1"), cell("R6C2"))),
                new TableRow(List.of(cell("R7C1"), cell("R7C2")))
        )).withHeaderRowCount(1).withColumnWeights(1.0, 2.0);

        Document document = Document.fromBlocks(
                "Report Table",
                new PageStyle(300.0, 160.0, 20.0, 20.0, 20.0, 20.0),
                List.of(table)
        );

        LaidOutDocument layout = new PaginatingDocumentLayoutEngine(
                new GreedyParagraphLayouter(new ApproximateTextMeasurer())
        ).layout(document, LayoutTheme.defaultTheme());

        List<LaidOutTableBlock> tableFragments = layout.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .filter(LaidOutTableBlock.class::isInstance)
                .map(LaidOutTableBlock.class::cast)
                .toList();

        assertTrue(layout.pages().size() > 1);
        assertTrue(tableFragments.size() > 1);
        assertEquals("H1", tableFragments.getFirst().rows().getFirst().cells().getFirst()
                .textBlocks().getFirst().lines().getFirst().text());
        assertEquals("H1", tableFragments.get(1).rows().getFirst().cells().getFirst()
                .textBlocks().getFirst().lines().getFirst().text());
        assertTrue(tableFragments.get(1).rows().getFirst().cells().getFirst().header());
        assertEquals("#E5E7EB", tableFragments.get(1).rows().getFirst().cells().getFirst().backgroundColor());
        double availableTableWidth = document.pageStyle().contentWidth();
        assertEquals(availableTableWidth / 3.0, tableFragments.getFirst().rows().getFirst().cells().getFirst().width(), 0.001);
        assertEquals(availableTableWidth * 2.0 / 3.0, tableFragments.getFirst().rows().getFirst().cells().get(1).width(), 0.001);
    }

    private static TableBlock table(String... values) {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("expected pairs");
        }
        return new TableBlock(List.of(
                new TableRow(List.of(cell(values[0]), cell(values[1]))),
                new TableRow(List.of(cell(values[2]), cell(values[3])))
        ));
    }

    private static TableCell cell(String text) {
        return new TableCell(List.of(Paragraph.of(text)));
    }
}
