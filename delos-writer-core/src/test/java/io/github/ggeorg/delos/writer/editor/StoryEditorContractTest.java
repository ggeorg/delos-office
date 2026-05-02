package io.github.ggeorg.delos.writer.editor;

import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.Story;
import io.github.ggeorg.delos.writer.document.TextPosition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class StoryEditorContractTest {
    @Test
    void insertReturnsStoryEditWithUpdatedStoryAndCaret() {
        Story story = Story.ofParagraphs(List.of(Paragraph.of("hello world")));

        StoryEdit edit = new StoryEditor().insert(story, new TextPosition(0, 5), " brave");

        assertEquals("hello brave world", edit.story().paragraphs().getFirst().plainText());
        assertEquals(new TextPosition(0, 11), edit.caretPosition());
    }

    @Test
    void replaceWholeStoryTextCanBeExpressedWithReplace() {
        Story story = Story.ofParagraphs(List.of(Paragraph.of("old")));

        StoryEdit edit = new StoryEditor().replace(story, new TextPosition(0, 0), new TextPosition(0, 3), "new");

        assertEquals("new", edit.story().paragraphs().getFirst().plainText());
        assertEquals(new TextPosition(0, 3), edit.caretPosition());
    }
}
