package io.github.ggeorg.delos.writer.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Optimal breakpoint search for Delos paragraph layout.
 *
 * <p>This class deliberately stops at the algorithmic boundary: it consumes
 * already-measured Knuth-Plass items and returns chosen breakpoints.</p>
 */
public final class KnuthPlassLineBreaker {
    private static final double EPSILON = 1e-9;
    private static final double OVERFULL_PENALTY = 10_000.0;
    private static final double IMPOSSIBLE_RATIO = 1_000.0;
    private static final double CONSECUTIVE_FLAGGED_DEMERITS = 3_000.0;

    public List<Integer> computeBreakpointIndices(List<? extends KnuthPlassTypes.Item> items, double lineWidth) {
        return computeBreakpoints(items, lineWidth).stream()
                .map(KnuthPlassTypes.Breakpoint::itemIndex)
                .toList();
    }

    public List<KnuthPlassTypes.Breakpoint> computeBreakpoints(List<? extends KnuthPlassTypes.Item> items, double lineWidth) {
        Objects.requireNonNull(items, "items");
        if (lineWidth <= 0) {
            throw new IllegalArgumentException("lineWidth must be > 0");
        }
        if (items.isEmpty()) {
            return List.of();
        }

        List<KnuthPlassTypes.Breakpoint> result = new ArrayList<>();
        int segmentStart = 0;
        int lineNumberBase = 0;

        while (segmentStart < items.size()) {
            int forcedBreakIndex = findNextForcedBreak(items, segmentStart);
            int segmentEndExclusive = forcedBreakIndex >= 0 ? forcedBreakIndex + 1 : items.size();
            List<? extends KnuthPlassTypes.Item> segment = items.subList(segmentStart, segmentEndExclusive);
            List<KnuthPlassTypes.Breakpoint> segmentBreakpoints = computeSegmentBreakpoints(segment, lineWidth, segmentStart, lineNumberBase);
            result.addAll(segmentBreakpoints);
            lineNumberBase += segmentBreakpoints.size();

            if (forcedBreakIndex < 0) {
                break;
            }
            segmentStart = forcedBreakIndex + 1;
        }

        return List.copyOf(result);
    }

    private List<KnuthPlassTypes.Breakpoint> computeSegmentBreakpoints(
            List<? extends KnuthPlassTypes.Item> items,
            double lineWidth,
            int itemIndexBase,
            int lineNumberBase
    ) {
        List<Integer> candidates = collectCandidates(items);
        if (candidates.isEmpty()) {
            return List.of();
        }

        SegmentMetricIndex metricIndex = SegmentMetricIndex.create(items);
        int candidateCount = candidates.size();
        double[] bestDemerits = new double[candidateCount];
        int[] bestPreviousCandidate = new int[candidateCount];
        double[] bestRatios = new double[candidateCount];
        int[] bestLineNumbers = new int[candidateCount];

        for (int i = 0; i < candidateCount; i++) {
            bestDemerits[i] = Double.POSITIVE_INFINITY;
            bestPreviousCandidate[i] = -1;
            bestRatios[i] = 0.0;
            bestLineNumbers[i] = 0;
        }

        for (int currentCandidate = 0; currentCandidate < candidateCount; currentCandidate++) {
            int currentIndex = candidates.get(currentCandidate);
            for (int previousCandidate = -1; previousCandidate < currentCandidate; previousCandidate++) {
                int previousIndex = previousCandidate >= 0 ? candidates.get(previousCandidate) : -1;
                double previousDemerits = previousCandidate >= 0 ? bestDemerits[previousCandidate] : 0.0;
                int previousLineNumber = previousCandidate >= 0 ? bestLineNumbers[previousCandidate] : 0;

                if (previousCandidate >= 0 && Double.isInfinite(previousDemerits)) {
                    continue;
                }

                SegmentMetrics metrics = metricIndex.measure(previousIndex, currentIndex);
                if (!metrics.hasContent()) {
                    continue;
                }

                double ratio = adjustmentRatio(metrics, lineWidth);
                double demerits = previousDemerits + lineDemerits(items.get(currentIndex), ratio);
                demerits += flaggedBreakTransitionDemerits(previousIndex >= 0 ? items.get(previousIndex) : null, items.get(currentIndex));
                if (demerits < bestDemerits[currentCandidate]) {
                    bestDemerits[currentCandidate] = demerits;
                    bestPreviousCandidate[currentCandidate] = previousCandidate;
                    bestRatios[currentCandidate] = ratio;
                    bestLineNumbers[currentCandidate] = previousLineNumber + 1;
                }
            }
        }

        if (Double.isInfinite(bestDemerits[candidateCount - 1])) {
            return List.of();
        }

        List<KnuthPlassTypes.Breakpoint> result = new ArrayList<>();
        int cursor = candidateCount - 1;
        while (cursor >= 0) {
            result.add(new KnuthPlassTypes.Breakpoint(
                    itemIndexBase + candidates.get(cursor),
                    bestRatios[cursor],
                    bestDemerits[cursor],
                    lineNumberBase + bestLineNumbers[cursor]
            ));
            cursor = bestPreviousCandidate[cursor];
        }
        Collections.reverse(result);
        return List.copyOf(result);
    }

    private int findNextForcedBreak(List<? extends KnuthPlassTypes.Item> items, int startIndex) {
        for (int i = startIndex; i < items.size(); i++) {
            if (forcedBreak(items.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private List<Integer> collectCandidates(List<? extends KnuthPlassTypes.Item> items) {
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            KnuthPlassTypes.Item item = items.get(i);
            if (item instanceof KnuthPlassTypes.Glue) {
                candidates.add(i);
            } else if (item instanceof KnuthPlassTypes.Penalty penalty && penalty.legalBreakpoint()) {
                candidates.add(i);
            }
        }
        return candidates;
    }

    /**
     * Prefix metrics for repeated segment scoring.
     *
     * <p>The dynamic-programming loop evaluates many overlapping candidate
     * ranges. Measuring each range by scanning the underlying item list makes
     * the inner loop O(n) per candidate pair. This index keeps the original
     * Knuth-Plass segment semantics, but answers width/stretch/shrink queries in
     * O(1): interior boxes/glues are counted from prefix sums, leading glues are
     * skipped, breakpoint glue contributes only stretch/shrink, and breakpoint
     * penalties contribute only their explicit width.</p>
     */
    private static final class SegmentMetricIndex {
        private final List<? extends KnuthPlassTypes.Item> items;
        private final double[] widthPrefix;
        private final double[] stretchPrefix;
        private final double[] shrinkPrefix;
        private final int[] contentPrefix;
        private final int[] nextNonGlueIndex;

        private SegmentMetricIndex(
                List<? extends KnuthPlassTypes.Item> items,
                double[] widthPrefix,
                double[] stretchPrefix,
                double[] shrinkPrefix,
                int[] contentPrefix,
                int[] nextNonGlueIndex
        ) {
            this.items = items;
            this.widthPrefix = widthPrefix;
            this.stretchPrefix = stretchPrefix;
            this.shrinkPrefix = shrinkPrefix;
            this.contentPrefix = contentPrefix;
            this.nextNonGlueIndex = nextNonGlueIndex;
        }

        static SegmentMetricIndex create(List<? extends KnuthPlassTypes.Item> items) {
            int size = items.size();
            double[] widthPrefix = new double[size + 1];
            double[] stretchPrefix = new double[size + 1];
            double[] shrinkPrefix = new double[size + 1];
            int[] contentPrefix = new int[size + 1];
            int[] nextNonGlueIndex = new int[size + 1];
            nextNonGlueIndex[size] = size;

            for (int i = 0; i < size; i++) {
                KnuthPlassTypes.Item item = items.get(i);
                widthPrefix[i + 1] = widthPrefix[i] + interiorWidth(item);
                stretchPrefix[i + 1] = stretchPrefix[i] + interiorStretch(item);
                shrinkPrefix[i + 1] = shrinkPrefix[i] + interiorShrink(item);
                contentPrefix[i + 1] = contentPrefix[i] + interiorContent(item);
            }

            for (int i = size - 1; i >= 0; i--) {
                nextNonGlueIndex[i] = items.get(i) instanceof KnuthPlassTypes.Glue
                        ? nextNonGlueIndex[i + 1]
                        : i;
            }

            return new SegmentMetricIndex(items, widthPrefix, stretchPrefix, shrinkPrefix, contentPrefix, nextNonGlueIndex);
        }

        SegmentMetrics measure(int previousBreakIndex, int currentBreakIndex) {
            int start = nextNonGlueIndex[previousBreakIndex + 1];
            if (start > currentBreakIndex) {
                return SegmentMetrics.EMPTY;
            }

            double width = range(widthPrefix, start, currentBreakIndex);
            double stretch = range(stretchPrefix, start, currentBreakIndex);
            double shrink = range(shrinkPrefix, start, currentBreakIndex);
            int contentCount = range(contentPrefix, start, currentBreakIndex);

            KnuthPlassTypes.Item breakpointItem = items.get(currentBreakIndex);
            if (breakpointItem instanceof KnuthPlassTypes.Box box) {
                width += box.width();
                contentCount++;
            } else if (breakpointItem instanceof KnuthPlassTypes.Glue glue) {
                stretch += glue.stretch();
                shrink += glue.shrink();
            } else if (breakpointItem instanceof KnuthPlassTypes.Penalty penalty) {
                width += penalty.width();
                if (!penalty.text().isEmpty() || penalty.width() > 0) {
                    contentCount++;
                }
            }

            return new SegmentMetrics(width, stretch, shrink, contentCount > 0);
        }

        private static double range(double[] prefix, int startInclusive, int endExclusive) {
            return prefix[endExclusive] - prefix[startInclusive];
        }

        private static int range(int[] prefix, int startInclusive, int endExclusive) {
            return prefix[endExclusive] - prefix[startInclusive];
        }

        private static double interiorWidth(KnuthPlassTypes.Item item) {
            if (item instanceof KnuthPlassTypes.Box box) {
                return box.width();
            }
            if (item instanceof KnuthPlassTypes.Glue glue) {
                return glue.width();
            }
            return 0.0;
        }

        private static double interiorStretch(KnuthPlassTypes.Item item) {
            return item instanceof KnuthPlassTypes.Glue glue ? glue.stretch() : 0.0;
        }

        private static double interiorShrink(KnuthPlassTypes.Item item) {
            return item instanceof KnuthPlassTypes.Glue glue ? glue.shrink() : 0.0;
        }

        private static int interiorContent(KnuthPlassTypes.Item item) {
            return item instanceof KnuthPlassTypes.Box || item instanceof KnuthPlassTypes.Glue ? 1 : 0;
        }
    }

    private boolean forcedBreak(KnuthPlassTypes.Item item) {
        return item instanceof KnuthPlassTypes.Penalty penalty && penalty.forcedBreak();
    }

    private double flaggedBreakTransitionDemerits(KnuthPlassTypes.Item previousBreakpointItem, KnuthPlassTypes.Item currentBreakpointItem) {
        if (previousBreakpointItem instanceof KnuthPlassTypes.Penalty previous
                && currentBreakpointItem instanceof KnuthPlassTypes.Penalty current
                && previous.flagged()
                && current.flagged()) {
            return CONSECUTIVE_FLAGGED_DEMERITS;
        }
        return 0.0;
    }

    private double adjustmentRatio(SegmentMetrics metrics, double lineWidth) {
        double delta = lineWidth - metrics.width();
        if (Math.abs(delta) <= EPSILON) {
            return 0.0;
        }
        if (delta > 0) {
            if (metrics.stretch() > EPSILON) {
                return delta / metrics.stretch();
            }
            return IMPOSSIBLE_RATIO;
        }
        if (metrics.shrink() > EPSILON) {
            return delta / metrics.shrink();
        }
        return -IMPOSSIBLE_RATIO;
    }

    /**
     * A discretionary hyphen break is allowed to be ragged when the line has no glue.
     * Treating that case as impossible makes one overfull long word cheaper than
     * correctly splitting it through hyphen penalties. Plain textless penalties are
     * still scored normally so they do not become free early breaks.
     */
    private boolean isUnderfullDiscretionaryHyphenBreak(KnuthPlassTypes.Item breakpointItem, double ratio) {
        if (ratio < IMPOSSIBLE_RATIO) {
            return false;
        }
        if (!(breakpointItem instanceof KnuthPlassTypes.Penalty penalty) || penalty.forcedBreak()) {
            return false;
        }
        return !penalty.text().isEmpty() || penalty.width() > EPSILON || penalty.flagged();
    }

    private double lineDemerits(KnuthPlassTypes.Item breakpointItem, double ratio) {
        if (forcedBreak(breakpointItem) && ratio >= 0.0) {
            return 0.0;
        }

        double badness = isUnderfullDiscretionaryHyphenBreak(breakpointItem, ratio)
                ? 0.0
                : 100.0 * Math.pow(Math.abs(ratio), 3.0);
        if (ratio < -1.0) {
            badness += OVERFULL_PENALTY;
        }

        double demerits = Math.pow(1.0 + badness, 2.0);
        if (breakpointItem instanceof KnuthPlassTypes.Penalty penalty && !penalty.forcedBreak()) {
            double penaltyContribution = (double) penalty.penalty() * penalty.penalty();
            demerits += penalty.penalty() >= 0 ? penaltyContribution : -penaltyContribution;
        }
        return demerits;
    }

    private record SegmentMetrics(double width, double stretch, double shrink, boolean hasContent) {
        private static final SegmentMetrics EMPTY = new SegmentMetrics(0.0, 0.0, 0.0, false);
    }
}
