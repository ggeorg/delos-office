package io.github.ggeorg.delos.writer.editor;

import io.github.ggeorg.delos.writer.document.Block;
import io.github.ggeorg.delos.writer.document.CaretPosition;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.DocumentMediaItem;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.document.Story;
import io.github.ggeorg.delos.writer.document.StoryPath;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TableCell;
import io.github.ggeorg.delos.writer.document.TableCellSelection;
import io.github.ggeorg.delos.writer.document.TableCellStoryPath;
import io.github.ggeorg.delos.writer.document.TableRow;
import io.github.ggeorg.delos.writer.document.TableStyle;
import io.github.ggeorg.delos.writer.document.TableColumnSpec;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.document.TextRun;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure document-editing helper working in logical paragraph/offset space.
 */
public final class DocumentEditor {
    private final StoryEditor storyEditor = new StoryEditor();

    public DocumentEdit replace(Document document, SelectionRange selection, TextPosition caret, String replacement, String description) {
        TextPosition start = selection != null && !selection.isCollapsed() ? selection.start() : caret;
        TextPosition end = selection != null && !selection.isCollapsed() ? selection.end() : caret;
        return replace(document, start, end, replacement, description);
    }

    public DocumentEdit replace(Document document, TextPosition start, TextPosition end, String replacement, String description) {
        StoryEdit storyEdit = storyEditor.replace(document.body(), start, end, replacement);
        Document updatedDocument = replaceStory(document, StoryPath.body(), storyEdit.story());
        return DocumentEdit.ofCaret(updatedDocument, storyEdit.caretPosition(), description);
    }

    public DocumentEdit replace(Document document, StoryPath storyPath, TextPosition start, TextPosition end, String replacement, String description) {
        if (storyPath == null || storyPath.isBody()) {
            return replace(document, start, end, replacement, description);
        }

        StoryEdit storyEdit = storyEditor.replace(resolveStory(document, storyPath), start, end, replacement);
        Document updatedDocument = replaceStory(document, storyPath, storyEdit.story());
        CaretPosition storyCaret = new CaretPosition(
                storyPath,
                storyEdit.caretPosition().paragraphIndex(),
                storyEdit.caretPosition().offset()
        );
        TextPosition boundaryCaret = boundaryCaretForStory(updatedDocument, storyPath);
        return DocumentEdit.ofStoryCaret(updatedDocument, boundaryCaret, storyCaret, description);
    }

    public DocumentEdit insert(Document document, StoryPath storyPath, TextPosition caret, String text, String description) {
        return replace(document, storyPath, caret, caret, text, description);
    }

    public int storyParagraphLength(Document document, StoryPath storyPath, int paragraphIndex) {
        Story story = resolveStory(document, storyPath);
        List<Paragraph> paragraphs = story.paragraphs();
        if (paragraphs.isEmpty()) {
            return 0;
        }
        int safeIndex = Math.max(0, Math.min(paragraphIndex, paragraphs.size() - 1));
        return paragraphs.get(safeIndex).length();
    }

    public int storyParagraphCount(Document document, StoryPath storyPath) {
        return Math.max(1, resolveStory(document, storyPath).paragraphs().size());
    }

    public Story resolveStory(Document document, StoryPath storyPath) {
        if (document == null || storyPath == null) {
            throw new IllegalArgumentException("document and storyPath are required");
        }
        if (storyPath.isBody()) {
            return document.body();
        }
        if (storyPath instanceof TableCellStoryPath tableCellPath) {
            return resolveTableCell(document, tableCellPath).content();
        }
        throw new IllegalArgumentException("Unsupported story path: " + storyPath);
    }

    public Document replaceStory(Document document, StoryPath storyPath, Story replacement) {
        if (document == null || storyPath == null || replacement == null) {
            throw new IllegalArgumentException("document, storyPath and replacement are required");
        }
        if (storyPath.isBody()) {
            return Document.fromBlocks(document.title(), document.pageStyle(), replacement.blocks(), document.mediaItems());
        }
        if (storyPath instanceof TableCellStoryPath tableCellPath) {
            rejectNestedTablesInTableCellStory(replacement);
            List<Block> blocks = new ArrayList<>(document.blocks());
            TableBlock tableBlock = requireTableBlock(blocks, tableCellPath.tableBlockIndex());
            requireCellAddress(tableBlock, tableCellPath.rowIndex(), tableCellPath.columnIndex());

            List<TableRow> rows = new ArrayList<>(tableBlock.rows());
            TableRow row = rows.get(tableCellPath.rowIndex());
            List<TableCell> cells = new ArrayList<>(row.cells());
            TableCell existingCell = cells.get(tableCellPath.columnIndex());
            cells.set(tableCellPath.columnIndex(), new TableCell(replacement, existingCell.style()));
            rows.set(tableCellPath.rowIndex(), new TableRow(cells));
            blocks.set(tableCellPath.tableBlockIndex(), new TableBlock(rows, tableBlock.columns(), tableBlock.headerRowCount(), tableBlock.style()));
            return Document.fromBlocks(document.title(), document.pageStyle(), List.copyOf(blocks), document.mediaItems());
        }
        throw new IllegalArgumentException("Unsupported story path: " + storyPath);
    }

    public DocumentEdit insertBlock(Document document, TextPosition caret, Block block, List<DocumentMediaItem> mediaItems, String description) {
        List<Paragraph> paragraphs = new ArrayList<>(document.paragraphs());
        if (paragraphs.isEmpty()) {
            paragraphs.add(Paragraph.of(""));
        }

        TextPosition safeCaret = clampPosition(paragraphs, caret);
        List<Block> updated = new ArrayList<>();
        int paragraphIndex = 0;
        int insertedBlockIndex = -1;

        for (Block currentBlock : document.blocks()) {
            if (currentBlock instanceof ParagraphBlock paragraphBlock && paragraphIndex == safeCaret.paragraphIndex() && insertedBlockIndex < 0) {
                Paragraph paragraph = paragraphBlock.paragraph();
                int offset = Math.max(0, Math.min(safeCaret.offset(), paragraph.length()));

                if (offset <= 0) {
                    insertedBlockIndex = updated.size();
                    updated.add(block);
                    updated.add(currentBlock);
                } else if (offset >= paragraph.length()) {
                    updated.add(currentBlock);
                    insertedBlockIndex = updated.size();
                    updated.add(block);
                } else {
                    updated.add(new ParagraphBlock(new Paragraph(paragraph.style(), prefixRuns(paragraph, offset))));
                    insertedBlockIndex = updated.size();
                    updated.add(block);
                    updated.add(new ParagraphBlock(new Paragraph(paragraph.style(), suffixRuns(paragraph, offset))));
                }

                paragraphIndex += 1;
                continue;
            }

            updated.add(currentBlock);
            if (currentBlock instanceof ParagraphBlock) {
                paragraphIndex += 1;
            }
        }

        if (insertedBlockIndex < 0) {
            insertedBlockIndex = updated.size();
            updated.add(block);
        }

        if (updated.stream().noneMatch(ParagraphBlock.class::isInstance) || !hasParagraphAfter(updated, insertedBlockIndex)) {
            updated.add(new ParagraphBlock(Paragraph.of("")));
        }

        TextPosition newCaret = nearestTextPositionAfterBlock(updated, insertedBlockIndex);
        Document updatedDocument = Document.fromBlocks(
                document.title(),
                document.pageStyle(),
                List.copyOf(updated),
                mergeMedia(document.mediaItems(), mediaItems)
        );
        return DocumentEdit.ofCaret(updatedDocument, newCaret, description);
    }

    public DocumentEdit replaceTableCellText(Document document, TableCellSelection cellSelection, String replacement, String description) {
        if (document == null || cellSelection == null) {
            throw new IllegalArgumentException("document and cellSelection are required");
        }
        List<Block> blocks = new ArrayList<>(document.blocks());
        if (cellSelection.blockIndex() >= blocks.size() || !(blocks.get(cellSelection.blockIndex()) instanceof TableBlock tableBlock)) {
            TextPosition caret = nearestTextPositionAfterBlock(blocks, Math.max(0, Math.min(cellSelection.blockIndex(), blocks.size() - 1)));
            return DocumentEdit.ofCaret(document, caret, description == null ? "Replace Table Cell" : description);
        }
        if (cellSelection.rowIndex() >= tableBlock.rowCount() || cellSelection.columnIndex() >= tableBlock.columnCount()) {
            TextPosition caret = nearestTextPositionAfterBlock(blocks, cellSelection.blockIndex());
            return DocumentEdit.ofCaret(document, caret, description == null ? "Replace Table Cell" : description);
        }

        Story replacementStory = storyEditor.fromPlainText(replacement);
        Document updatedDocument = replaceStory(document, TableCellStoryPath.from(cellSelection), replacementStory);
        TextPosition caret = nearestTextPositionAfterBlock(updatedDocument.blocks(), cellSelection.blockIndex());
        return DocumentEdit.ofCaret(updatedDocument, caret, description == null ? "Replace Table Cell" : description);
    }

    public String tableCellPlainText(Document document, TableCellSelection cellSelection) {
        if (document == null || cellSelection == null || cellSelection.blockIndex() >= document.blocks().size()) {
            return "";
        }
        if (!(document.blocks().get(cellSelection.blockIndex()) instanceof TableBlock tableBlock)) {
            return "";
        }
        if (cellSelection.rowIndex() >= tableBlock.rowCount() || cellSelection.columnIndex() >= tableBlock.columnCount()) {
            return "";
        }
        Story story = resolveStory(document, TableCellStoryPath.from(cellSelection));
        return storyEditor.plainText(story);
    }

    public TableBlock tableAt(Document document, TableCellSelection cellSelection) {
        if (document == null || cellSelection == null || cellSelection.blockIndex() >= document.blocks().size()) {
            return null;
        }
        if (!(document.blocks().get(cellSelection.blockIndex()) instanceof TableBlock tableBlock)) {
            return null;
        }
        if (cellSelection.rowIndex() >= tableBlock.rowCount() || cellSelection.columnIndex() >= tableBlock.columnCount()) {
            return null;
        }
        return tableBlock;
    }

    public DocumentEdit insertTableRow(Document document, TableCellSelection cellSelection, boolean after, String description) {
        return updateTable(document, cellSelection, description == null ? "Insert Table Row" : description, table -> {
            int insertIndex = cellSelection.rowIndex() + (after ? 1 : 0);
            List<TableRow> rows = new ArrayList<>(table.rows());
            rows.add(insertIndex, TableRow.blank(table.columnCount()));
            int headerRows = table.headerRowCount();
            if (insertIndex < headerRows) {
                headerRows += 1;
            }
            return new TableBlock(rows, table.columns(), headerRows, table.style());
        });
    }

    public DocumentEdit deleteTableRow(Document document, TableCellSelection cellSelection, String description) {
        return updateTable(document, cellSelection, description == null ? "Delete Table Row" : description, table -> {
            if (table.rowCount() <= 1) {
                return table;
            }
            List<TableRow> rows = new ArrayList<>(table.rows());
            rows.remove(cellSelection.rowIndex());
            int headerRows = Math.min(table.headerRowCount(), rows.size());
            if (cellSelection.rowIndex() < table.headerRowCount()) {
                headerRows = Math.max(0, headerRows - 1);
            }
            return new TableBlock(rows, table.columns(), headerRows, table.style());
        });
    }

    public DocumentEdit insertTableColumn(Document document, TableCellSelection cellSelection, boolean after, String description) {
        return updateTable(document, cellSelection, description == null ? "Insert Table Column" : description, table -> {
            int insertIndex = cellSelection.columnIndex() + (after ? 1 : 0);
            List<TableRow> rows = new ArrayList<>();
            for (TableRow row : table.rows()) {
                List<TableCell> cells = new ArrayList<>(row.cells());
                cells.add(insertIndex, TableCell.blank());
                rows.add(new TableRow(cells));
            }
            List<TableColumnSpec> columns = new ArrayList<>(table.columns());
            columns.add(insertIndex, TableColumnSpec.equal());
            return new TableBlock(rows, columns, table.headerRowCount(), table.style());
        });
    }

    public DocumentEdit deleteTableColumn(Document document, TableCellSelection cellSelection, String description) {
        return updateTable(document, cellSelection, description == null ? "Delete Table Column" : description, table -> {
            if (table.columnCount() <= 1) {
                return table;
            }
            List<TableRow> rows = new ArrayList<>();
            for (TableRow row : table.rows()) {
                List<TableCell> cells = new ArrayList<>(row.cells());
                cells.remove(cellSelection.columnIndex());
                rows.add(new TableRow(cells));
            }
            List<TableColumnSpec> columns = new ArrayList<>(table.columns());
            columns.remove(cellSelection.columnIndex());
            return new TableBlock(rows, columns, table.headerRowCount(), table.style());
        });
    }

    public DocumentEdit setTableHeaderRow(Document document, TableCellSelection cellSelection, boolean enabled, String description) {
        return updateTable(document, cellSelection, description == null ? "Toggle Table Header Row" : description,
                table -> table.withHeaderRowCount(enabled ? 1 : 0));
    }

    public DocumentEdit updateTableStyle(
            Document document,
            TableCellSelection cellSelection,
            double widthFraction,
            double cellPadding,
            boolean bordersEnabled,
            String description
    ) {
        return updateTable(document, cellSelection, description == null ? "Edit Table Properties" : description,
                table -> table.withStyle(new TableStyle(widthFraction, cellPadding, bordersEnabled)));
    }


    private DocumentEdit updateTable(Document document, TableCellSelection cellSelection, String description, java.util.function.UnaryOperator<TableBlock> updater) {
        if (document == null || cellSelection == null || updater == null) {
            throw new IllegalArgumentException("document, cellSelection and updater are required");
        }
        List<Block> blocks = new ArrayList<>(document.blocks());
        if (cellSelection.blockIndex() >= blocks.size() || !(blocks.get(cellSelection.blockIndex()) instanceof TableBlock tableBlock)) {
            TextPosition caret = nearestTextPositionAfterBlock(blocks, Math.max(0, Math.min(cellSelection.blockIndex(), blocks.size() - 1)));
            return DocumentEdit.ofCaret(document, caret, description);
        }
        if (cellSelection.rowIndex() >= tableBlock.rowCount() || cellSelection.columnIndex() >= tableBlock.columnCount()) {
            TextPosition caret = nearestTextPositionAfterBlock(blocks, cellSelection.blockIndex());
            return DocumentEdit.ofCaret(document, caret, description);
        }

        TableBlock updatedTable = updater.apply(tableBlock);
        blocks.set(cellSelection.blockIndex(), updatedTable);
        TextPosition caret = nearestTextPositionAfterBlock(blocks, cellSelection.blockIndex());
        Document updatedDocument = Document.fromBlocks(document.title(), document.pageStyle(), List.copyOf(blocks), document.mediaItems());
        return DocumentEdit.ofCaret(updatedDocument, caret, description);
    }

    public DocumentEdit removeBlock(Document document, int blockIndex, String description) {
        if (blockIndex < 0 || blockIndex >= document.blocks().size()) {
            TextPosition caret = new TextPosition(0, 0);
            return DocumentEdit.ofCaret(document, caret, description);
        }
        if (document.blocks().get(blockIndex) instanceof ParagraphBlock) {
            TextPosition caret = nearestTextPositionAfterBlock(document.blocks(), blockIndex);
            return DocumentEdit.ofCaret(document, caret, description);
        }

        List<Block> updated = new ArrayList<>(document.blocks());
        updated.remove(blockIndex);
        if (updated.stream().noneMatch(ParagraphBlock.class::isInstance)) {
            updated.add(new ParagraphBlock(Paragraph.of("")));
        }
        TextPosition caret = nearestTextPositionAfterBlock(updated, Math.min(blockIndex, updated.size() - 1));
        Document updatedDocument = Document.fromBlocks(document.title(), document.pageStyle(), updated, document.mediaItems());
        return DocumentEdit.ofCaret(updatedDocument, caret, description);
    }

    public DocumentEdit replaceBlock(Document document, int blockIndex, Block replacement, String description) {
        if (document == null || replacement == null) {
            throw new IllegalArgumentException("document and replacement are required");
        }
        if (blockIndex < 0 || blockIndex >= document.blocks().size()) {
            TextPosition caret = new TextPosition(0, 0);
            return DocumentEdit.ofCaret(document, caret, description == null ? "Replace Block" : description);
        }
        if (document.blocks().get(blockIndex) instanceof ParagraphBlock) {
            TextPosition caret = nearestTextPositionAfterBlock(document.blocks(), blockIndex);
            return DocumentEdit.ofCaret(document, caret, description == null ? "Replace Block" : description);
        }

        List<Block> updated = new ArrayList<>(document.blocks());
        updated.set(blockIndex, replacement);
        TextPosition caret = nearestTextPositionAfterBlock(updated, blockIndex);
        Document updatedDocument = Document.fromBlocks(document.title(), document.pageStyle(), updated, document.mediaItems());
        return DocumentEdit.ofCaret(updatedDocument, caret, description == null ? "Replace Block" : description);
    }

    private TextPosition boundaryCaretForStory(Document document, StoryPath storyPath) {
        if (storyPath instanceof TableCellStoryPath tableCellPath) {
            return nearestTextPositionAfterBlock(document.blocks(), tableCellPath.tableBlockIndex());
        }
        if (storyPath == null || storyPath.isBody()) {
            return new TextPosition(0, 0);
        }
        throw new IllegalArgumentException("Unsupported story path: " + storyPath);
    }

    private TableCell resolveTableCell(Document document, TableCellStoryPath path) {
        TableBlock tableBlock = requireTableBlock(document.blocks(), path.tableBlockIndex());
        requireCellAddress(tableBlock, path.rowIndex(), path.columnIndex());
        return tableBlock.rows().get(path.rowIndex()).cells().get(path.columnIndex());
    }

    private TableBlock requireTableBlock(List<Block> blocks, int blockIndex) {
        if (blockIndex >= blocks.size() || !(blocks.get(blockIndex) instanceof TableBlock tableBlock)) {
            throw new IllegalArgumentException("Story path does not point at a table block: " + blockIndex);
        }
        return tableBlock;
    }

    private void requireCellAddress(TableBlock tableBlock, int rowIndex, int columnIndex) {
        if (rowIndex >= tableBlock.rowCount() || columnIndex >= tableBlock.columnCount()) {
            throw new IllegalArgumentException("Story path does not point at an existing table cell");
        }
    }

    private void rejectNestedTablesInTableCellStory(Story story) {
        for (Block block : story.blocks()) {
            if (block instanceof TableBlock) {
                throw new IllegalArgumentException("Nested tables inside table cells are model-capable but disabled in v1");
            }
        }
    }

    private boolean hasParagraphAfter(List<Block> blocks, int blockIndex) {
        for (int index = blockIndex + 1; index < blocks.size(); index++) {
            if (blocks.get(index) instanceof ParagraphBlock) {
                return true;
            }
        }
        return false;
    }

    private List<DocumentMediaItem> mergeMedia(List<DocumentMediaItem> existing, List<DocumentMediaItem> additions) {
        if (additions == null || additions.isEmpty()) {
            return existing;
        }
        List<DocumentMediaItem> merged = new ArrayList<>(existing);
        for (DocumentMediaItem item : additions) {
            if (item == null) {
                continue;
            }
            merged.removeIf(existingItem -> existingItem.path().equals(item.path()));
            merged.add(item);
        }
        return List.copyOf(merged);
    }

    private TextPosition nearestTextPositionAfterBlock(List<Block> blocks, int blockIndex) {
        int paragraphIndex = 0;
        for (int index = 0; index < blocks.size(); index++) {
            Block block = blocks.get(index);
            if (block instanceof ParagraphBlock paragraphBlock) {
                if (index >= blockIndex) {
                    return new TextPosition(paragraphIndex, 0);
                }
                paragraphIndex += 1;
            }
        }

        paragraphIndex = 0;
        TextPosition best = new TextPosition(0, 0);
        for (Block block : blocks) {
            if (block instanceof ParagraphBlock paragraphBlock) {
                best = new TextPosition(paragraphIndex, paragraphBlock.paragraph().length());
                paragraphIndex += 1;
            }
        }
        return best;
    }

    private TextPosition clampPosition(List<Paragraph> paragraphs, TextPosition position) {
        int paragraphIndex = Math.max(0, Math.min(position.paragraphIndex(), paragraphs.size() - 1));
        int offset = Math.max(0, Math.min(position.offset(), paragraphs.get(paragraphIndex).length()));
        return new TextPosition(paragraphIndex, offset);
    }

    private List<TextRun> prefixRuns(Paragraph paragraph, int endOffsetExclusive) {
        List<TextRun> result = new ArrayList<>();
        int offset = 0;
        for (TextRun run : paragraph.runs()) {
            int runStart = offset;
            int runEnd = offset + run.text().length();
            if (endOffsetExclusive <= runStart) {
                break;
            }
            if (endOffsetExclusive >= runEnd) {
                result.add(run);
            } else {
                result.add(run.withText(run.text().substring(0, endOffsetExclusive - runStart)));
                break;
            }
            offset = runEnd;
        }
        return result;
    }

    private List<TextRun> suffixRuns(Paragraph paragraph, int startOffsetInclusive) {
        List<TextRun> result = new ArrayList<>();
        int offset = 0;
        for (TextRun run : paragraph.runs()) {
            int runStart = offset;
            int runEnd = offset + run.text().length();
            if (startOffsetInclusive >= runEnd) {
                offset = runEnd;
                continue;
            }
            if (startOffsetInclusive <= runStart) {
                result.add(run);
            } else {
                result.add(run.withText(run.text().substring(startOffsetInclusive - runStart)));
            }
            offset = runEnd;
        }
        return result;
    }
}
