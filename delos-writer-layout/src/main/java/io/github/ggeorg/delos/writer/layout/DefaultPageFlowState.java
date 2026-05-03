package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.PageStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Default mutable page-flow cursor used while assembling laid-out pages.
 *
 * <p>The main paginator owns final page creation; this object only tracks the
 * current page index, current Y cursor, and blocks collected for the open page.</p>
 */
final class DefaultPageFlowState implements PageFlowState {
    private int pageIndex;
    private double cursorY;
    private List<LaidOutBlock> currentBlocks;

    private DefaultPageFlowState(int pageIndex, double cursorY, List<LaidOutBlock> currentBlocks) {
        this.pageIndex = pageIndex;
        this.cursorY = cursorY;
        this.currentBlocks = currentBlocks;
    }

    static DefaultPageFlowState firstPage(PageStyle pageStyle) {
        return new DefaultPageFlowState(0, pageStyle.marginTop(), new ArrayList<>());
    }

    static DefaultPageFlowState subsequentPage(PageStyle pageStyle, int pageIndex) {
        return new DefaultPageFlowState(pageIndex, pageStyle.marginTop(), new ArrayList<>());
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
