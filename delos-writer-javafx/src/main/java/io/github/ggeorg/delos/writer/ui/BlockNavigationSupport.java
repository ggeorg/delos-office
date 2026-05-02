package io.github.ggeorg.delos.writer.ui;

import io.github.ggeorg.delos.writer.document.Block;
import io.github.ggeorg.delos.writer.document.BlockSelection;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.TextPosition;

import java.util.List;

/** Utility methods for moving between paragraph caret space and rich block space. */
final class BlockNavigationSupport {
    private BlockNavigationSupport() {
    }

    static boolean isRichBlock(Document document, BlockSelection selection) {
        if (document == null || selection == null) {
            return false;
        }
        int index = selection.blockIndex();
        return index >= 0 && index < document.blocks().size()
                && !(document.blocks().get(index) instanceof ParagraphBlock);
    }

    static BlockSelection richBlockAfterCaret(Document document, TextPosition caret) {
        if (document == null || caret == null || caret.offset() != paragraphLength(document, caret.paragraphIndex())) {
            return null;
        }
        int blockIndex = blockIndexForParagraph(document, caret.paragraphIndex());
        if (blockIndex < 0) {
            return null;
        }
        for (int index = blockIndex + 1; index < document.blocks().size(); index++) {
            if (document.blocks().get(index) instanceof ParagraphBlock) {
                return null;
            }
            return new BlockSelection(index);
        }
        return null;
    }

    static BlockSelection richBlockBeforeCaret(Document document, TextPosition caret) {
        if (document == null || caret == null || caret.offset() != 0) {
            return null;
        }
        int blockIndex = blockIndexForParagraph(document, caret.paragraphIndex());
        if (blockIndex < 0) {
            return null;
        }
        for (int index = blockIndex - 1; index >= 0; index--) {
            if (document.blocks().get(index) instanceof ParagraphBlock) {
                return null;
            }
            return new BlockSelection(index);
        }
        return null;
    }

    static TextPosition textPositionBeforeBlock(Document document, BlockSelection selection) {
        if (document == null || selection == null) {
            return null;
        }
        int paragraphIndex = 0;
        TextPosition best = null;
        for (int index = 0; index < Math.min(selection.blockIndex(), document.blocks().size()); index++) {
            Block block = document.blocks().get(index);
            if (block instanceof ParagraphBlock paragraphBlock) {
                best = new TextPosition(paragraphIndex, paragraphBlock.paragraph().length());
                paragraphIndex += 1;
            }
        }
        return best == null ? new TextPosition(0, 0) : best;
    }

    static TextPosition textPositionAfterBlock(Document document, BlockSelection selection) {
        if (document == null || selection == null) {
            return null;
        }
        int paragraphIndex = 0;
        for (int index = 0; index < document.blocks().size(); index++) {
            Block block = document.blocks().get(index);
            if (block instanceof ParagraphBlock) {
                if (index > selection.blockIndex()) {
                    return new TextPosition(paragraphIndex, 0);
                }
                paragraphIndex += 1;
            }
        }
        TextPosition before = textPositionBeforeBlock(document, selection);
        return before == null ? new TextPosition(0, 0) : before;
    }

    static BlockSelection selectFromHit(Document document, BlockSelection selection) {
        return isRichBlock(document, selection) ? selection : null;
    }

    private static int blockIndexForParagraph(Document document, int targetParagraphIndex) {
        int paragraphIndex = 0;
        List<Block> blocks = document.blocks();
        for (int index = 0; index < blocks.size(); index++) {
            if (blocks.get(index) instanceof ParagraphBlock) {
                if (paragraphIndex == targetParagraphIndex) {
                    return index;
                }
                paragraphIndex += 1;
            }
        }
        return -1;
    }

    private static int paragraphLength(Document document, int targetParagraphIndex) {
        if (targetParagraphIndex < 0 || targetParagraphIndex >= document.paragraphs().size()) {
            return -1;
        }
        return document.paragraphs().get(targetParagraphIndex).length();
    }
}
