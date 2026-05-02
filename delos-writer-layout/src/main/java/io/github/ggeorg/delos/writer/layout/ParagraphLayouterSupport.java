package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.writer.document.Alignment;
import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphStyle;
import io.github.ggeorg.delos.writer.document.TextRun;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Shared helpers for paragraph layouters.
 *
 * <p>v52 keeps the same logical character model but removes the old
 * one-record-per-character flattening allocation. Layouters now work against a
 * compact styled character view: one backing text buffer plus style spans.</p>
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

        String text(int startInclusive, int endExclusive) {
            return text.substring(startInclusive, endExclusive);
        }
    }
}
