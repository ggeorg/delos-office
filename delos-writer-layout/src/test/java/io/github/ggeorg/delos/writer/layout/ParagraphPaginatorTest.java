package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphStyle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParagraphPaginatorTest {
    @Test
    void appendsSingleParagraphFragmentAndAppliesSpacingAfterWhenAnotherParagraphFollows() {
        PageStyle pageStyle = new PageStyle(100, 100, 10, 10, 10, 10);
        List<LaidOutPage> pages = new ArrayList<>();
        TestPageFlowState state = TestPageFlowState.firstPage(pageStyle);
        Paragraph paragraph = Paragraph.of(ParagraphStyle.defaultBody().withSpacingAfter(7.0), "first");

        new ParagraphPaginator(PaginatingDocumentLayoutEngine.PaginationPolicy.relaxed()).appendParagraph(
            pageStyle,
            List.of(paragraph, Paragraph.of("next")),
            pages,
            state,
            0,
            paragraph,
            lines(0, 1),
            ListLayout.none(pageStyle)
        );

        assertEquals(0, pages.size());
        assertEquals(1, state.currentBlocks().size());
        LaidOutTextBlock block = (LaidOutTextBlock) state.currentBlocks().getFirst();
        assertEquals(0, block.sourceParagraphIndex());
        assertTrue(block.firstFragment());
        assertTrue(block.lastFragment());
        assertEquals(1, block.lines().size());
        assertEquals(27.0, state.cursorY(), 0.001);
    }

    @Test
    void splitsLongParagraphAcrossPagesWithoutChangingLineOrder() {
        PageStyle pageStyle = new PageStyle(100, 60, 10, 10, 10, 10);
        List<LaidOutPage> pages = new ArrayList<>();
        TestPageFlowState state = TestPageFlowState.firstPage(pageStyle);
        Paragraph paragraph = Paragraph.of("long");

        new ParagraphPaginator(PaginatingDocumentLayoutEngine.PaginationPolicy.relaxed()).appendParagraph(
            pageStyle,
            List.of(paragraph),
            pages,
            state,
            0,
            paragraph,
            lines(0, 5),
            ListLayout.none(pageStyle)
        );

        assertEquals(1, pages.size());
        LaidOutTextBlock first = (LaidOutTextBlock) pages.getFirst().blocks().getFirst();
        LaidOutTextBlock second = (LaidOutTextBlock) state.currentBlocks().getFirst();
        assertEquals(0, first.startLineIndex());
        assertEquals(4, first.lines().size());
        assertTrue(first.firstFragment());
        assertFalse(first.lastFragment());
        assertEquals(4, second.startLineIndex());
        assertEquals(1, second.lines().size());
        assertFalse(second.firstFragment());
        assertTrue(second.lastFragment());
        assertEquals("line-4", second.lines().getFirst().text());
    }

    @Test
    void keepsShortParagraphTogetherOnFreshPageWhenItFitsThere() {
        PageStyle pageStyle = new PageStyle(100, 60, 10, 10, 10, 10);
        List<LaidOutPage> pages = new ArrayList<>();
        TestPageFlowState state = TestPageFlowState.firstPage(pageStyle);
        state.currentBlocks().add(new LaidOutSeparator(-1, 10, 10, 80, 20));
        state.cursorY(30.0);
        Paragraph paragraph = Paragraph.of("short");

        new ParagraphPaginator(PaginatingDocumentLayoutEngine.PaginationPolicy.defaults()).appendParagraph(
            pageStyle,
            List.of(Paragraph.of("previous"), paragraph),
            pages,
            state,
            1,
            paragraph,
            lines(0, 3),
            ListLayout.none(pageStyle)
        );

        assertEquals(1, pages.size());
        assertEquals(1, state.pageIndex());
        LaidOutTextBlock block = (LaidOutTextBlock) state.currentBlocks().getFirst();
        assertEquals(3, block.lines().size());
        assertEquals(10.0, block.y(), 0.001);
    }

    @Test
    void listMarkerAppearsOnlyOnTheFirstParagraphFragment() {
        PageStyle pageStyle = new PageStyle(100, 40, 10, 10, 10, 10);
        List<LaidOutPage> pages = new ArrayList<>();
        TestPageFlowState state = TestPageFlowState.firstPage(pageStyle);
        Paragraph paragraph = Paragraph.of("list");
        ListLayout listLayout = new ListLayout(true, "1.", 25.0, 65.0, 10.0);

        new ParagraphPaginator(PaginatingDocumentLayoutEngine.PaginationPolicy.relaxed()).appendParagraph(
            pageStyle,
            List.of(paragraph),
            pages,
            state,
            0,
            paragraph,
            lines(0, 3),
            listLayout
        );

        LaidOutTextBlock first = (LaidOutTextBlock) pages.getFirst().blocks().getFirst();
        LaidOutTextBlock second = (LaidOutTextBlock) state.currentBlocks().getFirst();
        assertTrue(first.listMarker().visible());
        assertEquals("1.", first.listMarker().text());
        assertFalse(second.listMarker().visible());
    }

    private static List<LaidOutLine> lines(int start, int count) {
        List<LaidOutLine> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int lineIndex = start + i;
            lines.add(new LaidOutLine(
                "line-" + lineIndex,
                0.0,
                i * 10.0,
                40.0,
                10.0,
                8.0,
                lineIndex * 10,
                lineIndex * 10 + 6,
                List.of(),
                List.of(0.0, 40.0),
                List.of(lineIndex * 10, lineIndex * 10 + 6)
            ));
        }
        return List.copyOf(lines);
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
