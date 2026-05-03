package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.Block;
import io.github.ggeorg.delos.writer.document.HorizontalRuleBlock;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.ParagraphStyle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListLayoutStateTest {
    private static final PageStyle PAGE = new PageStyle(400.0, 600.0, 40.0, 40.0, 40.0, 40.0);
    private static final ParagraphStyle NUMBERED = ParagraphStyle.defaultBody().asNumberedListItem(0, 1);
    private static final ParagraphStyle NUMBERED_FROM_FIVE = ParagraphStyle.defaultBody().asNumberedListItem(0, 5);
    private static final ParagraphStyle BULLET = ParagraphStyle.defaultBody().asBulletListItem(0);
    private static final ParagraphStyle BODY = ParagraphStyle.defaultBody();

    @Test
    void advancesNumberedMarkersAndResetsAfterPlainParagraph() {
        ListLayoutState state = new ListLayoutState();

        assertEquals("1.", state.layoutFor(PAGE, Paragraph.of(NUMBERED, "one")).markerText());
        assertEquals("2.", state.layoutFor(PAGE, Paragraph.of(NUMBERED, "two")).markerText());

        ListLayout plain = state.layoutFor(PAGE, Paragraph.of(BODY, "plain"));
        assertFalse(plain.enabled());

        assertEquals("1.", state.layoutFor(PAGE, Paragraph.of(NUMBERED, "one again")).markerText());
    }

    @Test
    void replaysCountersBeforeIncrementalRelayoutAnchor() {
        List<Block> blocks = List.of(
                ParagraphBlock.of(Paragraph.of(NUMBERED, "one")),
                ParagraphBlock.of(Paragraph.of(NUMBERED, "two")),
                ParagraphBlock.of(Paragraph.of(NUMBERED, "three"))
        );

        ListLayoutState state = new ListLayoutState();
        state.replayBeforeParagraph(blocks, 2);

        assertEquals("3.", state.layoutFor(PAGE, Paragraph.of(NUMBERED, "three edited")).markerText());
    }

    @Test
    void nonParagraphBlockResetsReplayState() {
        List<Block> blocks = List.of(
                ParagraphBlock.of(Paragraph.of(NUMBERED, "one")),
                new HorizontalRuleBlock(),
                ParagraphBlock.of(Paragraph.of(NUMBERED, "after rule"))
        );

        ListLayoutState state = new ListLayoutState();
        state.replayBeforeParagraph(blocks, 2);

        assertEquals("2.", state.layoutFor(PAGE, Paragraph.of(NUMBERED, "second after rule")).markerText());
    }

    @Test
    void bulletLayoutUsesListGeometryWithoutAdvancingNumberedCounter() {
        ListLayoutState state = new ListLayoutState();

        assertEquals("1.", state.layoutFor(PAGE, Paragraph.of(NUMBERED, "numbered")).markerText());
        ListLayout bullet = state.layoutFor(PAGE, Paragraph.of(BULLET, "bullet"));
        assertTrue(bullet.enabled());
        assertEquals("•", bullet.markerText());

        assertEquals("2.", state.layoutFor(PAGE, Paragraph.of(NUMBERED, "numbered resumes")).markerText());
    }

    @Test
    void numberedListHonorsExplicitStartValue() {
        ListLayoutState state = new ListLayoutState();

        assertEquals("5.", state.layoutFor(PAGE, Paragraph.of(NUMBERED_FROM_FIVE, "five")).markerText());
        assertEquals("6.", state.layoutFor(PAGE, Paragraph.of(NUMBERED_FROM_FIVE, "six")).markerText());
    }
}
