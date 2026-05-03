package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Alignment;
import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphStyle;
import io.github.ggeorg.delos.writer.document.TextRun;
import io.github.ggeorg.delos.writer.layout.KnuthPlassParagraphLayouter;
import io.github.ggeorg.delos.writer.layout.LaidOutLine;
import io.github.ggeorg.delos.writer.layout.LaidOutRun;
import io.github.ggeorg.delos.writer.layout.TextMeasurer;
import io.github.ggeorg.delos.render.RenderFont;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnuthPlassParagraphLayouterTest {

    private TextMeasurer measurer() {
        return new io.github.ggeorg.delos.writer.layout.ApproximateTextMeasurer();
    }

    private KnuthPlassParagraphLayouter knuthPlass() {
        return new KnuthPlassParagraphLayouter(measurer());
    }

    @Test
    void materializesChosenBreaksBackIntoStableLinesAndRuns() {
        Paragraph paragraph = new Paragraph(List.of(
                TextRun.plain("Delos keeps "),
                new TextRun("styled", CharacterStyle.PLAIN.withBold(true)),
                TextRun.plain(" words flowing through the experimental layouter.")
        ));

        List<LaidOutLine> lines = runDirectly(() -> knuthPlass().layoutLines(
                paragraph,
                new RenderFont("System", 14, false, false),
                135,
                4
        ));

        assertTrue(lines.size() >= 2, "expected the experimental layouter to wrap the paragraph");
        assertEquals("Delos keeps styled words flowing through the experimental layouter.",
                lines.stream().map(LaidOutLine::sourceText).reduce("", String::concat));
        assertEquals(0, lines.getFirst().startOffset());
        assertEquals(paragraph.length(), lines.getLast().endOffset());
        assertTrue(lines.stream().flatMap(line -> line.runs().stream()).anyMatch(LaidOutRun::bold));
        assertTrue(lines.stream().allMatch(line -> line.caretStops().size() == line.text().length() + 1));
    }

    @Test
    void leftAlignedParagraphsUseKnuthPlassWithoutJustifyingSpacing() {
        Paragraph paragraph = Paragraph.of("Delos should keep normal left aligned paragraphs readable while hyphenation works outside justify.");
        RenderFont font = new RenderFont("System", 14, false, false);

        List<LaidOutLine> lines = runDirectly(() -> knuthPlass().layoutLines(paragraph, font, 180, 4));

        assertTrue(lines.size() >= 2, "expected the paragraph to wrap");
        assertEquals(paragraph.plainText(), lines.stream().map(LaidOutLine::sourceText).reduce("", String::concat));
        for (LaidOutLine line : lines) {
            assertTrue(line.width() <= 180.0 + 0.001,
                    "left-aligned lines may naturally fill the measure, but must not exceed it");
            assertEquals(0.0, line.x(), 0.001,
                    "left-aligned lines should remain anchored at the left edge");
            assertEquals(line.width(), line.caretStops().getLast(), 0.001,
                    "caret stops must remain normalized to the laid-out line width");
        }

        Paragraph justifiedParagraph = Paragraph.of(
                ParagraphStyle.defaultBody().withAlignment(Alignment.JUSTIFY),
                paragraph.plainText()
        );
        List<LaidOutLine> justifiedLines = runDirectly(() -> knuthPlass().layoutLines(justifiedParagraph, font, 180, 4));
        assertTrue(justifiedLines.size() >= 2, "expected the justified paragraph to wrap as well");
        assertTrue(justifiedLines.subList(0, justifiedLines.size() - 1).stream()
                        .anyMatch(line -> Math.abs(line.width() - 180.0) < 1.0),
                "justification stretching should be limited to JUSTIFY alignment, not required from LEFT alignment");
    }

    @Test
    void preservesExplicitLineBreaksIncludingBlankLines() {
        Paragraph paragraph = Paragraph.of("alpha\n\nbeta");

        List<LaidOutLine> lines = runDirectly(() -> knuthPlass().layoutLines(
                paragraph,
                new RenderFont("System", 14, false, false),
                500,
                4
        ));

        assertEquals(3, lines.size());
        assertEquals("alpha", lines.get(0).text());
        assertEquals("", lines.get(1).text());
        assertEquals("beta", lines.get(2).text());
        assertEquals(0, lines.get(0).startOffset());
        assertEquals(5, lines.get(0).endOffset());
        assertEquals(6, lines.get(1).startOffset());
        assertEquals(6, lines.get(1).endOffset());
        assertEquals(7, lines.get(2).startOffset());
        assertEquals(11, lines.get(2).endOffset());
        assertFalse(lines.get(2).runs().isEmpty());
    }

    @Test
    void justifyExpandsNonTerminalLinesToTheAvailableWidth() {
        Paragraph paragraph = Paragraph.of(
                ParagraphStyle.defaultBody().withAlignment(Alignment.JUSTIFY),
                "Delos writer justification should stretch glue across non terminal lines only."
        );

        List<LaidOutLine> lines = runDirectly(() -> knuthPlass().layoutLines(
                paragraph,
                new RenderFont("System", 14, false, false),
                180,
                4
        ));

        assertTrue(lines.size() >= 2, "expected a wrapped justified paragraph");
        for (int i = 0; i < lines.size() - 1; i++) {
            assertTrue(Math.abs(lines.get(i).width() - 180.0) < 1.0,
                    "non-terminal justified lines should fill the available width");
            assertEquals(0.0, lines.get(i).x(), 0.001);
        }
        assertTrue(lines.getLast().width() < 180.0 - 1.0,
                "the final line should remain ragged rather than stretched");
    }

    @Test
    void justifyPreservesMixedRunStylingAndCaretStopsAcrossExpandedLines() {
        Paragraph paragraph = new Paragraph(
                ParagraphStyle.defaultBody().withAlignment(Alignment.JUSTIFY),
                List.of(
                        TextRun.plain("Delos "),
                        new TextRun("bold", CharacterStyle.PLAIN.withBold(true)),
                        TextRun.plain(" and "),
                        new TextRun("italic", CharacterStyle.PLAIN.withItalic(true)),
                        TextRun.plain(" runs should justify cleanly across the same paragraph with stable caret mapping.")
                )
        );

        List<LaidOutLine> lines = runDirectly(() -> knuthPlass().layoutLines(
                paragraph,
                new RenderFont("System", 14, false, false),
                190,
                4
        ));

        assertTrue(lines.size() >= 2, "expected a wrapped justified paragraph");
        assertEquals("Delos bold and italic runs should justify cleanly across the same paragraph with stable caret mapping.",
                lines.stream().map(LaidOutLine::sourceText).reduce("", String::concat));
        assertTrue(lines.stream().flatMap(line -> line.runs().stream()).anyMatch(LaidOutRun::bold));
        assertTrue(lines.stream().flatMap(line -> line.runs().stream()).anyMatch(LaidOutRun::italic));
        assertTrue(lines.stream().allMatch(line -> line.caretStops().size() == line.text().length() + 1));
    }

    @Test
    void justifyDoesNotStretchSegmentsThatEndAtExplicitNewlines() {
        double maxWidth = 150.0;
        Paragraph paragraph = Paragraph.of(
                ParagraphStyle.defaultBody().withAlignment(Alignment.JUSTIFY),
                "alpha beta gamma\ndelta epsilon zeta eta theta iota"
        );

        List<LaidOutLine> lines = runDirectly(() -> knuthPlass().layoutLines(
                paragraph,
                new RenderFont("System", 14, false, false),
                maxWidth,
                4
        ));

        assertTrue(lines.size() >= 3, "expected at least one wrapped line after the explicit newline");
        assertEquals("alpha beta gamma", lines.get(0).text());
        assertTrue(lines.get(0).width() < maxWidth - 1.0,
                "a segment-final line caused by an explicit newline should remain ragged");
        assertTrue(Math.abs(lines.get(1).width() - maxWidth) < 1.0,
                "the next segment should still justify its non-terminal lines");
    }

    @Test
    void justifyLeavesSingleWordSegmentsRaggedWhenThereIsNoVisibleGlue() {
        Paragraph paragraph = Paragraph.of(
                ParagraphStyle.defaultBody().withAlignment(Alignment.JUSTIFY),
                "solitary\nalpha beta gamma delta epsilon"
        );
        RenderFont font = new RenderFont("System", 14, false, false);
        TextMeasurer measurer = measurer();

        double maxWidth = 150.0;
        List<LaidOutLine> lines = runDirectly(() -> knuthPlass().layoutLines(
                paragraph,
                font,
                maxWidth,
                4
        ));

        assertTrue(lines.size() >= 3, "expected the second segment to wrap");
        LaidOutLine solitaryLine = lines.getFirst();
        double naturalWidth = runDirectly(() -> measurer.textWidth("solitary", font));
        assertEquals("solitary", solitaryLine.text());
        assertEquals(naturalWidth, solitaryLine.width(), 0.5,
                "single-word lines with no visible glue should keep their natural width");
        assertTrue(solitaryLine.width() < maxWidth - 1.0,
                "single-word lines should remain ragged rather than stretched");
    }


    @Test
    void automaticallyHyphenatesLongWordsWhenNeeded() {
        Paragraph paragraph = Paragraph.of(
                ParagraphStyle.defaultBody(),
                "demonstration"
        );

        List<LaidOutLine> lines = runDirectly(() -> knuthPlass().layoutLines(
                paragraph,
                new RenderFont("System", 14, false, false),
                48,
                4
        ));

        assertTrue(lines.size() >= 2, "expected the long word to wrap through a discretionary hyphen");
        assertTrue(lines.stream().limit(lines.size() - 1).anyMatch(line -> line.text().endsWith("-")),
                "expected a non-terminal line to end with a discretionary hyphen");
        assertEquals("demonstration", lines.stream().map(LaidOutLine::sourceText).reduce("", String::concat));
    }

    @Test
    void automaticHyphenGlyphDoesNotBecomeDocumentSourceTextOrOffset() {
        Paragraph paragraph = Paragraph.of(
                ParagraphStyle.defaultBody(),
                "demonstration"
        );

        List<LaidOutLine> lines = runDirectly(() -> knuthPlass().layoutLines(
                paragraph,
                new RenderFont("System", 14, false, false),
                48,
                4
        ));

        LaidOutLine hyphenated = lines.stream()
                .filter(line -> line.text().endsWith("-"))
                .findFirst()
                .orElseThrow();
        assertTrue(hyphenated.length() > hyphenated.sourceText().length(),
                "the visible discretionary hyphen should be a glyph, not source text");
        assertEquals(hyphenated.endOffset(), hyphenated.offsetForColumn(hyphenated.length()),
                "clicking/careting after the visible hyphen must resolve to the source break offset");
        assertEquals(
                "demonstration",
                lines.stream().map(LaidOutLine::sourceText).reduce("", String::concat),
                "line source text reconstruction must ignore automatically inserted hyphen glyphs"
        );
    }


    @Test
    void doesNotAutoHyphenateCodeLikeTokens() {
        String token = "https://example.com/demo";
        Paragraph paragraph = Paragraph.of(
                ParagraphStyle.defaultBody(),
                token
        );

        List<LaidOutLine> lines = runDirectly(() -> knuthPlass().layoutLines(
                paragraph,
                new RenderFont("System", 14, false, false),
                60,
                4
        ));

        assertEquals(token, lines.stream().map(LaidOutLine::sourceText).reduce("", String::concat));
        assertTrue(lines.stream().noneMatch(line -> line.text().endsWith("-")),
                "code-like tokens should not gain automatic discretionary hyphens");
    }

    @Test
    void justifyDistributesExtraSpaceThroughGlueRuns() {
        Paragraph paragraph = Paragraph.of(
                ParagraphStyle.defaultBody().withAlignment(Alignment.JUSTIFY),
                "alpha beta gamma delta epsilon zeta"
        );
        RenderFont font = new RenderFont("System", 14, false, false);

        List<LaidOutLine> lines = runDirectly(() -> knuthPlass().layoutLines(
                paragraph,
                font,
                165,
                4
        ));

        assertTrue(lines.size() >= 2, "expected a wrapped justified paragraph");
        LaidOutLine firstLine = lines.getFirst();
        TextMeasurer measurer = measurer();
        boolean foundExpandedGlue = runDirectly(() -> firstLine.runs().stream()
                .filter(run -> !run.text().isEmpty() && run.text().chars().allMatch(Character::isWhitespace))
                .anyMatch(run -> run.width() > measurer.textWidth(run.text(), measurer.styledFont(font, run.bold(), run.italic())) + 0.25));

        assertTrue(foundExpandedGlue, "expected justification to widen at least one glue run");
    }

    @Test
    void hyphenationWorksForCenterAndRightAlignedParagraphsToo() {
        RenderFont font = new RenderFont("System", 14, false, false);
        for (Alignment alignment : List.of(Alignment.CENTER, Alignment.RIGHT)) {
            Paragraph paragraph = Paragraph.of(
                    ParagraphStyle.defaultBody().withAlignment(alignment),
                    "demonstration"
            );

            List<LaidOutLine> lines = runDirectly(() -> knuthPlass().layoutLines(paragraph, font, 48, 4));

            assertTrue(lines.size() >= 2, "expected " + alignment + " paragraph to hyphenate");
            assertTrue(lines.stream().limit(lines.size() - 1).anyMatch(line -> line.text().endsWith("-")),
                    "expected a visible discretionary hyphen for " + alignment);
        }
    }

    private static <T> T runDirectly(java.util.function.Supplier<T> supplier) {
        return supplier.get();
    }
}
