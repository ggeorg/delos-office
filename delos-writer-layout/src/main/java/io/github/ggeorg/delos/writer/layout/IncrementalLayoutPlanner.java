package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.Block;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;

import java.util.List;
import java.util.Objects;

/**
 * Computes conservative reuse boundaries for incremental document pagination.
 *
 * <p>This class intentionally knows nothing about text measurement. It only
 * decides whether an existing laid-out prefix is safe to reuse and where the
 * following cold-ish suffix relayout should start.</p>
 */
final class IncrementalLayoutPlanner {
    boolean canReuseIncrementallyWithBlocks(List<Block> previousBlocks, List<Block> currentBlocks) {
        if (!containsNonParagraphBlock(previousBlocks) && !containsNonParagraphBlock(currentBlocks)) {
            return true;
        }
        if (previousBlocks.size() != currentBlocks.size()) {
            return false;
        }
        for (int i = 0; i < previousBlocks.size(); i++) {
            Block previous = previousBlocks.get(i);
            Block current = currentBlocks.get(i);
            if (previous instanceof ParagraphBlock && current instanceof ParagraphBlock) {
                continue;
            }
            if (!Objects.equals(previous, current)) {
                return false;
            }
        }
        return true;
    }

    IncrementalLayoutPlan plan(LayoutCache reusableCache, LayoutInputs currentInputs, List<Block> currentBlocks) {
        if (reusableCache == null) {
            return IncrementalLayoutPlan.cold();
        }

        int firstChangedParagraph = firstChangedParagraphIndex(reusableCache.paragraphs(), currentInputs.paragraphs());
        int relayoutAnchorParagraph = relayoutAnchorParagraph(
            reusableCache.paragraphs(),
            currentInputs.paragraphs(),
            firstChangedParagraph
        );
        int reusablePrefixPageCount = reusablePrefixPageCount(reusableCache.document(), relayoutAnchorParagraph);
        int relayoutStartParagraph = relayoutStartParagraphIndex(
            reusableCache.document(),
            reusablePrefixPageCount,
            relayoutAnchorParagraph
        );
        int relayoutStartBlockIndex = relayoutStartBlockIndex(
            reusableCache.document(),
            reusablePrefixPageCount,
            currentBlocks,
            relayoutStartParagraph
        );

        return new IncrementalLayoutPlan(
            firstChangedParagraph,
            relayoutAnchorParagraph,
            reusablePrefixPageCount,
            relayoutStartParagraph,
            relayoutStartBlockIndex
        );
    }

    private boolean containsNonParagraphBlock(List<Block> blocks) {
        for (Block block : blocks) {
            if (!(block instanceof ParagraphBlock)) {
                return true;
            }
        }
        return false;
    }

    private int firstChangedParagraphIndex(List<Paragraph> previousParagraphs, List<Paragraph> currentParagraphs) {
        int limit = Math.min(previousParagraphs.size(), currentParagraphs.size());
        for (int i = 0; i < limit; i++) {
            if (!Objects.equals(previousParagraphs.get(i), currentParagraphs.get(i))) {
                return i;
            }
        }
        return limit;
    }

    private int relayoutAnchorParagraph(List<Paragraph> previousParagraphs, List<Paragraph> currentParagraphs, int firstChangedParagraph) {
        if (previousParagraphs.equals(currentParagraphs)) {
            return firstChangedParagraph;
        }
        if (firstChangedParagraph <= 0) {
            return 0;
        }
        return firstChangedParagraph - 1;
    }

    private int reusablePrefixPageCount(LaidOutDocument previousDocument, int relayoutAnchorParagraph) {
        if (previousDocument == null || relayoutAnchorParagraph <= 0) {
            return 0;
        }

        int reusableCount = 0;
        for (LaidOutPage page : previousDocument.pages()) {
            int maxParagraphIndex = maxBodyParagraphIndex(page);
            if (maxParagraphIndex >= 0 && maxParagraphIndex < relayoutAnchorParagraph) {
                reusableCount++;
                continue;
            }
            if (maxParagraphIndex < 0 && reusableCount == 0) {
                reusableCount++;
                continue;
            }
            break;
        }
        return reusableCount;
    }

    private int relayoutStartParagraphIndex(LaidOutDocument previousDocument, int reusablePrefixPageCount, int relayoutAnchorParagraph) {
        if (previousDocument == null || reusablePrefixPageCount <= 0) {
            return 0;
        }
        if (reusablePrefixPageCount >= previousDocument.pages().size()) {
            return relayoutAnchorParagraph;
        }

        LaidOutPage firstDirtyPage = previousDocument.pages().get(reusablePrefixPageCount);
        int minParagraphIndex = minBodyParagraphIndex(firstDirtyPage);
        return minParagraphIndex >= 0 ? Math.min(minParagraphIndex, relayoutAnchorParagraph) : relayoutAnchorParagraph;
    }

    private int relayoutStartBlockIndex(
        LaidOutDocument previousDocument,
        int reusablePrefixPageCount,
        List<Block> currentBlocks,
        int relayoutStartParagraph
    ) {
        int paragraphBlockIndex = blockIndexForParagraphIndex(currentBlocks, relayoutStartParagraph);
        if (previousDocument == null || reusablePrefixPageCount < 0 || reusablePrefixPageCount >= previousDocument.pages().size()) {
            return paragraphBlockIndex;
        }

        int[] paragraphBlockIndexes = paragraphBlockIndexes(currentBlocks);
        int minSourceBlockIndex = Integer.MAX_VALUE;
        LaidOutPage firstDirtyPage = previousDocument.pages().get(reusablePrefixPageCount);
        for (LaidOutBlock block : firstDirtyPage.blocks()) {
            if (block instanceof LaidOutTextBlock textBlock
                    && textBlock.role() == BlockRole.BODY
                    && textBlock.sourceParagraphIndex() >= 0
                    && textBlock.sourceParagraphIndex() < paragraphBlockIndexes.length) {
                minSourceBlockIndex = Math.min(minSourceBlockIndex, paragraphBlockIndexes[textBlock.sourceParagraphIndex()]);
            } else if (block instanceof LaidOutAtomicBlock atomicBlock && atomicBlock.sourceBlockIndex() >= 0) {
                minSourceBlockIndex = Math.min(minSourceBlockIndex, atomicBlock.sourceBlockIndex());
            }
        }

        if (minSourceBlockIndex == Integer.MAX_VALUE) {
            return paragraphBlockIndex;
        }
        return Math.min(minSourceBlockIndex, paragraphBlockIndex);
    }

    private int blockIndexForParagraphIndex(List<Block> blocks, int targetParagraphIndex) {
        if (targetParagraphIndex <= 0) {
            return 0;
        }
        int paragraphIndex = 0;
        for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
            if (blocks.get(blockIndex) instanceof ParagraphBlock) {
                if (paragraphIndex == targetParagraphIndex) {
                    return blockIndex;
                }
                paragraphIndex += 1;
            }
        }
        return blocks.size();
    }

    private int[] paragraphBlockIndexes(List<Block> blocks) {
        int paragraphCount = 0;
        for (Block block : blocks) {
            if (block instanceof ParagraphBlock) {
                paragraphCount += 1;
            }
        }

        int[] indexes = new int[paragraphCount];
        int paragraphIndex = 0;
        for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
            if (blocks.get(blockIndex) instanceof ParagraphBlock) {
                indexes[paragraphIndex] = blockIndex;
                paragraphIndex += 1;
            }
        }
        return indexes;
    }

    private int minBodyParagraphIndex(LaidOutPage page) {
        int min = Integer.MAX_VALUE;
        for (LaidOutBlock block : page.blocks()) {
            if (block instanceof LaidOutTextBlock textBlock && textBlock.role() == BlockRole.BODY && textBlock.sourceParagraphIndex() >= 0) {
                min = Math.min(min, textBlock.sourceParagraphIndex());
            }
        }
        return min == Integer.MAX_VALUE ? -1 : min;
    }

    private int maxBodyParagraphIndex(LaidOutPage page) {
        int max = -1;
        for (LaidOutBlock block : page.blocks()) {
            if (block instanceof LaidOutTextBlock textBlock && textBlock.role() == BlockRole.BODY) {
                max = Math.max(max, textBlock.sourceParagraphIndex());
            }
        }
        return max;
    }
}
