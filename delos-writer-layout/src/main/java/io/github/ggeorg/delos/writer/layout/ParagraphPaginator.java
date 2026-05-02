package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;

import java.util.List;
import java.util.Objects;

/**
 * Paginates a laid-out paragraph into page-sized text fragments.
 *
 * <p>This class is intentionally behavior-preserving. It owns only the paragraph
 * flow rules that used to live in {@link PaginatingDocumentLayoutEngine}: spacing
 * before/after, fragment selection, widow/orphan control, short-paragraph
 * keep-together handling, list marker placement, and page continuation.</p>
 */
final class ParagraphPaginator {
    private final PaginatingDocumentLayoutEngine.PaginationPolicy paginationPolicy;

    ParagraphPaginator(PaginatingDocumentLayoutEngine.PaginationPolicy paginationPolicy) {
        this.paginationPolicy = Objects.requireNonNull(paginationPolicy, "paginationPolicy");
    }

    void appendParagraph(
        PageStyle pageStyle,
        List<Paragraph> paragraphs,
        List<LaidOutPage> pages,
        PageFlowState state,
        int paragraphIndex,
        Paragraph paragraph,
        List<LaidOutLine> lines,
        ListLayout listLayout
    ) {
        Objects.requireNonNull(pageStyle, "pageStyle");
        Objects.requireNonNull(paragraphs, "paragraphs");
        Objects.requireNonNull(pages, "pages");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(paragraph, "paragraph");
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(listLayout, "listLayout");

        double contentBottom = pageStyle.height() - pageStyle.marginBottom();
        ParagraphFragmentMetrics fragmentMetrics = new ParagraphFragmentMetrics(lines);
        int nextLineIndex = 0;
        boolean appliedSpacingBefore = false;

        while (nextLineIndex < fragmentMetrics.lineCount()) {
            if (!appliedSpacingBefore && paragraph.style().spacingBefore() > 0 && paragraphIndex > 0) {
                if (state.currentBlocks().isEmpty()) {
                    // Suppress paragraph spacing-before at the top of a page.
                    appliedSpacingBefore = true;
                } else if (state.cursorY() + paragraph.style().spacingBefore() <= contentBottom) {
                    state.cursorY(state.cursorY() + paragraph.style().spacingBefore());
                    appliedSpacingBefore = true;
                } else {
                    pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
                    state.advanceToNextPage(pageStyle);
                    continue;
                }
            }

            if (state.cursorY() >= contentBottom && !state.currentBlocks().isEmpty()) {
                pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
                state.advanceToNextPage(pageStyle);
                continue;
            }

            int fragmentEndExclusive = chooseFragmentEndExclusive(
                fragmentMetrics,
                nextLineIndex,
                state.cursorY(),
                contentBottom,
                pageStyle.contentHeight(),
                !state.currentBlocks().isEmpty()
            );
            if (fragmentEndExclusive <= nextLineIndex) {
                if (!state.currentBlocks().isEmpty()) {
                    pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
                    state.advanceToNextPage(pageStyle);
                    continue;
                }

                fragmentEndExclusive = Math.min(nextLineIndex + 1, fragmentMetrics.lineCount());
            }

            List<LaidOutLine> fragmentLines = fragmentMetrics.sliceAndNormalize(nextLineIndex, fragmentEndExclusive);
            double fragmentHeight = fragmentMetrics.height(nextLineIndex, fragmentEndExclusive);
            boolean firstFragment = nextLineIndex == 0;
            boolean lastFragment = fragmentEndExclusive == fragmentMetrics.lineCount();

            state.currentBlocks().add(new LaidOutTextBlock(
                BlockRole.BODY,
                listLayout.blockX(),
                state.cursorY(),
                listLayout.contentWidth(),
                fragmentHeight,
                paragraphIndex,
                nextLineIndex,
                firstFragment,
                lastFragment,
                fragmentLines,
                listMarkerFor(listLayout, fragmentLines, firstFragment, nextLineIndex)
            ));
            state.cursorY(state.cursorY() + fragmentHeight);
            nextLineIndex = fragmentEndExclusive;

            if (lastFragment) {
                double spacingAfter = paragraph.style().spacingAfter();
                if (paragraphIndex < paragraphs.size() - 1) {
                    if (state.cursorY() + spacingAfter <= contentBottom) {
                        state.cursorY(state.cursorY() + spacingAfter);
                    } else if (!state.currentBlocks().isEmpty()) {
                        pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
                        state.advanceToNextPage(pageStyle);
                    }
                }
            } else {
                pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
                state.advanceToNextPage(pageStyle);
            }
        }
    }

    private LaidOutListMarker listMarkerFor(ListLayout listLayout, List<LaidOutLine> fragmentLines, boolean firstFragment, int nextLineIndex) {
        if (!listLayout.enabled() || !firstFragment || nextLineIndex != 0 || fragmentLines.isEmpty()) {
            return LaidOutListMarker.none();
        }
        LaidOutLine firstLine = fragmentLines.get(0);
        double baselineY = firstLine.y() + firstLine.baseline();
        return new LaidOutListMarker(listLayout.markerText(), listLayout.markerX(), baselineY, ListLayoutState.TEXT_INDENT);
    }

    private int chooseFragmentEndExclusive(
        ParagraphFragmentMetrics fragmentMetrics,
        int startLineIndex,
        double cursorY,
        double contentBottom,
        double freshPageContentHeight,
        boolean canBreakBeforeFragment
    ) {
        double availableHeight = contentBottom - cursorY;
        int bestEndExclusive = fragmentMetrics.fittingEndExclusive(startLineIndex, availableHeight);

        int keepTogetherCandidate = keepShortParagraphTogetherWhenPossible(
            fragmentMetrics,
            startLineIndex,
            bestEndExclusive,
            freshPageContentHeight,
            canBreakBeforeFragment
        );
        if (keepTogetherCandidate != bestEndExclusive) {
            return keepTogetherCandidate;
        }

        return enforceWidowOrphanControl(fragmentMetrics.lineCount(), startLineIndex, bestEndExclusive, canBreakBeforeFragment);
    }

    private int keepShortParagraphTogetherWhenPossible(
        ParagraphFragmentMetrics fragmentMetrics,
        int startLineIndex,
        int bestEndExclusive,
        double freshPageContentHeight,
        boolean canBreakBeforeFragment
    ) {
        if (!canBreakBeforeFragment || startLineIndex != 0) {
            return bestEndExclusive;
        }

        int totalLineCount = fragmentMetrics.lineCount();
        if (totalLineCount <= 0 || totalLineCount > paginationPolicy.keepTogetherMaxLines()) {
            return bestEndExclusive;
        }

        if (bestEndExclusive >= totalLineCount) {
            return bestEndExclusive;
        }

        double fullParagraphHeight = fragmentMetrics.height(0, totalLineCount);
        if (fullParagraphHeight <= freshPageContentHeight) {
            return startLineIndex;
        }

        return bestEndExclusive;
    }

    private int enforceWidowOrphanControl(
        int totalLineCount,
        int startLineIndex,
        int bestEndExclusive,
        boolean canBreakBeforeFragment
    ) {
        if (bestEndExclusive <= startLineIndex || bestEndExclusive >= totalLineCount) {
            return bestEndExclusive;
        }

        int adjustedEndExclusive = bestEndExclusive;
        int remainingLineCount = totalLineCount - adjustedEndExclusive;
        if (remainingLineCount > 0 && remainingLineCount < paginationPolicy.minWidowLines()) {
            adjustedEndExclusive = Math.max(startLineIndex, totalLineCount - paginationPolicy.minWidowLines());
        }

        int placedLineCount = adjustedEndExclusive - startLineIndex;
        int adjustedRemainingLineCount = totalLineCount - adjustedEndExclusive;

        if (startLineIndex == 0 && adjustedRemainingLineCount > 0 && placedLineCount < paginationPolicy.minOrphanLines()) {
            return canBreakBeforeFragment ? startLineIndex : bestEndExclusive;
        }

        if (adjustedEndExclusive <= startLineIndex) {
            return canBreakBeforeFragment ? startLineIndex : bestEndExclusive;
        }

        return adjustedEndExclusive;
    }
}
