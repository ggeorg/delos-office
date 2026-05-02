package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.TextLayoutResult;
import io.github.ggeorg.delos.writer.document.Alignment;
import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphStyle;
import io.github.ggeorg.delos.writer.document.TextRun;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Shared helpers for paragraph line breakers.
 *
 * <p>Helpers in this class turn rich paragraph runs into style-aware character
 * streams and measured line runs.</p>
 */
final class ParagraphLayouterSupport {
    private final TextMeasurer measurer;

    ParagraphLayouterSupport(TextMeasurer measurer) {
        this.measurer = Objects.requireNonNull(measurer, "measurer");
    }

    static double alignedX(Alignment alignment, double indent, double maxWidth, double lineWidth) {
        double availableWidth = Math.max(0, maxWidth - indent);
        return switch (alignment) {
            case CENTER -> indent + Math.max(0, (availableWidth - lineWidth) / 2.0);
            case RIGHT -> indent + Math.max(0, availableWidth - lineWidth);
            case JUSTIFY, LEFT -> indent;
        };
    }

    double lineAdvance(RenderFont baseFont, ParagraphStyle style, double lineGap) {
        return measurer.lineHeight(baseFont) * style.lineSpacingMultiplier() + lineGap;
    }

    LaidOutLine emptyLine(RenderFont baseFont, int offset, double y, double x) {
        return new LaidOutLine(
                "",
                x,
                y,
                0,
                measurer.lineHeight(baseFont),
                measurer.baseline(baseFont),
                offset,
                offset,
                List.of(),
                List.of(0.0)
        );
    }

    StyledText styledText(Paragraph paragraph) {
        Objects.requireNonNull(paragraph, "paragraph");
        StringBuilder text = new StringBuilder();
        List<StyleSpan> spans = new ArrayList<>();

        CharacterStyle activeStyle = null;
        int activeStart = 0;
        for (TextRun run : paragraph.runs()) {
            String runText = Objects.requireNonNullElse(run.text(), "");
            if (runText.isEmpty()) {
                continue;
            }
            CharacterStyle runStyle = Objects.requireNonNullElse(run.style(), CharacterStyle.PLAIN);
            if (activeStyle == null) {
                activeStyle = runStyle;
                activeStart = text.length();
            } else if (!activeStyle.sameAs(runStyle)) {
                spans.add(new StyleSpan(activeStart, text.length(), activeStyle));
                activeStyle = runStyle;
                activeStart = text.length();
            }
            text.append(runText);
        }

        if (activeStyle != null) {
            spans.add(new StyleSpan(activeStart, text.length(), activeStyle));
        }
        return new StyledText(text.toString(), spans);
    }

    String sliceText(StyledText chars, int startInclusive, int endExclusive) {
        return chars.text(startInclusive, endExclusive);
    }


    LaidOutLine materializeLine(
            StyledText chars,
            int startInclusive,
            int endExclusive,
            RenderFont baseFont,
            double y,
            boolean firstVisualLine,
            double maxWidth,
            ParagraphStyle paragraphStyle
    ) {
        double firstLineIndent = firstVisualLine ? Math.max(0, paragraphStyle.firstLineIndent()) : 0;
        if (startInclusive >= endExclusive) {
            int offset = startInclusive < chars.size() ? chars.offset(startInclusive) : 0;
            return emptyLine(baseFont, offset, y, firstLineIndent);
        }

        StringBuilder lineText = new StringBuilder();
        List<Double> relativeCaretStops = new ArrayList<>();
        List<LaidOutRun> runs = new ArrayList<>();
        relativeCaretStops.add(0.0);

        double relativeX = 0;
        int lineColumn = 0;
        int runStart = startInclusive;
        while (runStart < endExclusive) {
            CharacterStyle runStyle = chars.style(runStart);
            int runEnd = runStart + 1;
            while (runEnd < endExclusive && runStyle.sameAs(chars.style(runEnd))) {
                runEnd++;
            }

            String runText = chars.text(runStart, runEnd);
            RenderFont runFont = measurer.styledFont(baseFont, runStyle.bold(), runStyle.italic());
            TextLayoutResult runLayout = measurer.layoutText(runText, runFont);
            List<Double> runStops = runLayout.caretStops();
            double runWidth = runLayout.width();
            double runX = relativeX;

            runs.add(new LaidOutRun(
                    runText,
                    lineColumn,
                    lineColumn + runText.length(),
                    runX,
                    runWidth,
                    runStyle
            ));

            lineText.append(runText);
            for (int i = 1; i < runStops.size(); i++) {
                relativeCaretStops.add(runX + runStops.get(i));
            }

            relativeX += runWidth;
            lineColumn += runText.length();
            runStart = runEnd;
        }

        int startOffset = chars.offset(startInclusive);
        int endOffset = chars.offset(endExclusive - 1) + 1;
        double lineX = alignedX(paragraphStyle.alignment(), firstLineIndent, maxWidth, relativeX);
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
                relativeCaretStops
        );
    }

    record StyleSpan(int startInclusive, int endExclusive, CharacterStyle style) {
        StyleSpan {
            if (startInclusive < 0) {
                throw new IllegalArgumentException("startInclusive must be >= 0");
            }
            if (endExclusive < startInclusive) {
                throw new IllegalArgumentException("endExclusive must be >= startInclusive");
            }
            style = Objects.requireNonNullElse(style, CharacterStyle.PLAIN);
        }

        boolean contains(int index) {
            return index >= startInclusive && index < endExclusive;
        }
    }

    static final class StyledText {
        private final String text;
        private final List<StyleSpan> spans;
        private int cachedSpanIndex;

        StyledText(String text, List<StyleSpan> spans) {
            this.text = Objects.requireNonNullElse(text, "");
            this.spans = List.copyOf(Objects.requireNonNull(spans, "spans"));
            this.cachedSpanIndex = 0;
        }

        int size() {
            return text.length();
        }

        boolean isEmpty() {
            return text.isEmpty();
        }

        char ch(int index) {
            return text.charAt(index);
        }

        int offset(int index) {
            return index;
        }

        CharacterStyle style(int index) {
            if (spans.isEmpty()) {
                return CharacterStyle.PLAIN;
            }
            if (cachedSpanIndex >= 0 && cachedSpanIndex < spans.size()) {
                StyleSpan cached = spans.get(cachedSpanIndex);
                if (cached.contains(index)) {
                    return cached.style();
                }
            }
            for (int i = 0; i < spans.size(); i++) {
                StyleSpan span = spans.get(i);
                if (span.contains(index)) {
                    cachedSpanIndex = i;
                    return span.style();
                }
            }
            return spans.getLast().style();
        }

        int indexOf(char ch, int fromIndex) {
            return text.indexOf(String.valueOf(ch), Math.max(0, fromIndex));
        }

        String text(int startInclusive, int endExclusive) {
            return text.substring(startInclusive, endExclusive);
        }
    }
}
