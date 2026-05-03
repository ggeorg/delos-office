package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.Block;
import io.github.ggeorg.delos.writer.document.ListMarkerKind;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;

import java.util.List;

/**
 * Mutable list-numbering state used during pagination.
 *
 * <p>The paginator may reuse pages before an incremental relayout anchor. This
 * class owns the counter replay needed to resume numbered-list markers exactly
 * where the reused prefix left off.</p>
 */
final class ListLayoutState {
    static final double TEXT_INDENT = 28.0;

    private static final double LEVEL_INDENT = 24.0;
    private static final int MAX_LEVELS = 9;
    private static final String BULLET_MARKER = "•";

    private final int[] numberedCounters;

    ListLayoutState() {
        this(MAX_LEVELS);
    }

    ListLayoutState(int maxLevels) {
        if (maxLevels < 1) {
            throw new IllegalArgumentException("maxLevels must be >= 1");
        }
        this.numberedCounters = new int[maxLevels];
    }

    void replayBeforeParagraph(List<Block> blocks, int paragraphLimitExclusive) {
        if (paragraphLimitExclusive <= 0) {
            return;
        }

        int paragraphIndex = 0;
        for (Block block : blocks) {
            if (block instanceof ParagraphBlock paragraphBlock) {
                if (paragraphIndex >= paragraphLimitExclusive) {
                    return;
                }
                advance(paragraphBlock.paragraph());
                paragraphIndex += 1;
            } else {
                reset();
            }
        }
    }

    ListLayout layoutFor(PageStyle pageStyle, Paragraph paragraph) {
        if (!paragraph.style().isListItem()) {
            reset();
            return ListLayout.none(pageStyle);
        }

        int level = normalizedLevel(paragraph);
        double markerIndent = level * LEVEL_INDENT;
        double bodyIndent = markerIndent + TEXT_INDENT;
        String markerText;
        if (paragraph.style().listStyle().kind() == ListMarkerKind.NUMBERED) {
            if (numberedCounters[level] <= 0) {
                numberedCounters[level] = paragraph.style().listStyle().start();
            }
            markerText = numberedCounters[level]++ + ".";
        } else {
            markerText = BULLET_MARKER;
        }
        resetChildren(level);

        double contentWidth = Math.max(1, pageStyle.contentWidth() - bodyIndent);
        return new ListLayout(true, markerText, pageStyle.marginLeft() + bodyIndent, contentWidth, -TEXT_INDENT);
    }

    void reset() {
        for (int i = 0; i < numberedCounters.length; i++) {
            numberedCounters[i] = 0;
        }
    }

    private void advance(Paragraph paragraph) {
        if (!paragraph.style().isListItem()) {
            reset();
            return;
        }

        int level = normalizedLevel(paragraph);
        if (paragraph.style().listStyle().kind() == ListMarkerKind.NUMBERED) {
            if (numberedCounters[level] <= 0) {
                numberedCounters[level] = paragraph.style().listStyle().start();
            }
            numberedCounters[level] += 1;
        }
        resetChildren(level);
    }

    private int normalizedLevel(Paragraph paragraph) {
        return Math.min(paragraph.style().listStyle().level(), numberedCounters.length - 1);
    }

    private void resetChildren(int level) {
        for (int i = level + 1; i < numberedCounters.length; i++) {
            numberedCounters[i] = 0;
        }
    }
}
