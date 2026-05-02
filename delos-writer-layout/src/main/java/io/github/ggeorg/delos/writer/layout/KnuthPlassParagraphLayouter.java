package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.hyphenation.DefaultHyphenators;
import io.github.ggeorg.delos.hyphenation.HyphenationPolicy;
import io.github.ggeorg.delos.hyphenation.Hyphenator;
import io.github.ggeorg.delos.writer.document.Alignment;
import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.layout.ParagraphLayouterSupport.StyledText;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphStyle;
import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.TextLayoutResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Experimental paragraph layouter backed by {@link KnuthPlassLineBreaker}.
 *
 * <p>v12 H3 wires the already-measured Knuth-Plass items into the real Delos
 * line pipeline: paragraphs are tokenized into boxes/glue/penalties, the
 * breakpoint search chooses visual line ends, and the chosen ranges are
 * materialized back into {@link LaidOutLine} instances with stable runs and
 * caret stops. Greedy remains the production default; this strategy exists so
 * typography work can advance without destabilizing pagination.</p>
 *
 * <p>v12 H4 teaches the experimental layouter to honor {@link Alignment#JUSTIFY}
 * on non-terminal visual lines by distributing the line breaker's adjustment
 * ratio through measured glue fragments instead of treating justify like a
 * plain left-aligned line.</p>
 *
 * <p>v13 H5 hardens paragraph-level justify behavior around mixed styling,
 * explicit newline segment boundaries, and lines that have no visible glue to
 * distribute.</p>
 *
 * <p>v117.14 makes this the general paragraph line breaker for all alignments.
 * Hyphenation is no longer hidden behind {@link Alignment#JUSTIFY}; left,
 * center, right, and justified paragraphs all get the same discretionary
 * hyphen opportunities, while justification remains only a spacing decision.</p>
 */
public final class KnuthPlassParagraphLayouter implements ParagraphLayouter {
    private static final double EPSILON = 1e-9;

    private final TextMeasurer measurer;
    private final KnuthPlassLineBreaker lineBreaker;
    private final Hyphenator hyphenator;
    private final HyphenationPolicy hyphenationPolicy;
    private final ParagraphLayouterSupport support;

    public KnuthPlassParagraphLayouter() {
        this(new ApproximateTextMeasurer());
    }

    public KnuthPlassParagraphLayouter(TextMeasurer measurer) {
        this(measurer, new KnuthPlassLineBreaker(), DefaultHyphenators.english());
    }

    public KnuthPlassParagraphLayouter(TextMeasurer measurer, KnuthPlassLineBreaker lineBreaker) {
        this(measurer, lineBreaker, DefaultHyphenators.english());
    }

    public KnuthPlassParagraphLayouter(TextMeasurer measurer, KnuthPlassLineBreaker lineBreaker, Hyphenator hyphenator) {
        this(measurer, lineBreaker, hyphenator, HyphenationPolicy.english());
    }

    public KnuthPlassParagraphLayouter(
            TextMeasurer measurer,
            KnuthPlassLineBreaker lineBreaker,
            Hyphenator hyphenator,
            HyphenationPolicy hyphenationPolicy
    ) {
        this.measurer = Objects.requireNonNull(measurer, "measurer");
        this.lineBreaker = Objects.requireNonNull(lineBreaker, "lineBreaker");
        this.hyphenationPolicy = Objects.requireNonNull(hyphenationPolicy, "hyphenationPolicy");
        this.hyphenator = Objects.requireNonNullElse(hyphenator, Hyphenator.NONE);
        this.support = new ParagraphLayouterSupport(this.measurer);
    }

    @Override
    public List<LaidOutLine> layoutLines(Paragraph paragraph, RenderFont baseFont, double maxWidth, double lineGap) {
        Objects.requireNonNull(paragraph, "paragraph");
        Objects.requireNonNull(baseFont, "baseFont");

        StyledText chars = support.styledText(paragraph);
        List<LaidOutLine> lines = new ArrayList<>();
        ParagraphStyle style = paragraph.style();

        if (chars.isEmpty()) {
            lines.add(support.emptyLine(baseFont, 0, 0, Math.max(0, style.firstLineIndent())));
            return lines;
        }

        double y = 0;
        int sourceIndex = 0;
        boolean firstVisualLine = true;

        while (sourceIndex < chars.size()) {
            int newlineIndex = chars.indexOf('\n', sourceIndex);
            int segmentEndExclusive = newlineIndex >= 0 ? newlineIndex : chars.size();
            double firstLineIndent = firstVisualLine ? Math.max(0, style.firstLineIndent()) : 0;

            if (segmentEndExclusive == sourceIndex) {
                int offset = chars.offset(sourceIndex);
                lines.add(support.emptyLine(baseFont, offset, y, firstLineIndent));
                y += support.lineAdvance(baseFont, style, lineGap);
                firstVisualLine = false;
                sourceIndex = sourceIndex + 1;
                continue;
            }

            double availableWidth = Math.max(1, maxWidth - firstLineIndent);
            List<KnuthPlassTypes.Item> items = tokenizeSegment(chars, sourceIndex, segmentEndExclusive, baseFont, availableWidth);
            List<KnuthPlassTypes.Breakpoint> breakpoints = paragraphStyleUsesJustification(style)
                    ? lineBreaker.computeBreakpoints(items, availableWidth)
                    : computeRaggedBreakpoints(items, maxWidth, firstLineIndent);

            if (breakpoints.isEmpty()) {
                breakpoints = List.of(new KnuthPlassTypes.Breakpoint(items.size() - 1, 0.0, 0.0, 1));
            }

            int previousBreak = -1;
            for (int breakpointIndex = 0; breakpointIndex < breakpoints.size(); breakpointIndex++) {
                KnuthPlassTypes.Breakpoint breakpoint = breakpoints.get(breakpointIndex);
                boolean lastLineInSegment = breakpointIndex == breakpoints.size() - 1;
                lines.add(materializeLine(
                        items,
                        previousBreak,
                        breakpoint,
                        baseFont,
                        y,
                        firstVisualLine,
                        lastLineInSegment,
                        maxWidth,
                        style
                ));
                y += support.lineAdvance(baseFont, style, lineGap);
                firstVisualLine = false;
                previousBreak = breakpoint.itemIndex();
            }

            if (newlineIndex < 0) {
                break;
            }
            sourceIndex = newlineIndex + 1;
        }

        return lines;
    }


    private boolean paragraphStyleUsesJustification(ParagraphStyle style) {
        return style.alignment() == Alignment.JUSTIFY;
    }

    private List<KnuthPlassTypes.Breakpoint> computeRaggedBreakpoints(
            List<KnuthPlassTypes.Item> items,
            double maxWidth,
            double firstLineIndent
    ) {
        List<KnuthPlassTypes.Breakpoint> breakpoints = new ArrayList<>();
        int previousBreakIndex = -1;
        int lineNumber = 1;

        while (previousBreakIndex < items.size() - 1) {
            double availableWidth = Math.max(1.0, maxWidth - (lineNumber == 1 ? firstLineIndent : 0.0));
            int chosenCandidate = -1;
            int firstCandidate = -1;

            for (int i = previousBreakIndex + 1; i < items.size(); i++) {
                KnuthPlassTypes.Item item = items.get(i);
                if (!isLegalBreakpoint(item)) {
                    continue;
                }

                if (firstCandidate < 0) {
                    firstCandidate = i;
                }

                double naturalWidth = naturalLineWidth(items, previousBreakIndex, i);
                if (naturalWidth <= availableWidth + EPSILON) {
                    chosenCandidate = i;
                }

                if (naturalWidth > availableWidth + EPSILON && chosenCandidate >= 0) {
                    break;
                }
            }

            if (chosenCandidate < 0) {
                chosenCandidate = firstCandidate >= 0 ? firstCandidate : items.size() - 1;
            }

            breakpoints.add(new KnuthPlassTypes.Breakpoint(chosenCandidate, 0.0, 0.0, lineNumber));
            previousBreakIndex = chosenCandidate;
            lineNumber++;
        }

        return List.copyOf(breakpoints);
    }

    private boolean isLegalBreakpoint(KnuthPlassTypes.Item item) {
        return item instanceof KnuthPlassTypes.Glue
                || item instanceof KnuthPlassTypes.Penalty penalty && penalty.legalBreakpoint();
    }


    private double naturalLineWidth(List<KnuthPlassTypes.Item> items, int previousBreakIndex, int currentBreakIndex) {
        double width = 0.0;
        for (RunFragment fragment : collectFragments(items, previousBreakIndex, currentBreakIndex)) {
            if (!fragment.breakpointGlue()) {
                width += fragment.width();
            }
        }
        return width;
    }

    private List<KnuthPlassTypes.Item> tokenizeSegment(StyledText chars, int startInclusive, int endExclusive, RenderFont baseFont, double availableWidth) {
        List<KnuthPlassTypes.Item> items = new ArrayList<>();
        int i = startInclusive;
        while (i < endExclusive) {
            char firstChar = chars.ch(i);
            boolean whitespace = Character.isWhitespace(firstChar);
            CharacterStyle style = chars.style(i);
            int j = i + 1;

            while (j < endExclusive) {
                char nextChar = chars.ch(j);
                CharacterStyle nextStyle = chars.style(j);
                if (Character.isWhitespace(nextChar) != whitespace || !style.sameAs(nextStyle)) {
                    break;
                }
                j++;
            }

            String text = support.sliceText(chars, i, j);
            RenderFont font = measurer.styledFont(baseFont, style.bold(), style.italic());
            int startOffset = chars.offset(i);
            int endOffset = chars.offset(j - 1) + 1;

            if (whitespace) {
                double width = measurer.textWidth(text, font);
                double stretch = Math.max(1.0, width * 0.5);
                double shrink = Math.max(0.5, width / 3.0);
                items.add(new KnuthPlassTypes.Glue(text, style, startOffset, endOffset, width, stretch, shrink));
            } else {
                appendHyphenatedBoxes(items, text, style, startOffset, font, availableWidth);
            }
            i = j;
        }

        int finalOffset = chars.offset(endExclusive - 1) + 1;
        items.add(new KnuthPlassTypes.Penalty("", finalOffset, 0, KnuthPlassTypes.FORCED_BREAK_PENALTY, false));
        return List.copyOf(items);
    }

    private void appendHyphenatedBoxes(
            List<KnuthPlassTypes.Item> items,
            String sourceText,
            CharacterStyle style,
            int sourceStartOffset,
            RenderFont font,
            double availableWidth
    ) {
        VisibleWord visibleWord = toVisibleWord(sourceText, sourceStartOffset);
        String visibleText = visibleWord.text();
        if (visibleText.isEmpty()) {
            return;
        }

        double visibleWidth = measurer.textWidth(visibleText, font);
        List<Integer> breakpoints = hyphenator.hyphenationPoints(sourceText);
        if (breakpoints.isEmpty()) {
            items.add(new KnuthPlassTypes.Box(
                    visibleText,
                    style,
                    visibleWord.sourceOffsetAt(0),
                    visibleWord.sourceOffsetAt(visibleText.length()),
                    visibleWidth
            ));
            return;
        }

        var explicitBreakpoints = explicitSoftHyphenBreakpoints(sourceText);
        double hyphenWidth = measurer.textWidth("-", font);
        int previousVisible = 0;
        for (int breakpoint : breakpoints) {
            int clamped = Math.max(previousVisible + 1, Math.min(breakpoint, visibleText.length() - 1));
            if (clamped <= previousVisible || clamped >= visibleText.length()) {
                continue;
            }
            String segment = visibleText.substring(previousVisible, clamped);
            if (!segment.isEmpty()) {
                items.add(new KnuthPlassTypes.Box(
                        segment,
                        style,
                        visibleWord.sourceOffsetAt(previousVisible),
                        visibleWord.sourceOffsetAt(clamped),
                        measurer.textWidth(segment, font)
                ));
                boolean explicitBreak = explicitBreakpoints.contains(clamped);
                int penalty = hyphenationPolicy.penaltyFor(visibleText, explicitBreak);
                items.add(new KnuthPlassTypes.Penalty("-", visibleWord.sourceOffsetAt(clamped), hyphenWidth, penalty, true));
            }
            previousVisible = clamped;
        }

        if (previousVisible < visibleText.length()) {
            String tail = visibleText.substring(previousVisible);
            items.add(new KnuthPlassTypes.Box(
                    tail,
                    style,
                    visibleWord.sourceOffsetAt(previousVisible),
                    visibleWord.sourceOffsetAt(visibleText.length()),
                    measurer.textWidth(tail, font)
            ));
        }
    }


    private List<Integer> explicitSoftHyphenBreakpoints(String sourceText) {
        List<Integer> points = new ArrayList<>();
        int visibleIndex = 0;
        int visibleLength = 0;
        for (int i = 0; i < sourceText.length(); i++) {
            if (sourceText.charAt(i) != '\u00AD') {
                visibleLength++;
            }
        }
        for (int i = 0; i < sourceText.length(); i++) {
            char ch = sourceText.charAt(i);
            if (ch == '\u00AD') {
                if (visibleIndex > 0 && visibleIndex < visibleLength) {
                    points.add(visibleIndex);
                }
            } else {
                visibleIndex++;
            }
        }
        return points;
    }

    private VisibleWord toVisibleWord(String sourceText, int sourceStartOffset) {
        StringBuilder visible = new StringBuilder();
        List<Integer> boundaryOffsets = new ArrayList<>();
        boundaryOffsets.add(sourceStartOffset);
        for (int i = 0; i < sourceText.length(); i++) {
            char ch = sourceText.charAt(i);
            if (ch == '­') {
                continue;
            }
            visible.append(ch);
            boundaryOffsets.add(sourceStartOffset + i + 1);
        }
        return new VisibleWord(visible.toString(), List.copyOf(boundaryOffsets));
    }

    private LaidOutLine materializeLine(
            List<KnuthPlassTypes.Item> items,
            int previousBreakIndex,
            KnuthPlassTypes.Breakpoint breakpoint,
            RenderFont baseFont,
            double y,
            boolean firstVisualLine,
            boolean lastLineInSegment,
            double maxWidth,
            ParagraphStyle paragraphStyle
    ) {
        double firstLineIndent = firstVisualLine ? Math.max(0, paragraphStyle.firstLineIndent()) : 0;
        List<RunFragment> fragments = collectFragments(items, previousBreakIndex, breakpoint.itemIndex());
        if (fragments.isEmpty()) {
            int offset = safeOffset(items, previousBreakIndex, breakpoint.itemIndex());
            return support.emptyLine(baseFont, offset, y, firstLineIndent);
        }

        boolean justifyLine = shouldJustify(paragraphStyle.alignment(), lastLineInSegment, fragments);
        JustificationPlan justificationPlan = justifyLine
                ? justificationPlan(fragments, maxWidth - firstLineIndent)
                : JustificationPlan.none();
        StringBuilder lineText = new StringBuilder();
        List<Double> relativeCaretStops = new ArrayList<>();
        List<Integer> relativeCaretOffsets = new ArrayList<>();
        List<LaidOutRun> runs = new ArrayList<>();
        relativeCaretStops.add(0.0);
        relativeCaretOffsets.add(fragments.getFirst().startOffset());

        double relativeX = 0;
        int lineColumn = 0;
        int startOffset = fragments.getFirst().startOffset();
        int endOffset = fragments.getLast().endOffset();

        for (RunFragment fragment : fragments) {
            RenderFont runFont = measurer.styledFont(baseFont, fragment.style().bold(), fragment.style().italic());
            TextLayoutResult baseLayout = measurer.layoutText(fragment.text(), runFont);
            List<Double> baseStops = baseLayout.caretStops();
            double baseWidth = baseLayout.width();
            double runWidth = adjustedFragmentWidth(fragment, breakpoint.adjustmentRatio(), justifyLine, baseWidth, justificationPlan);
            List<Double> runStops = adjustedCaretStops(baseStops, runWidth);
            double runX = relativeX;

            runs.add(new LaidOutRun(
                    fragment.text(),
                    lineColumn,
                    lineColumn + fragment.text().length(),
                    runX,
                    runWidth,
                    fragment.style()
            ));

            lineText.append(fragment.text());
            for (int i = 1; i < runStops.size(); i++) {
                relativeCaretStops.add(runX + runStops.get(i));
                relativeCaretOffsets.add(fragment.caretOffsets().get(Math.min(i, fragment.caretOffsets().size() - 1)));
            }

            relativeX += runWidth;
            lineColumn += fragment.text().length();
            endOffset = Math.max(endOffset, fragment.endOffset());
            startOffset = Math.min(startOffset, fragment.startOffset());
        }

        double lineX = ParagraphLayouterSupport.alignedX(paragraphStyle.alignment(), firstLineIndent, maxWidth, relativeX);
        return new LaidOutLine(
                lineText.toString(),
                lineX,
                y,
                relativeX,
                measurer.lineHeight(baseFont),
                measurer.baseline(baseFont),
                startOffset,
                endOffset,
                runs,
                relativeCaretStops,
                relativeCaretOffsets
        );
    }

    private boolean shouldJustify(Alignment alignment, boolean lastLineInSegment, List<RunFragment> fragments) {
        if (alignment != Alignment.JUSTIFY || lastLineInSegment) {
            return false;
        }

        boolean hasVisibleContent = false;
        boolean hasVisibleGlue = false;
        for (RunFragment fragment : fragments) {
            if (fragment.breakpointGlue()) {
                continue;
            }
            if (fragment.glue()) {
                hasVisibleGlue = true;
            } else if (!fragment.text().isEmpty()) {
                hasVisibleContent = true;
            }
        }
        return hasVisibleContent && hasVisibleGlue;
    }

    private double adjustedFragmentWidth(
            RunFragment fragment,
            double adjustmentRatio,
            boolean justifyLine,
            double baseWidth,
            JustificationPlan justificationPlan
    ) {
        if (fragment.breakpointGlue()) {
            return 0.0;
        }
        if (!justifyLine || !fragment.glue()) {
            return baseWidth;
        }

        double delta;
        if (justificationPlan.active()) {
            delta = adjustmentRatio >= 0
                    ? justificationPlan.stretchDeltaFor(fragment)
                    : justificationPlan.shrinkDeltaFor(fragment);
        } else if (Math.abs(adjustmentRatio) > EPSILON) {
            delta = adjustmentRatio > 0
                    ? adjustmentRatio * fragment.stretch()
                    : adjustmentRatio * fragment.shrink();
        } else {
            delta = 0.0;
        }
        return Math.max(0.0, baseWidth + delta);
    }


    private JustificationPlan justificationPlan(List<RunFragment> fragments, double targetWidth) {
        double naturalWidth = 0.0;
        double visibleStretch = 0.0;
        double visibleShrink = 0.0;
        boolean hasVisibleGlue = false;

        for (RunFragment fragment : fragments) {
            if (fragment.breakpointGlue()) {
                continue;
            }
            naturalWidth += fragment.width();
            if (fragment.glue()) {
                hasVisibleGlue = true;
                visibleStretch += fragment.stretch();
                visibleShrink += fragment.shrink();
            }
        }

        if (!hasVisibleGlue) {
            return JustificationPlan.none();
        }

        double delta = targetWidth - naturalWidth;
        if (Math.abs(delta) <= EPSILON) {
            return JustificationPlan.none();
        }
        if (delta > 0 && visibleStretch > EPSILON) {
            return new JustificationPlan(delta, visibleStretch, visibleShrink, true);
        }
        if (delta < 0 && visibleShrink > EPSILON) {
            return new JustificationPlan(delta, visibleStretch, visibleShrink, true);
        }
        return JustificationPlan.none();
    }

    private List<Double> adjustedCaretStops(List<Double> baseStops, double targetWidth) {
        if (baseStops.isEmpty()) {
            return List.of(0.0);
        }

        double baseWidth = baseStops.get(baseStops.size() - 1);
        if (baseStops.size() == 1 || Math.abs(targetWidth - baseWidth) <= EPSILON) {
            return baseStops;
        }

        List<Double> adjusted = new ArrayList<>(baseStops.size());
        adjusted.add(0.0);
        if (Math.abs(baseWidth) <= EPSILON) {
            int charCount = baseStops.size() - 1;
            for (int i = 1; i < baseStops.size(); i++) {
                adjusted.add(targetWidth * i / charCount);
            }
            return List.copyOf(adjusted);
        }

        double scale = targetWidth / baseWidth;
        for (int i = 1; i < baseStops.size(); i++) {
            adjusted.add(baseStops.get(i) * scale);
        }
        return List.copyOf(adjusted);
    }

    private List<RunFragment> collectFragments(List<KnuthPlassTypes.Item> items, int previousBreakIndex, int currentBreakIndex) {
        List<RunFragment> fragments = new ArrayList<>();
        int startIndex = Math.max(0, previousBreakIndex + 1);
        for (int i = startIndex; i <= currentBreakIndex && i < items.size(); i++) {
            KnuthPlassTypes.Item item = items.get(i);
            if (item instanceof KnuthPlassTypes.Box box) {
                fragments.add(new RunFragment(
                        box.text(),
                        box.style(),
                        box.startOffset(),
                        box.endOffset(),
                        false,
                        false,
                        box.width(),
                        0.0,
                        0.0,
                        defaultFragmentCaretOffsets(box.startOffset(), box.endOffset(), box.text().length())
                ));
            } else if (item instanceof KnuthPlassTypes.Glue glue) {
                fragments.add(new RunFragment(
                        glue.text(),
                        glue.style(),
                        glue.startOffset(),
                        glue.endOffset(),
                        true,
                        i == currentBreakIndex,
                        glue.width(),
                        glue.stretch(),
                        glue.shrink(),
                        defaultFragmentCaretOffsets(glue.startOffset(), glue.endOffset(), glue.text().length())
                ));
            } else if (i == currentBreakIndex && item instanceof KnuthPlassTypes.Penalty penalty && !penalty.text().isEmpty()) {
                CharacterStyle style = fragments.isEmpty() ? CharacterStyle.PLAIN : fragments.getLast().style();
                fragments.add(new RunFragment(
                        penalty.text(),
                        style,
                        penalty.offset(),
                        penalty.offset(),
                        false,
                        false,
                        penalty.width(),
                        0.0,
                        0.0,
                        List.of(penalty.offset(), penalty.offset())
                ));
            }
        }
        return fragments;
    }

    private int safeOffset(List<KnuthPlassTypes.Item> items, int previousBreakIndex, int currentBreakIndex) {
        int startIndex = Math.max(0, previousBreakIndex + 1);
        for (int i = startIndex; i <= currentBreakIndex && i < items.size(); i++) {
            KnuthPlassTypes.Item item = items.get(i);
            if (item instanceof KnuthPlassTypes.Box box) {
                return box.startOffset();
            }
            if (item instanceof KnuthPlassTypes.Glue glue) {
                return glue.startOffset();
            }
            if (item instanceof KnuthPlassTypes.Penalty penalty) {
                return penalty.offset();
            }
        }
        return 0;
    }


    private record VisibleWord(String text, List<Integer> sourceOffsets) {
        private int sourceOffsetAt(int visibleIndex) {
            return sourceOffsets.get(visibleIndex);
        }
    }

    private record RunFragment(
            String text,
            CharacterStyle style,
            int startOffset,
            int endOffset,
            boolean glue,
            boolean breakpointGlue,
            double width,
            double stretch,
            double shrink,
            List<Integer> caretOffsets
    ) {
        RunFragment {
            caretOffsets = normalizeCaretOffsets(startOffset, endOffset, text.length(), caretOffsets);
        }
    }

    private static List<Integer> defaultFragmentCaretOffsets(int startOffset, int endOffset, int textLength) {
        return normalizeCaretOffsets(startOffset, endOffset, textLength, null);
    }

    private static List<Integer> normalizeCaretOffsets(
            int startOffset,
            int endOffset,
            int textLength,
            List<Integer> candidate
    ) {
        int expectedSize = Math.max(0, textLength) + 1;
        if (candidate != null && candidate.size() == expectedSize) {
            return List.copyOf(candidate);
        }
        List<Integer> offsets = new ArrayList<>(expectedSize);
        int span = Math.max(0, endOffset - startOffset);
        for (int i = 0; i < expectedSize; i++) {
            offsets.add(startOffset + Math.min(i, span));
        }
        if (!offsets.isEmpty()) {
            offsets.set(offsets.size() - 1, endOffset);
        }
        return List.copyOf(offsets);
    }

    private record JustificationPlan(
            double delta,
            double totalStretch,
            double totalShrink,
            boolean active
    ) {
        private static JustificationPlan none() {
            return new JustificationPlan(0.0, 0.0, 0.0, false);
        }

        private double stretchDeltaFor(RunFragment fragment) {
            if (!active || totalStretch <= EPSILON) {
                return 0.0;
            }
            return delta * (fragment.stretch() / totalStretch);
        }

        private double shrinkDeltaFor(RunFragment fragment) {
            if (!active || totalShrink <= EPSILON) {
                return 0.0;
            }
            return delta * (fragment.shrink() / totalShrink);
        }
    }

}
