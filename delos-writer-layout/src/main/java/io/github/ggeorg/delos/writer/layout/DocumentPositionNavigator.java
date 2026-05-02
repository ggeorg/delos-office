package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.document.TextPosition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Document-wide caret navigation across the current layout tree.
 */
public final class DocumentPositionNavigator {
    private final CaretLocator locator = new CaretLocator();

    public TextPosition firstPosition(LaidOutDocument document) {
        if (document == null) {
            return null;
        }
        for (LaidOutPage page : document.pages()) {
            for (LaidOutBlock rawBlock : page.blocks()) {
                if (rawBlock instanceof LaidOutTextBlock block && block.role() == BlockRole.BODY) {
                    if (block.lines().isEmpty()) {
                        return new TextPosition(block.sourceParagraphIndex(), 0);
                    }
                    return new TextPosition(block.sourceParagraphIndex(), block.lines().getFirst().startOffset());
                }
            }
        }
        return new TextPosition(0, 0);
    }

    public TextPosition moveToDocumentStart(LaidOutDocument document) {
        return firstPosition(document);
    }

    public TextPosition moveToDocumentEnd(LaidOutDocument document) {
        if (document == null) {
            return null;
        }

        TextPosition best = null;
        for (LaidOutPage page : document.pages()) {
            for (LaidOutBlock rawBlock : page.blocks()) {
                if (!(rawBlock instanceof LaidOutTextBlock block) || block.role() != BlockRole.BODY) {
                    continue;
                }
                for (LaidOutLine line : block.lines()) {
                    TextPosition candidate = new TextPosition(block.sourceParagraphIndex(), line.endOffset());
                    if (best == null || candidate.compareTo(best) > 0) {
                        best = candidate;
                    }
                }
            }
        }
        return best == null ? new TextPosition(0, 0) : best;
    }

    public TextPosition moveLeft(LaidOutDocument document, TextPosition current) {
        if (current == null) {
            return firstPosition(document);
        }
        if (current.offset() > 0) {
            return new TextPosition(current.paragraphIndex(), current.offset() - 1);
        }
        if (current.paragraphIndex() <= 0) {
            return new TextPosition(0, 0);
        }

        Integer previousEnd = paragraphEndOffset(document, current.paragraphIndex() - 1);
        return new TextPosition(current.paragraphIndex() - 1, previousEnd == null ? 0 : previousEnd);
    }

    public TextPosition moveRight(LaidOutDocument document, TextPosition current) {
        if (current == null) {
            return firstPosition(document);
        }

        Integer paragraphEnd = paragraphEndOffset(document, current.paragraphIndex());
        if (paragraphEnd == null) {
            return current;
        }
        if (current.offset() < paragraphEnd) {
            return new TextPosition(current.paragraphIndex(), current.offset() + 1);
        }

        Integer nextEnd = paragraphEndOffset(document, current.paragraphIndex() + 1);
        if (nextEnd == null) {
            return current;
        }
        return new TextPosition(current.paragraphIndex() + 1, 0);
    }

    /**
     * Move to the start of the previous word, using the laid-out paragraph text
     * as the source of truth. Works for Unicode letters/digits, including Greek.
     */
    public TextPosition moveWordLeft(LaidOutDocument document, TextPosition current) {
        if (current == null) {
            return firstPosition(document);
        }

        ParagraphText paragraph = paragraphText(document, current.paragraphIndex());
        if (paragraph == null) {
            return moveLeft(document, current);
        }

        int index = Math.min(Math.max(current.offset(), 0), paragraph.length());
        if (index == 0) {
            return moveLeft(document, current);
        }

        String text = paragraph.text();
        while (index > 0 && !isWordCharacter(text.charAt(index - 1))) {
            index--;
        }
        while (index > 0 && isWordCharacter(text.charAt(index - 1))) {
            index--;
        }
        return new TextPosition(current.paragraphIndex(), index);
    }

    /**
     * Move to the start of the next word, preserving the usual editor behavior:
     * from inside a word, skip the current word and following separators.
     */
    public TextPosition moveWordRight(LaidOutDocument document, TextPosition current) {
        if (current == null) {
            return firstPosition(document);
        }

        ParagraphText paragraph = paragraphText(document, current.paragraphIndex());
        if (paragraph == null) {
            return moveRight(document, current);
        }

        String text = paragraph.text();
        int index = Math.min(Math.max(current.offset(), 0), paragraph.length());
        while (index < text.length() && isWordCharacter(text.charAt(index))) {
            index++;
        }
        while (index < text.length() && !isWordCharacter(text.charAt(index))) {
            index++;
        }

        if (index < text.length()) {
            return new TextPosition(current.paragraphIndex(), index);
        }

        Integer nextEnd = paragraphEndOffset(document, current.paragraphIndex() + 1);
        if (nextEnd == null) {
            return new TextPosition(current.paragraphIndex(), paragraph.length());
        }
        return new TextPosition(current.paragraphIndex() + 1, 0);
    }

    public SelectionRange wordRangeAt(LaidOutDocument document, TextPosition position) {
        if (position == null) {
            TextPosition first = firstPosition(document);
            return first == null ? null : new SelectionRange(first, first);
        }

        ParagraphText paragraph = paragraphText(document, position.paragraphIndex());
        if (paragraph == null || paragraph.text().isEmpty()) {
            return new SelectionRange(position, position);
        }

        String text = paragraph.text();
        int probe = Math.min(Math.max(position.offset(), 0), text.length());
        if (probe == text.length() || !isWordCharacter(text.charAt(probe))) {
            if (probe > 0 && isWordCharacter(text.charAt(probe - 1))) {
                probe--;
            } else {
                return new SelectionRange(position, position);
            }
        }

        int start = probe;
        int end = probe + 1;
        while (start > 0 && isWordCharacter(text.charAt(start - 1))) {
            start--;
        }
        while (end < text.length() && isWordCharacter(text.charAt(end))) {
            end++;
        }

        return new SelectionRange(
                new TextPosition(position.paragraphIndex(), start),
                new TextPosition(position.paragraphIndex(), end)
        );
    }

    public SelectionRange paragraphRangeAt(LaidOutDocument document, TextPosition position) {
        if (position == null) {
            TextPosition first = firstPosition(document);
            return first == null ? null : new SelectionRange(first, first);
        }
        Integer endOffset = paragraphEndOffset(document, position.paragraphIndex());
        int end = endOffset == null ? position.offset() : endOffset;
        return new SelectionRange(
                new TextPosition(position.paragraphIndex(), 0),
                new TextPosition(position.paragraphIndex(), end)
        );
    }

    public TextPosition moveToLineStart(LaidOutDocument document, TextPosition current) {
        ResolvedTextPosition resolved = locator.resolve(document, current);
        if (resolved == null) {
            return firstPosition(document);
        }
        return new TextPosition(resolved.block().sourceParagraphIndex(), resolved.line().startOffset());
    }

    public TextPosition moveToLineEnd(LaidOutDocument document, TextPosition current) {
        ResolvedTextPosition resolved = locator.resolve(document, current);
        if (resolved == null) {
            return moveToDocumentEnd(document);
        }
        return new TextPosition(resolved.block().sourceParagraphIndex(), resolved.line().endOffset());
    }

    public TextPosition moveVertical(LaidOutDocument document, TextPosition current, int direction, double preferredPageX) {
        ResolvedTextPosition resolved = locator.resolve(document, current);
        if (resolved == null) {
            return firstPosition(document);
        }

        List<LineAddress> lines = allLines(document);
        int lineSeq = findLineIndex(lines, resolved.pageIndex(), resolved.blockIndex(), resolved.lineIndex());
        if (lineSeq < 0) {
            return current;
        }

        int targetIndex = Math.max(0, Math.min(lineSeq + direction, lines.size() - 1));
        LineAddress target = lines.get(targetIndex);
        double targetLocalX = preferredPageX - target.block().x() - target.line().x();
        int column = target.line().nearestColumn(targetLocalX);
        return new TextPosition(target.block().sourceParagraphIndex(), target.line().offsetForColumn(column));
    }

    public TextPosition movePage(LaidOutDocument document, TextPosition current, int direction, double preferredPageX) {
        ResolvedTextPosition resolved = locator.resolve(document, current);
        if (resolved == null) {
            return firstPosition(document);
        }

        int targetPageIndex = Math.max(0, Math.min(resolved.pageIndex() + direction, document.pages().size() - 1));
        if (targetPageIndex == resolved.pageIndex()) {
            return current;
        }

        List<LineAddress> targetPageLines = pageLines(document, targetPageIndex);
        if (targetPageLines.isEmpty()) {
            return current;
        }

        double currentCenterY = resolved.block().y() + resolved.line().y() + resolved.line().height() / 2.0;
        LineAddress best = targetPageLines.getFirst();
        double bestDistance = Double.MAX_VALUE;
        for (LineAddress candidate : targetPageLines) {
            double candidateCenterY = candidate.block().y() + candidate.line().y() + candidate.line().height() / 2.0;
            double distance = Math.abs(candidateCenterY - currentCenterY);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }

        double targetLocalX = preferredPageX - best.block().x() - best.line().x();
        int column = best.line().nearestColumn(targetLocalX);
        return new TextPosition(best.block().sourceParagraphIndex(), best.line().offsetForColumn(column));
    }

    private Integer paragraphEndOffset(LaidOutDocument document, int paragraphIndex) {
        if (document == null || paragraphIndex < 0) {
            return null;
        }

        Integer best = null;
        for (LaidOutPage page : document.pages()) {
            for (LaidOutBlock rawBlock : page.blocks()) {
                if (!(rawBlock instanceof LaidOutTextBlock block) || block.role() != BlockRole.BODY) {
                    continue;
                }
                if (block.sourceParagraphIndex() != paragraphIndex) {
                    continue;
                }
                for (LaidOutLine line : block.lines()) {
                    if (best == null || line.endOffset() > best) {
                        best = line.endOffset();
                    }
                }
            }
        }
        return best;
    }

    private ParagraphText paragraphText(LaidOutDocument document, int paragraphIndex) {
        if (document == null || paragraphIndex < 0) {
            return null;
        }

        List<LineTextSegment> segments = new ArrayList<>();
        int endOffset = -1;
        for (LaidOutPage page : document.pages()) {
            for (LaidOutBlock rawBlock : page.blocks()) {
                if (!(rawBlock instanceof LaidOutTextBlock block) || block.role() != BlockRole.BODY) {
                    continue;
                }
                if (block.sourceParagraphIndex() != paragraphIndex) {
                    continue;
                }
                for (LaidOutLine line : block.lines()) {
                    segments.add(new LineTextSegment(line.startOffset(), line.endOffset(), line.sourceText()));
                    endOffset = Math.max(endOffset, line.endOffset());
                }
            }
        }
        if (endOffset < 0) {
            return null;
        }

        char[] chars = new char[endOffset];
        for (int index = 0; index < chars.length; index++) {
            chars[index] = ' ';
        }
        segments.sort(Comparator.comparingInt(LineTextSegment::startOffset));
        for (LineTextSegment segment : segments) {
            int available = Math.min(segment.text().length(), Math.max(0, segment.endOffset() - segment.startOffset()));
            for (int index = 0; index < available; index++) {
                int target = segment.startOffset() + index;
                if (target >= 0 && target < chars.length) {
                    chars[target] = segment.text().charAt(index);
                }
            }
        }
        return new ParagraphText(new String(chars));
    }

    private static boolean isWordCharacter(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_';
    }

    private List<LineAddress> pageLines(LaidOutDocument document, int pageIndex) {
        List<LineAddress> result = new ArrayList<>();
        if (document == null || pageIndex < 0 || pageIndex >= document.pages().size()) {
            return result;
        }

        LaidOutPage page = document.pages().get(pageIndex);
        for (int blockIndex = 0; blockIndex < page.blocks().size(); blockIndex++) {
            if (!(page.blocks().get(blockIndex) instanceof LaidOutTextBlock block)) {
                continue;
            }
            if (block.role() != BlockRole.BODY) {
                continue;
            }
            for (int lineIndex = 0; lineIndex < block.lines().size(); lineIndex++) {
                result.add(new LineAddress(page.pageIndex(), blockIndex, block, lineIndex, block.lines().get(lineIndex)));
            }
        }
        return result;
    }

    private List<LineAddress> allLines(LaidOutDocument document) {
        List<LineAddress> result = new ArrayList<>();
        for (LaidOutPage page : document.pages()) {
            for (int blockIndex = 0; blockIndex < page.blocks().size(); blockIndex++) {
                if (!(page.blocks().get(blockIndex) instanceof LaidOutTextBlock block)) {
                    continue;
                }
                if (block.role() != BlockRole.BODY) {
                    continue;
                }
                for (int lineIndex = 0; lineIndex < block.lines().size(); lineIndex++) {
                    result.add(new LineAddress(page.pageIndex(), blockIndex, block, lineIndex, block.lines().get(lineIndex)));
                }
            }
        }
        return result;
    }

    private int findLineIndex(List<LineAddress> lines, int pageIndex, int blockIndex, int lineIndex) {
        for (int i = 0; i < lines.size(); i++) {
            LineAddress line = lines.get(i);
            if (line.pageIndex() == pageIndex && line.blockIndex() == blockIndex && line.lineIndex() == lineIndex) {
                return i;
            }
        }
        return -1;
    }

    private record LineAddress(
            int pageIndex,
            int blockIndex,
            LaidOutTextBlock block,
            int lineIndex,
            LaidOutLine line
    ) {
    }

    private record LineTextSegment(int startOffset, int endOffset, String text) {
    }

    private record ParagraphText(String text) {
        int length() {
            return text.length();
        }
    }
}
