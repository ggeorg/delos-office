package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.HorizontalRuleBlock;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.Story;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.editor.StoryEdit;
import io.github.ggeorg.delos.writer.editor.StoryEditor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class StoryEditorContractTest {
    @Test
    void insertsTextInsideAStoryParagraph() {
        Story story = Story.ofParagraphs(List.of(Paragraph.of("hello world")));

        StoryEdit edit = new StoryEditor().insert(story, new TextPosition(0, 5), " brave");

        assertEquals("hello brave world", edit.story().paragraphs().getFirst().plainText());
        assertEquals(new TextPosition(0, 11), edit.caretPosition());
    }

    @Test
    void replacementCanSplitOneParagraphIntoSeveralParagraphs() {
        Story story = Story.ofParagraphs(List.of(Paragraph.of("alpha omega")));

        StoryEdit edit = new StoryEditor().replace(story, new TextPosition(0, 6), new TextPosition(0, 6), "beta\ngamma\n");

        assertEquals(3, edit.story().paragraphs().size());
        assertEquals("alpha beta", edit.story().paragraphs().get(0).plainText());
        assertEquals("gamma", edit.story().paragraphs().get(1).plainText());
        assertEquals("omega", edit.story().paragraphs().get(2).plainText());
        assertEquals(new TextPosition(2, 0), edit.caretPosition());
    }

    @Test
    void editingParagraphProjectionPreservesNonParagraphBlocks() {
        Story story = Story.ofBlocks(List.of(
                ParagraphBlock.of(Paragraph.of("before")),
                new HorizontalRuleBlock(),
                ParagraphBlock.of(Paragraph.of("after"))
        ));

        StoryEdit edit = new StoryEditor().replace(story, new TextPosition(1, 0), new TextPosition(1, 5), "done");

        assertEquals(3, edit.story().blocks().size());
        assertInstanceOf(HorizontalRuleBlock.class, edit.story().blocks().get(1));
        assertEquals("before", edit.story().paragraphs().get(0).plainText());
        assertEquals("done", edit.story().paragraphs().get(1).plainText());
    }

    @Test
    void plainTextUsesNewlinesBetweenStoryParagraphs() {
        Story story = Story.ofParagraphs(List.of(
                Paragraph.of("one"),
                Paragraph.of("two")
        ));

        assertEquals("one\ntwo", new StoryEditor().plainText(story));
    }
}
