package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.TextRun;
import io.github.ggeorg.delos.writer.layout.LaidOutLine;
import io.github.ggeorg.delos.writer.layout.LaidOutRun;
import io.github.ggeorg.delos.writer.layout.GreedyParagraphLayouter;
import io.github.ggeorg.delos.render.RenderFont;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
class LayouterRunContractTest {


    private GreedyParagraphLayouter layouter;
    private RenderFont baseFont;

    @BeforeEach
    void setUp() {
        layouter = new GreedyParagraphLayouter();
        baseFont = new RenderFont("System", 14, false, false);
    }

    @Test
    void preservesStyledRunsAndCaretStopsOnSingleVisualLine() {
        Paragraph paragraph = new Paragraph(List.of(
                TextRun.plain("ab"),
                new TextRun("CD", CharacterStyle.PLAIN.withBold(true)),
                new TextRun("ef", CharacterStyle.PLAIN.withItalic(true))
        ));

        List<LaidOutLine> lines = layouter.layoutLines(paragraph, baseFont, 1000, 4);
        assertEquals(1, lines.size());

        LaidOutLine line = lines.get(0);
        assertEquals("abCDef", line.text());
        assertEquals(7, line.caretStops().size(), "caret stops should equal text length + 1");
        assertEquals(0, line.startOffset());
        assertEquals(6, line.endOffset());

        assertEquals(3, line.runs().size());
        assertRun(line.runs().get(0), "ab", 0, 2, false, false);
        assertRun(line.runs().get(1), "CD", 2, 4, true, false);
        assertRun(line.runs().get(2), "ef", 4, 6, false, true);
    }


    private void assertRun(LaidOutRun run, String text, int start, int end, boolean bold, boolean italic) {
        assertEquals(text, run.text());
        assertEquals(start, run.startColumn());
        assertEquals(end, run.endColumn());
        assertEquals(bold, run.bold());
        assertEquals(italic, run.italic());
    }
}
