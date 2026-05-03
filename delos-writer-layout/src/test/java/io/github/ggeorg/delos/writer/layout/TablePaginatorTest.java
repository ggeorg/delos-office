package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TableCell;
import io.github.ggeorg.delos.writer.document.TableRow;
import io.github.ggeorg.delos.writer.document.TableStyle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TablePaginatorTest {
    @Test
    void appendsTableOnCurrentPageWhenRowsFit() {
        PageStyle pageStyle = new PageStyle(300, 300, 20, 20, 20, 20);
        List<LaidOutPage> pages = new ArrayList<>();
        TestPageFlowState state = TestPageFlowState.firstPage(pageStyle);
        TablePaginator paginator = new TablePaginator(new TableBlockLayouter(
            new GreedyParagraphLayouter(new ApproximateTextMeasurer())
        ));

        paginator.appendTable(pageStyle, LayoutTheme.defaultTheme(), pages, state, 4, table("A1", "B1", "A2", "B2"));

        assertEquals(0, pages.size());
        assertEquals(1, state.currentBlocks().size());
        LaidOutTableBlock block = (LaidOutTableBlock) state.currentBlocks().getFirst();
        assertEquals(4, block.sourceBlockIndex());
        assertEquals(2, block.rowCount());
        assertEquals(2, block.columnCount());
        assertEquals(20.0, block.x(), 0.001);
        assertEquals(20.0, block.y(), 0.001);
        assertEquals(260.0, block.width(), 0.001);
        assertFalse(block.hasVerticalOverflow());
        assertTrue(state.cursorY() > block.y() + block.height());
    }

    @Test
    void startsFreshPageWhenMinimumTableFragmentDoesNotFitCurrentPage() {
        PageStyle pageStyle = new PageStyle(300, 160, 20, 20, 20, 20);
        List<LaidOutPage> pages = new ArrayList<>();
        TestPageFlowState state = TestPageFlowState.firstPage(pageStyle);
        state.currentBlocks().add(new LaidOutSeparator(-1, 20, 20, 260, 110));
        state.cursorY(130.0);
        TablePaginator paginator = new TablePaginator(new TableBlockLayouter(
            new GreedyParagraphLayouter(new ApproximateTextMeasurer())
        ));

        paginator.appendTable(pageStyle, LayoutTheme.defaultTheme(), pages, state, 8, table("A1", "B1", "A2", "B2"));

        assertEquals(1, pages.size());
        assertEquals(1, state.pageIndex());
        assertEquals(1, state.currentBlocks().size());
        LaidOutTableBlock table = (LaidOutTableBlock) state.currentBlocks().getFirst();
        assertEquals(8, table.sourceBlockIndex());
        assertEquals(20.0, table.y(), 0.001);
    }

    @Test
    void fragmentsLongTableByRowsAndRepeatsHeaderRows() {
        PageStyle pageStyle = new PageStyle(300, 160, 20, 20, 20, 20);
        List<LaidOutPage> pages = new ArrayList<>();
        TestPageFlowState state = TestPageFlowState.firstPage(pageStyle);
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
        TablePaginator paginator = new TablePaginator(new TableBlockLayouter(
            new GreedyParagraphLayouter(new ApproximateTextMeasurer())
        ));

        paginator.appendTable(pageStyle, LayoutTheme.defaultTheme(), pages, state, 9, table);

        assertTrue(pages.size() > 0);
        LaidOutTableBlock firstFragment = (LaidOutTableBlock) pages.getFirst().blocks().getFirst();
        LaidOutTableBlock secondFragment = (LaidOutTableBlock) pages.get(1).blocks().getFirst();
        assertEquals("H1", firstFragment.rows().getFirst().cells().getFirst().textBlocks().getFirst().lines().getFirst().text());
        assertEquals("H1", secondFragment.rows().getFirst().cells().getFirst().textBlocks().getFirst().lines().getFirst().text());
        assertTrue(secondFragment.rows().getFirst().cells().getFirst().header());
        assertEquals("#E5E7EB", secondFragment.rows().getFirst().cells().getFirst().backgroundColor());
        assertEquals(260.0 / 3.0, firstFragment.rows().getFirst().cells().getFirst().width(), 0.001);
        assertEquals(260.0 * 2.0 / 3.0, firstFragment.rows().getFirst().cells().get(1).width(), 0.001);
    }

    @Test
    void preservesTableBorderFlagOnFragments() {
        PageStyle pageStyle = new PageStyle(300, 300, 20, 20, 20, 20);
        List<LaidOutPage> pages = new ArrayList<>();
        TestPageFlowState state = TestPageFlowState.firstPage(pageStyle);
        TableBlock table = table("A1", "B1", "A2", "B2")
            .withStyle(TableStyle.defaults().withBordersEnabled(false));
        TablePaginator paginator = new TablePaginator(new TableBlockLayouter(
            new GreedyParagraphLayouter(new ApproximateTextMeasurer())
        ));

        paginator.appendTable(pageStyle, LayoutTheme.defaultTheme(), pages, state, 11, table);

        LaidOutTableBlock block = (LaidOutTableBlock) state.currentBlocks().getFirst();
        assertEquals(false, block.bordersEnabled());
    }


    @Test
    void oversizedSingleRowIsPlacedAndMarkedAsControlledOverflow() {
        PageStyle pageStyle = new PageStyle(300, 120, 20, 20, 20, 20);
        List<LaidOutPage> pages = new ArrayList<>();
        TestPageFlowState state = TestPageFlowState.firstPage(pageStyle);
        TablePaginator paginator = new TablePaginator(new TableBlockLayouter(
            new GreedyParagraphLayouter(new ApproximateTextMeasurer())
        ));

        paginator.appendTable(pageStyle, LayoutTheme.defaultTheme(), pages, state, 12, table(veryLongText(160), "B1"));

        assertEquals(0, pages.size());
        assertEquals(1, state.currentBlocks().size());
        LaidOutTableBlock block = (LaidOutTableBlock) state.currentBlocks().getFirst();
        assertTrue(block.height() > pageStyle.height() - pageStyle.marginTop() - pageStyle.marginBottom());
        assertTrue(block.hasVerticalOverflow());
        assertTrue(state.cursorY() > pageStyle.height() - pageStyle.marginBottom());
    }

    @Test
    void oversizedDataRowAfterHeaderMakesProgressAndMarksOverflowingFragment() {
        PageStyle pageStyle = new PageStyle(300, 120, 20, 20, 20, 20);
        List<LaidOutPage> pages = new ArrayList<>();
        TestPageFlowState state = TestPageFlowState.firstPage(pageStyle);
        TableBlock table = new TableBlock(List.of(
            new TableRow(List.of(cell("H1"), cell("H2"))),
            new TableRow(List.of(cell(veryLongText(160)), cell("B1")))
        )).withHeaderRowCount(1);
        TablePaginator paginator = new TablePaginator(new TableBlockLayouter(
            new GreedyParagraphLayouter(new ApproximateTextMeasurer())
        ));

        paginator.appendTable(pageStyle, LayoutTheme.defaultTheme(), pages, state, 13, table);

        assertEquals(1, pages.size());
        LaidOutTableBlock headerOnlyFragment = (LaidOutTableBlock) pages.getFirst().blocks().getFirst();
        LaidOutTableBlock overflowingFragment = (LaidOutTableBlock) state.currentBlocks().getFirst();
        assertFalse(headerOnlyFragment.hasVerticalOverflow());
        assertTrue(overflowingFragment.hasVerticalOverflow());
        assertEquals(2, overflowingFragment.rowCount());
        assertEquals("H1", overflowingFragment.rows().getFirst().cells().getFirst().textBlocks().getFirst().lines().getFirst().text());
    }

    private static TableBlock table(String... values) {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("expected pairs");
        }
        List<TableRow> rows = new ArrayList<>();
        for (int i = 0; i < values.length; i += 2) {
            rows.add(new TableRow(List.of(cell(values[i]), cell(values[i + 1]))));
        }
        return new TableBlock(rows);
    }

    private static TableCell cell(String text) {
        return new TableCell(List.of(Paragraph.of(text)));
    }

    private static String veryLongText(int words) {
        return "word ".repeat(words).trim();
    }

    private static final class TestPageFlowState implements PageFlowState {
        private int pageIndex;
        private double cursorY;
        private List<LaidOutBlock> currentBlocks = new ArrayList<>();

        private TestPageFlowState(int pageIndex, double cursorY) {
            this.pageIndex = pageIndex;
            this.cursorY = cursorY;
        }

        static TestPageFlowState firstPage(PageStyle pageStyle) {
            return new TestPageFlowState(0, pageStyle.marginTop());
        }

        @Override
        public int pageIndex() {
            return pageIndex;
        }

        @Override
        public double cursorY() {
            return cursorY;
        }

        @Override
        public void cursorY(double cursorY) {
            this.cursorY = cursorY;
        }

        @Override
        public List<LaidOutBlock> currentBlocks() {
            return currentBlocks;
        }

        @Override
        public void advanceToNextPage(PageStyle pageStyle) {
            pageIndex += 1;
            cursorY = pageStyle.marginTop();
            currentBlocks = new ArrayList<>();
        }
    }
}
