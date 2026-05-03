package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.CaretPosition;
import io.github.ggeorg.delos.writer.document.TableCellStoryPath;
import io.github.ggeorg.delos.writer.document.TextPosition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves logical positions against the current layout tree.
 */
public final class CaretLocator {
    private LaidOutDocument indexedDocument;
    private ParagraphBlockIndex indexedLayout;

    public ResolvedTextPosition resolve(LaidOutDocument document, CaretPosition position) {
        if (position == null) {
            return null;
        }
        if (position.isBodyStory()) {
            return resolve(document, position.toLegacyBodyTextPosition());
        }
        if (position.storyPath() instanceof TableCellStoryPath path) {
            return resolveTableCellCaret(document, path, position);
        }
        return null;
    }

    public ResolvedTextPosition resolve(LaidOutDocument document, TextPosition position) {
        if (document == null || position == null) {
            return null;
        }

        ResolvedTextPosition nearestInParagraph = null;
        int nearestDistance = Integer.MAX_VALUE;

        for (BlockLocation location : indexFor(document).locationsFor(position.paragraphIndex())) {
            LaidOutTextBlock block = location.block();
            for (int lineIndex = 0; lineIndex < block.lines().size(); lineIndex++) {
                LaidOutLine line = block.lines().get(lineIndex);
                if (containsCaretPosition(line, position.offset())) {
                    int column = line.columnForOffset(position.offset());
                    return new ResolvedTextPosition(
                            location.page(),
                            location.pageIndex(),
                            block,
                            location.blockIndex(),
                            line,
                            lineIndex,
                            column,
                            position
                    );
                }

                int candidateColumn = position.offset() < line.startOffset() ? 0 : line.length();
                int candidateOffset = line.offsetForColumn(candidateColumn);
                int distance = Math.abs(candidateOffset - position.offset());
                if (nearestInParagraph == null || distance < nearestDistance) {
                    TextPosition clamped = new TextPosition(position.paragraphIndex(), candidateOffset);
                    nearestInParagraph = new ResolvedTextPosition(
                            location.page(),
                            location.pageIndex(),
                            block,
                            location.blockIndex(),
                            line,
                            lineIndex,
                            candidateColumn,
                            clamped
                    );
                    nearestDistance = distance;
                }
            }
        }

        // Do not resolve a caret against a different paragraph. During Enter/backspace
        // the paragraph list changes shape; returning a line from another paragraph
        // makes the renderer and scroll coordinator believe the active row/page is
        // somewhere else. Invalid positions are clamped by the caller.
        return nearestInParagraph;
    }

    private ResolvedTextPosition resolveTableCellCaret(LaidOutDocument document, TableCellStoryPath path, CaretPosition position) {
        if (document == null) {
            return null;
        }
        for (LaidOutPage page : document.pages()) {
            for (int pageBlockIndex = 0; pageBlockIndex < page.blocks().size(); pageBlockIndex++) {
                if (!(page.blocks().get(pageBlockIndex) instanceof LaidOutTableBlock tableBlock)
                        || tableBlock.sourceBlockIndex() != path.tableBlockIndex()
                        || path.rowIndex() >= tableBlock.rows().size()) {
                    continue;
                }
                LaidOutTableRow row = tableBlock.rows().get(path.rowIndex());
                if (path.columnIndex() >= row.cells().size()) {
                    continue;
                }
                LaidOutTableCell cell = row.cells().get(path.columnIndex());
                ResolvedTextPosition resolved = resolveWithinCell(page, pageBlockIndex, tableBlock, cell, position);
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        return null;
    }

    private ResolvedTextPosition resolveWithinCell(
            LaidOutPage page,
            int pageBlockIndex,
            LaidOutTableBlock tableBlock,
            LaidOutTableCell cell,
            CaretPosition position
    ) {
        ResolvedTextPosition nearestInParagraph = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (LaidOutTextBlock textBlock : cell.textBlocks()) {
            if (textBlock.sourceParagraphIndex() != position.storyBlockIndex()) {
                continue;
            }
            LaidOutTextBlock absoluteBlock = absoluteCellTextBlock(tableBlock, cell, textBlock);
            for (int lineIndex = 0; lineIndex < absoluteBlock.lines().size(); lineIndex++) {
                LaidOutLine line = absoluteBlock.lines().get(lineIndex);
                if (containsCaretPosition(line, position.offset())) {
                    int column = line.columnForOffset(position.offset());
                    return new ResolvedTextPosition(
                            page,
                            page.pageIndex(),
                            absoluteBlock,
                            pageBlockIndex,
                            line,
                            lineIndex,
                            column,
                            new TextPosition(position.storyBlockIndex(), position.offset())
                    );
                }
                int candidateColumn = position.offset() < line.startOffset() ? 0 : line.length();
                int candidateOffset = line.offsetForColumn(candidateColumn);
                int distance = Math.abs(candidateOffset - position.offset());
                if (nearestInParagraph == null || distance < nearestDistance) {
                    nearestInParagraph = new ResolvedTextPosition(
                            page,
                            page.pageIndex(),
                            absoluteBlock,
                            pageBlockIndex,
                            line,
                            lineIndex,
                            candidateColumn,
                            new TextPosition(position.storyBlockIndex(), candidateOffset)
                    );
                    nearestDistance = distance;
                }
            }
        }
        return nearestInParagraph;
    }

    private LaidOutTextBlock absoluteCellTextBlock(LaidOutTableBlock tableBlock, LaidOutTableCell cell, LaidOutTextBlock textBlock) {
        return new LaidOutTextBlock(
                textBlock.role(),
                tableBlock.x() + cell.x() + textBlock.x(),
                tableBlock.y() + cell.y() + textBlock.y(),
                textBlock.width(),
                textBlock.height(),
                textBlock.sourceParagraphIndex(),
                textBlock.startLineIndex(),
                textBlock.firstFragment(),
                textBlock.lastFragment(),
                textBlock.lines(),
                textBlock.listMarker()
        );
    }

    private ParagraphBlockIndex indexFor(LaidOutDocument document) {
        if (indexedDocument != document || indexedLayout == null) {
            indexedDocument = document;
            indexedLayout = ParagraphBlockIndex.from(document);
        }
        return indexedLayout;
    }

    private boolean containsCaretPosition(LaidOutLine line, int offset) {
        if (line.startOffset() == line.endOffset()) {
            return offset == line.startOffset();
        }
        return offset >= line.startOffset() && offset < line.endOffset();
    }

    public CaretGeometry locateCaret(LaidOutDocument document, CaretPosition position) {
        return locateCaret(resolve(document, position));
    }

    public CaretGeometry locateCaret(LaidOutDocument document, TextPosition position) {
        return locateCaret(resolve(document, position));
    }

    public CaretGeometry locateCaret(ResolvedTextPosition resolved) {
        if (resolved == null) {
            return null;
        }

        double caretX = resolved.block().x() + resolved.line().caretXForColumn(resolved.columnIndex());
        double caretY = resolved.block().y() + resolved.line().y();
        return new CaretGeometry(caretX, caretY, resolved.line().height());
    }

    private record ParagraphBlockIndex(Map<Integer, List<BlockLocation>> locationsByParagraph) {
        private ParagraphBlockIndex {
            locationsByParagraph = Map.copyOf(Objects.requireNonNull(locationsByParagraph, "locationsByParagraph"));
        }

        static ParagraphBlockIndex from(LaidOutDocument document) {
            Map<Integer, List<BlockLocation>> locations = new HashMap<>();
            for (LaidOutPage page : document.pages()) {
                for (int blockIndex = 0; blockIndex < page.blocks().size(); blockIndex++) {
                    if (!(page.blocks().get(blockIndex) instanceof LaidOutTextBlock block)) {
                        continue;
                    }
                    if (block.role() != BlockRole.BODY || block.sourceParagraphIndex() < 0) {
                        continue;
                    }
                    locations.computeIfAbsent(block.sourceParagraphIndex(), unused -> new ArrayList<>())
                            .add(new BlockLocation(page, page.pageIndex(), block, blockIndex));
                }
            }

            Map<Integer, List<BlockLocation>> immutable = new HashMap<>();
            for (Map.Entry<Integer, List<BlockLocation>> entry : locations.entrySet()) {
                immutable.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            return new ParagraphBlockIndex(immutable);
        }

        List<BlockLocation> locationsFor(int paragraphIndex) {
            return locationsByParagraph.getOrDefault(paragraphIndex, List.of());
        }
    }

    private record BlockLocation(
            LaidOutPage page,
            int pageIndex,
            LaidOutTextBlock block,
            int blockIndex
    ) {
    }
}
