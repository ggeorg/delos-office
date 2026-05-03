package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.FormulaBlock;
import io.github.ggeorg.delos.writer.document.HorizontalRuleBlock;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.document.PageStyle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtomicBlockPaginatorTest {
    private static final double EPSILON = 0.0001;

    @Test
    void appendsImageOnCurrentPageWhenItFits() {
        PageStyle pageStyle = new PageStyle(100, 100, 10, 10, 10, 10);
        List<LaidOutPage> pages = new ArrayList<>();
        TestPageFlowState state = TestPageFlowState.firstPage(pageStyle);

        new AtomicBlockPaginator().appendImage(
            pageStyle,
            pages,
            state,
            7,
            new ImageBlock("media/small.png", 20, 20, "small")
        );

        assertEquals(0, pages.size());
        assertEquals(0, state.pageIndex());
        assertEquals(1, state.currentBlocks().size());
        LaidOutAtomicBlock block = (LaidOutAtomicBlock) state.currentBlocks().get(0);
        assertEquals(7, block.sourceBlockIndex());
        assertClose(10.0, block.x());
        assertClose(10.0, block.y());
        assertClose(20.0, block.height());
        assertClose(36.0, state.cursorY());
    }

    @Test
    void movesAtomicBlockToFreshPageWhenItDoesNotFitCurrentPage() {
        PageStyle pageStyle = new PageStyle(100, 100, 10, 10, 10, 10);
        List<LaidOutPage> pages = new ArrayList<>();
        TestPageFlowState state = TestPageFlowState.firstPage(pageStyle);
        state.currentBlocks().add(new LaidOutSeparator(-1, 10, 10, 80, 75));
        state.cursorY(85.0);

        new AtomicBlockPaginator().appendImage(
            pageStyle,
            pages,
            state,
            2,
            new ImageBlock("media/next-page.png", 20, 20, "next")
        );

        assertEquals(1, pages.size());
        assertEquals(1, state.pageIndex());
        assertEquals(1, state.currentBlocks().size());
        LaidOutAtomicBlock block = (LaidOutAtomicBlock) state.currentBlocks().get(0);
        assertEquals(2, block.sourceBlockIndex());
        assertClose(10.0, block.y());
        assertClose(36.0, state.cursorY());
    }

    @Test
    void appendsFormulaAndHorizontalRuleUsingSharedAtomicPaginationPath() {
        PageStyle pageStyle = new PageStyle(200, 200, 20, 20, 20, 20);
        List<LaidOutPage> pages = new ArrayList<>();
        TestPageFlowState state = TestPageFlowState.firstPage(pageStyle);
        AtomicBlockPaginator paginator = new AtomicBlockPaginator();

        paginator.appendFormula(
            pageStyle,
            LayoutTheme.defaultTheme(),
            pages,
            state,
            3,
            new FormulaBlock("E = mc^2", "energy")
        );
        paginator.appendHorizontalRule(pageStyle, pages, state, 4, new HorizontalRuleBlock());

        assertEquals(0, pages.size());
        assertEquals(2, state.currentBlocks().size());
        assertTrue(state.currentBlocks().get(0) instanceof LaidOutFormulaBlock);
        assertTrue(state.currentBlocks().get(1) instanceof LaidOutSeparator);
        assertEquals(3, ((LaidOutAtomicBlock) state.currentBlocks().get(0)).sourceBlockIndex());
        assertEquals(4, ((LaidOutAtomicBlock) state.currentBlocks().get(1)).sourceBlockIndex());
    }

    private static void assertClose(double expected, double actual) {
        assertTrue(Math.abs(expected - actual) < EPSILON);
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
