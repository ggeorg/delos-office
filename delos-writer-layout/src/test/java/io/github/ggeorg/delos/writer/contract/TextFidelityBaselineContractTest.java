package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.TextRun;
import io.github.ggeorg.delos.writer.layout.GreedyParagraphLayouter;
import io.github.ggeorg.delos.writer.layout.LaidOutLine;
import io.github.ggeorg.delos.writer.layout.TextMeasurer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextFidelityBaselineContractTest {
    @Test
    void greedyWrappingUsesMeasuredRunWidthInsteadOfSummedCharacterWidths() {
        GreedyParagraphLayouter layouter = new GreedyParagraphLayouter(new KerningAwareTestMeasurer());
        RenderFont font = new RenderFont("Test", 10.0, false, false);

        List<LaidOutLine> lines = layouter.layoutLines(
                new Paragraph(List.of(TextRun.plain("AVB"))),
                font,
                19.0,
                0.0
        );

        assertEquals(2, lines.size());
        assertEquals("AV", lines.getFirst().text());
        assertEquals(18.0, lines.getFirst().width(), 0.0001);
        assertEquals(18.0, lines.getFirst().runs().getFirst().width(), 0.0001);
        assertEquals(List.of(0.0, 10.0, 18.0), lines.getFirst().caretStops());
        assertEquals("B", lines.get(1).text());
    }

    private static final class KerningAwareTestMeasurer implements TextMeasurer {
        @Override
        public RenderFont styledFont(RenderFont baseFont, boolean bold, boolean italic) {
            return new RenderFont(baseFont.family(), baseFont.size(), bold, italic);
        }

        @Override
        public double textWidth(String text, RenderFont font) {
            String safeText = text == null ? "" : text;
            return switch (safeText) {
                case "" -> 0.0;
                case "A", "V", "B" -> 10.0;
                case "AV" -> 18.0;
                case "AVB" -> 28.0;
                default -> safeText.length() * 10.0;
            };
        }

        @Override
        public double charWidth(char ch, RenderFont font) {
            return 10.0;
        }

        @Override
        public double lineHeight(RenderFont font) {
            return 12.0;
        }

        @Override
        public double baseline(RenderFont font) {
            return 8.0;
        }

        @Override
        public List<Double> caretStops(String text, RenderFont font) {
            String safeText = text == null ? "" : text;
            List<Double> stops = new ArrayList<>(safeText.length() + 1);
            stops.add(0.0);
            for (int i = 1; i <= safeText.length(); i++) {
                stops.add(textWidth(safeText.substring(0, i), font));
            }
            return List.copyOf(stops);
        }
    }
}
