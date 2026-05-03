package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.Block;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class IncrementalLayoutPlannerTest {
    @Test
    void acceptsStableRichBlocksWhenOnlyParagraphContentChanges() {
        IncrementalLayoutPlanner planner = new IncrementalLayoutPlanner();
        List<Block> previous = List.of(
            new ParagraphBlock(Paragraph.of("before")),
            new ImageBlock("media/a.png", 20, 20, "a"),
            new ParagraphBlock(Paragraph.of("after"))
        );
        List<Block> current = List.of(
            new ParagraphBlock(Paragraph.of("before changed")),
            new ImageBlock("media/a.png", 20, 20, "a"),
            new ParagraphBlock(Paragraph.of("after"))
        );

        assertTrue(planner.canReuseIncrementallyWithBlocks(previous, current));
    }

    @Test
    void rejectsChangedRichBlocksAndStructuralChanges() {
        IncrementalLayoutPlanner planner = new IncrementalLayoutPlanner();
        List<Block> previous = List.of(
            new ParagraphBlock(Paragraph.of("before")),
            new ImageBlock("media/a.png", 20, 20, "a"),
            new ParagraphBlock(Paragraph.of("after"))
        );
        List<Block> changedImage = List.of(
            new ParagraphBlock(Paragraph.of("before")),
            new ImageBlock("media/b.png", 20, 20, "b"),
            new ParagraphBlock(Paragraph.of("after"))
        );
        List<Block> insertedBlock = List.of(
            new ParagraphBlock(Paragraph.of("before")),
            new ImageBlock("media/a.png", 20, 20, "a"),
            new ParagraphBlock(Paragraph.of("middle")),
            new ParagraphBlock(Paragraph.of("after"))
        );

        assertFalse(planner.canReuseIncrementallyWithBlocks(previous, changedImage));
        assertFalse(planner.canReuseIncrementallyWithBlocks(previous, insertedBlock));
    }
}
