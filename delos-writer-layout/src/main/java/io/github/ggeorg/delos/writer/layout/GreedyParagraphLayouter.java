package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphStyle;
import io.github.ggeorg.delos.writer.layout.ParagraphLayouterSupport.StyledText;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Simple greedy line breaker that preserves paragraph source offsets for caret
 * mapping.
 */
public final class GreedyParagraphLayouter implements ParagraphLayouter {
    private final TextMeasurer measurer;
    private final ParagraphLayouterSupport support;

    public GreedyParagraphLayouter() {
        this(new ApproximateTextMeasurer());
    }

    public GreedyParagraphLayouter(TextMeasurer measurer) {
        this.measurer = Objects.requireNonNull(measurer, "measurer");
        this.support = new ParagraphLayouterSupport(this.measurer);
    }

    @Override
    public List<LaidOutLine> layoutLines(Paragraph paragraph, RenderFont baseFont, double maxWidth, double lineGap) {
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
            double firstLineIndent = firstVisualLine ? Math.max(0, style.firstLineIndent()) : 0;
            double availableWidth = Math.max(1, maxWidth - firstLineIndent);
            if (chars.ch(sourceIndex) == '\n') {
                lines.add(support.emptyLine(baseFont, chars.offset(sourceIndex), y, firstLineIndent));
                y += support.lineAdvance(baseFont, style, lineGap);
                sourceIndex++;
                firstVisualLine = false;
                continue;
            }

            int lineStart = sourceIndex;
            int i = sourceIndex;
            int lastBreakSourceIndex = -1;
            double width = 0;
            double widthBeforeActiveRun = 0;
            int activeRunStart = sourceIndex;
            CharacterStyle activeStyle = null;
            RenderFont activeFont = null;

            while (i < chars.size() && chars.ch(i) != '\n') {
                CharacterStyle charStyle = chars.style(i);
                if (activeStyle == null || !activeStyle.sameAs(charStyle)) {
                    activeStyle = charStyle;
                    activeFont = measurer.styledFont(baseFont, charStyle.bold(), charStyle.italic());
                    widthBeforeActiveRun = width;
                    activeRunStart = i;
                }
                char ch = chars.ch(i);
                double candidateWidth = widthBeforeActiveRun
                        + measurer.textWidth(chars.text(activeRunStart, i + 1), activeFont);
                if (candidateWidth <= availableWidth || i == sourceIndex) {
                    width = candidateWidth;
                    if (Character.isWhitespace(ch)) {
                        lastBreakSourceIndex = i + 1;
                    }
                    i++;
                } else {
                    break;
                }
            }

            int lineEndExclusive;
            if (i < chars.size() && chars.ch(i) == '\n') {
                lineEndExclusive = i;
                sourceIndex = i + 1;
            } else if (i >= chars.size()) {
                lineEndExclusive = i;
                sourceIndex = i;
            } else if (lastBreakSourceIndex > lineStart) {
                lineEndExclusive = lastBreakSourceIndex;
                sourceIndex = lastBreakSourceIndex;
            } else {
                lineEndExclusive = i;
                sourceIndex = i;
            }

            lines.add(support.materializeLine(chars, lineStart, lineEndExclusive, baseFont, y, firstVisualLine, maxWidth, style));
            y += support.lineAdvance(baseFont, style, lineGap);
            firstVisualLine = false;
        }

        return lines;
    }

}
