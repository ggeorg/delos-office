package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.CharacterStyle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParagraphFragmentMetricsTest {
    @Test
    void findsLargestFittingFragmentUsingLineMetrics() {
        ParagraphFragmentMetrics metrics = new ParagraphFragmentMetrics(fixedLines(10, 20.0));

        assertEquals(0, metrics.fittingEndExclusive(0, 19.999));
        assertEquals(1, metrics.fittingEndExclusive(0, 20.0));
        assertEquals(3, metrics.fittingEndExclusive(0, 60.0));
        assertEquals(7, metrics.fittingEndExclusive(4, 60.0));
        assertEquals(10, metrics.fittingEndExclusive(7, 1000.0));
    }

    @Test
    void computesFragmentHeightWithoutMaterializingNormalizedLines() {
        ParagraphFragmentMetrics metrics = new ParagraphFragmentMetrics(fixedLines(5, 18.0));

        assertEquals(0.0, metrics.height(2, 2), 0.0001);
        assertEquals(18.0, metrics.height(2, 3), 0.0001);
        assertEquals(54.0, metrics.height(1, 4), 0.0001);
    }

    @Test
    void normalizesSlicedLinesToFragmentLocalOrigin() {
        ParagraphFragmentMetrics metrics = new ParagraphFragmentMetrics(fixedLines(5, 22.0));

        List<LaidOutLine> fragment = metrics.sliceAndNormalize(2, 5);

        assertEquals(3, fragment.size());
        assertEquals(0.0, fragment.get(0).y(), 0.0001);
        assertEquals(22.0, fragment.get(1).y(), 0.0001);
        assertEquals(44.0, fragment.get(2).y(), 0.0001);
        assertEquals(0, metrics.sliceAndNormalize(2, 2).size());
    }

    @Test
    void fittingEndIsMonotonicForIncreasingAvailableHeight() {
        ParagraphFragmentMetrics metrics = new ParagraphFragmentMetrics(fixedLines(100, 14.0));
        int previous = 0;
        for (double available = 0.0; available <= 1400.0; available += 7.0) {
            int current = metrics.fittingEndExclusive(0, available);
            assertTrue(current >= previous);
            previous = current;
        }
    }

    private static List<LaidOutLine> fixedLines(int lineCount, double lineHeight) {
        List<LaidOutLine> lines = new ArrayList<>(lineCount);
        int offset = 0;
        for (int i = 0; i < lineCount; i++) {
            String text = "line-" + i;
            List<Double> caretStops = new ArrayList<>(text.length() + 1);
            for (int c = 0; c <= text.length(); c++) {
                caretStops.add((double) c * 8.0);
            }
            lines.add(new LaidOutLine(
                text,
                0.0,
                i * lineHeight,
                text.length() * 8.0,
                lineHeight,
                Math.max(1.0, lineHeight - 4.0),
                offset,
                offset + text.length(),
                List.of(new LaidOutRun(text, 0, text.length(), 0.0, text.length() * 8.0, CharacterStyle.PLAIN)),
                caretStops
            ));
            offset += text.length();
        }
        return List.copyOf(lines);
    }
}
