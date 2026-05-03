package io.github.ggeorg.delos.writer.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable per-paragraph line metrics used by the paginator when splitting a
 * visual paragraph into page fragments.
 *
 * <p>The paginator asks the same questions many times while it flows a long
 * paragraph: how tall is a slice, which line is the last one that fits, and how
 * do the selected lines look when normalized to a fragment-local y origin. This
 * helper keeps those operations explicit and avoids repeatedly rediscovering
 * the same geometry from the raw {@link LaidOutLine} list.</p>
 */
final class ParagraphFragmentMetrics {
    private final List<LaidOutLine> lines;
    private final double[] lineTops;
    private final double[] lineBottoms;

    ParagraphFragmentMetrics(List<LaidOutLine> lines) {
        this.lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        this.lineTops = new double[this.lines.size()];
        this.lineBottoms = new double[this.lines.size()];
        for (int i = 0; i < this.lines.size(); i++) {
            LaidOutLine line = this.lines.get(i);
            lineTops[i] = line.y();
            lineBottoms[i] = line.y() + line.height();
        }
    }

    int lineCount() {
        return lines.size();
    }

    /**
     * Returns the exclusive line index of the largest fragment that fits in the
     * available height. If no line fits, {@code startLineIndex} is returned.
     */
    int fittingEndExclusive(int startLineIndex, double availableHeight) {
        if (startLineIndex < 0 || startLineIndex >= lineCount() || availableHeight < 0.0) {
            return startLineIndex;
        }

        int bestEndExclusive = startLineIndex;
        int low = startLineIndex + 1;
        int high = lineCount();
        while (low <= high) {
            int candidateEndExclusive = (low + high) >>> 1;
            double candidateHeight = height(startLineIndex, candidateEndExclusive);
            if (candidateHeight <= availableHeight) {
                bestEndExclusive = candidateEndExclusive;
                low = candidateEndExclusive + 1;
            } else {
                high = candidateEndExclusive - 1;
            }
        }
        return bestEndExclusive;
    }

    double height(int startInclusive, int endExclusive) {
        if (startInclusive >= endExclusive) {
            return 0.0;
        }
        checkRange(startInclusive, endExclusive);
        return lineBottoms[endExclusive - 1] - lineTops[startInclusive];
    }

    List<LaidOutLine> sliceAndNormalize(int startInclusive, int endExclusive) {
        if (startInclusive >= endExclusive) {
            return List.of();
        }
        checkRange(startInclusive, endExclusive);

        List<LaidOutLine> fragment = new ArrayList<>(endExclusive - startInclusive);
        double baseY = lineTops[startInclusive];
        for (int i = startInclusive; i < endExclusive; i++) {
            LaidOutLine line = lines.get(i);
            fragment.add(new LaidOutLine(
                line.text(),
                line.x(),
                line.y() - baseY,
                line.width(),
                line.height(),
                line.baseline(),
                line.startOffset(),
                line.endOffset(),
                line.runs(),
                line.caretStops(),
                line.caretOffsets()
            ));
        }
        return List.copyOf(fragment);
    }

    private void checkRange(int startInclusive, int endExclusive) {
        if (startInclusive < 0 || endExclusive > lineCount() || startInclusive > endExclusive) {
            throw new IndexOutOfBoundsException(
                "Invalid paragraph fragment range: " + startInclusive + ".." + endExclusive
                    + " for " + lineCount() + " lines"
            );
        }
    }
}
