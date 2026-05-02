package io.github.ggeorg.delos.writer.editor;

import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.document.TextRun;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ParagraphRunsContractTest {
    @Test
    void prefixAndSuffixPreserveRunStylesWhenSplittingInsideStyledRuns() {
        Paragraph paragraph = new Paragraph(
                List.of(
                        new TextRun("Hello", CharacterStyle.PLAIN.withBold(true)),
                        new TextRun("World", CharacterStyle.PLAIN.withItalic(true))
                )
        );

        List<TextRun> prefix = ParagraphRuns.prefix(paragraph, 7);
        List<TextRun> suffix = ParagraphRuns.suffix(paragraph, 7);

        assertEquals(List.of(new TextRun("Hello", CharacterStyle.PLAIN.withBold(true)), new TextRun("Wo", CharacterStyle.PLAIN.withItalic(true))), prefix);
        assertEquals(List.of(new TextRun("rld", CharacterStyle.PLAIN.withItalic(true))), suffix);
    }

    @Test
    void clampPositionKeepsCaretInsideExistingParagraphBounds() {
        List<Paragraph> paragraphs = List.of(Paragraph.of("abc"));

        assertEquals(new TextPosition(0, 3), ParagraphRuns.clampPosition(paragraphs, new TextPosition(9, 99)));
        assertEquals(new TextPosition(0, 0), ParagraphRuns.clampPosition(paragraphs, new TextPosition(-1, -5)));
    }

    @Test
    void normalizeReplacementUsesSingleNewlineRepresentation() {
        assertEquals("a\nb\nc", ParagraphRuns.normalizeReplacement("a\r\nb\rc"));
    }
}
