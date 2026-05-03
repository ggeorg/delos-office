package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.PageStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPageFlowStateTest {
    @Test
    void startsAtFirstPageMarginAndAdvancesToFreshPage() {
        PageStyle pageStyle = new PageStyle(200, 300, 20, 30, 40, 50);
        DefaultPageFlowState state = DefaultPageFlowState.firstPage(pageStyle);

        assertEquals(0, state.pageIndex());
        assertEquals(pageStyle.marginTop(), state.cursorY(), 0.001);
        assertTrue(state.currentBlocks().isEmpty());

        state.currentBlocks().add(new LaidOutSeparator(-1, 20, 40, 120, 4));
        state.cursorY(88.0);
        state.advanceToNextPage(pageStyle);

        assertEquals(1, state.pageIndex());
        assertEquals(pageStyle.marginTop(), state.cursorY(), 0.001);
        assertTrue(state.currentBlocks().isEmpty());
    }
}
