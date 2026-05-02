package io.github.ggeorg.delos.writer.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class StoryModelContractTest {
    @Test
    void documentBodyIsAStoryWhileBlocksRemainCompatible() {
        Document document = Document.fromBlocks("Story", PageStyle.a4Default(), List.of(
                ParagraphBlock.of(Paragraph.of("alpha")),
                new HorizontalRuleBlock(),
                ParagraphBlock.of(Paragraph.of("beta"))
        ));

        assertEquals(3, document.body().blocks().size());
        assertEquals(document.body().blocks(), document.blocks());
        assertEquals(2, document.paragraphs().size());
        assertEquals("alpha", document.paragraphs().getFirst().plainText());
        assertEquals("beta", document.paragraphs().getLast().plainText());
    }

    @Test
    void tableCellContentIsAStoryWhileParagraphProjectionRemainsCompatible() {
        TableCell cell = new TableCell(List.of(
                Paragraph.of("line one"),
                Paragraph.of("line two")
        ));

        assertEquals(2, cell.content().blocks().size());
        assertEquals(2, cell.paragraphs().size());
        assertEquals("line one", cell.paragraphs().getFirst().plainText());
        assertEquals("line two", cell.paragraphs().getLast().plainText());
    }

    @Test
    void storyRejectsEmptyContentContainers() {
        assertThrows(IllegalArgumentException.class, () -> Story.ofBlocks(List.of()));
        assertThrows(IllegalArgumentException.class, () -> Story.ofParagraphs(List.of()));
    }
}
