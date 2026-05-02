package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.hyphenation.Hyphenator;
import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.TextLayoutResult;
import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.TextRun;
import io.github.ggeorg.delos.writer.layout.KnuthPlassLineBreaker;
import io.github.ggeorg.delos.writer.layout.KnuthPlassParagraphLayouter;
import io.github.ggeorg.delos.writer.layout.LaidOutLine;
import io.github.ggeorg.delos.writer.layout.TextMeasurer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnuthPlassMeasurementReuseContractTest {

    @Test
    void reusesMeasuredTextRunsBetweenTokenizationAndLineMaterialization() {
        CountingTextMeasurer measurer = new CountingTextMeasurer();
        KnuthPlassParagraphLayouter layouter = new KnuthPlassParagraphLayouter(
                measurer,
                new KnuthPlassLineBreaker(),
                Hyphenator.NONE
        );
        RenderFont font = new RenderFont("Test", 12.0, false, false);

        List<LaidOutLine> lines = layouter.layoutLines(Paragraph.of("alpha beta alpha beta"), font, 1_000.0, 0.0);

        assertEquals("alpha beta alpha beta", lines.stream().map(LaidOutLine::sourceText).reduce("", String::concat));
        assertEquals(1, measurer.layoutCallsFor("alpha", font),
                "the same plain word must be measured once even though both tokenization and materialization need it");
        assertEquals(1, measurer.layoutCallsFor("beta", font),
                "repeated equal words in the same font should reuse the same measured layout");
        assertEquals(1, measurer.layoutCallsFor(" ", font),
                "glue width and glue caret stops should come from one shared measurement");
        assertTrue(measurer.totalLayoutCalls() <= 3,
                "only the unique visible text/font runs should require backend layout measurements");
    }

    @Test
    void measurementReuseIsScopedByFontStyle() {
        CountingTextMeasurer measurer = new CountingTextMeasurer();
        KnuthPlassParagraphLayouter layouter = new KnuthPlassParagraphLayouter(
                measurer,
                new KnuthPlassLineBreaker(),
                Hyphenator.NONE
        );
        RenderFont baseFont = new RenderFont("Test", 12.0, false, false);
        RenderFont boldFont = new RenderFont("Test", 12.0, true, false);
        Paragraph paragraph = new Paragraph(List.of(
                TextRun.plain("same"),
                new TextRun("same", CharacterStyle.PLAIN.withBold(true))
        ));

        List<LaidOutLine> lines = layouter.layoutLines(paragraph, baseFont, 1_000.0, 0.0);

        assertEquals("samesame", lines.stream().map(LaidOutLine::sourceText).reduce("", String::concat));
        assertEquals(1, measurer.layoutCallsFor("same", baseFont),
                "plain text and bold text must not share a measurement because their fonts differ");
        assertEquals(1, measurer.layoutCallsFor("same", boldFont),
                "bold text should still be measured only once for its own font");
        assertEquals(2, measurer.totalLayoutCalls());
    }

    private static final class CountingTextMeasurer implements TextMeasurer {
        private final Map<Key, Integer> layoutCalls = new HashMap<>();

        @Override
        public RenderFont styledFont(RenderFont baseFont, boolean bold, boolean italic) {
            return new RenderFont(baseFont.family(), baseFont.size(), bold, italic);
        }

        @Override
        public double textWidth(String text, RenderFont font) {
            return widthOf(text, font);
        }

        @Override
        public double charWidth(char ch, RenderFont font) {
            return widthOf(String.valueOf(ch), font);
        }

        @Override
        public double lineHeight(RenderFont font) {
            return 14.0;
        }

        @Override
        public double baseline(RenderFont font) {
            return 10.0;
        }

        @Override
        public List<Double> caretStops(String text, RenderFont font) {
            String safeText = text == null ? "" : text;
            List<Double> stops = new ArrayList<>(safeText.length() + 1);
            stops.add(0.0);
            double x = 0.0;
            for (int i = 0; i < safeText.length(); i++) {
                x += widthOf(String.valueOf(safeText.charAt(i)), font);
                stops.add(x);
            }
            return List.copyOf(stops);
        }

        @Override
        public TextLayoutResult layoutText(String text, RenderFont font) {
            String safeText = text == null ? "" : text;
            layoutCalls.merge(new Key(safeText, font), 1, Integer::sum);
            return new TextLayoutResult(
                    safeText,
                    font,
                    widthOf(safeText, font),
                    lineHeight(font),
                    baseline(font),
                    caretStops(safeText, font)
            );
        }

        private double widthOf(String text, RenderFont font) {
            String safeText = text == null ? "" : text;
            double multiplier = font.bold() ? 11.0 : 10.0;
            if (safeText.isBlank() && !safeText.isEmpty()) {
                return safeText.length() * multiplier / 2.0;
            }
            return safeText.length() * multiplier;
        }

        private int layoutCallsFor(String text, RenderFont font) {
            return layoutCalls.getOrDefault(new Key(text, font), 0);
        }

        private int totalLayoutCalls() {
            return layoutCalls.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    private record Key(String text, RenderFont font) {
    }
}
