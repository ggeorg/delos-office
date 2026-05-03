package io.github.ggeorg.delos.writer.ui;

import io.github.ggeorg.delos.writer.document.Alignment;
import io.github.ggeorg.delos.writer.document.BlockSelection;
import io.github.ggeorg.delos.writer.document.CaretPosition;
import io.github.ggeorg.delos.writer.document.DocumentMediaItem;
import io.github.ggeorg.delos.writer.document.FormulaBlock;
import io.github.ggeorg.delos.writer.document.FormulaSourceFormat;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.document.ListMarkerKind;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TableCellSelection;
import io.github.ggeorg.delos.writer.session.EditorSession;
import io.github.ggeorg.delos.writer.editor.DocumentEdit;
import io.github.ggeorg.delos.writer.editor.DocumentEditor;
import io.github.ggeorg.delos.writer.editor.DocumentFormatter;
import io.github.ggeorg.delos.writer.editor.EditCommand;
import io.github.ggeorg.delos.writer.editor.EditorInteractionModel;
import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.document.Story;
import io.github.ggeorg.delos.writer.editor.TextOffsets;
import io.github.ggeorg.delos.writer.editor.TextStyle;
import io.github.ggeorg.delos.writer.layout.DocumentPositionNavigator;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.document.TextPosition;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.text.Normalizer;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Applies semantic edit intents to the document/session.
 */
public final class DocumentViewportEditController {
    private final EditorSession session;
    private final EditorInteractionModel interactionModel;
    private final DocumentPositionNavigator navigator;
    private final DocumentEditor documentEditor;
    private final DocumentFormatter documentFormatter;
    private final Supplier<LaidOutDocument> laidOutDocumentSupplier;
    private final BiConsumer<TextPosition, SelectionRange> rebuildLayout;
    private final Runnable requestFollowCaret;
    private final Runnable clearPreferredCaretX;

    public DocumentViewportEditController(
            EditorSession session,
            EditorInteractionModel interactionModel,
            DocumentPositionNavigator navigator,
            DocumentEditor documentEditor,
            DocumentFormatter documentFormatter,
            Supplier<LaidOutDocument> laidOutDocumentSupplier,
            BiConsumer<TextPosition, SelectionRange> rebuildLayout,
            Runnable requestFollowCaret,
            Runnable clearPreferredCaretX
    ) {
        this.session = session;
        this.interactionModel = interactionModel;
        this.navigator = navigator;
        this.documentEditor = documentEditor;
        this.documentFormatter = documentFormatter;
        this.laidOutDocumentSupplier = laidOutDocumentSupplier;
        this.rebuildLayout = rebuildLayout;
        this.requestFollowCaret = requestFollowCaret;
        this.clearPreferredCaretX = clearPreferredCaretX;
    }

    public void undo() {
        if (!session.canUndo()) {
            return;
        }

        EditCommand editCommand = currentEditCommand(session.peekUndoCommand());
        TextPosition targetCaret = editCommand == null ? interactionModel.caretPosition() : editCommand.undoCaretPosition();
        SelectionRange targetSelection = editCommand == null ? interactionModel.selectionRange() : editCommand.undoSelectionRange();
        CaretPosition targetStoryCaret = editCommand == null ? interactionModel.storyCaretPosition() : editCommand.undoStoryCaretPosition();
        applySessionMutation(targetCaret, targetSelection, targetStoryCaret, session::undo);
    }

    public void redo() {
        if (!session.canRedo()) {
            return;
        }

        EditCommand editCommand = currentEditCommand(session.peekRedoCommand());
        TextPosition targetCaret = editCommand == null ? interactionModel.caretPosition() : editCommand.executeCaretPosition();
        SelectionRange targetSelection = editCommand == null ? interactionModel.selectionRange() : editCommand.executeSelectionRange();
        CaretPosition targetStoryCaret = editCommand == null ? interactionModel.storyCaretPosition() : editCommand.executeStoryCaretPosition();
        applySessionMutation(targetCaret, targetSelection, targetStoryCaret, session::redo);
    }

    public void copy() {
        String text = selectedText();
        if (text.isEmpty()) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    public void cut() {
        SelectionRange selection = interactionModel.selectionRange();
        if (selection == null || selection.isCollapsed()) {
            return;
        }
        copy();
        replaceSelection("", "Cut");
    }

    public void paste() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (!clipboard.hasString()) {
            return;
        }
        replaceSelection(clipboard.getString(), "Paste");
    }

    public void selectAll() {
        LaidOutDocument laidOutDocument = laidOutDocumentSupplier.get();
        if (laidOutDocument == null) {
            return;
        }
        TextPosition start = navigator.moveToDocumentStart(laidOutDocument);
        TextPosition end = navigator.moveToDocumentEnd(laidOutDocument);
        if (start == null || end == null) {
            return;
        }
        interactionModel.setSelection(start, end);
        clearPreferredCaretX.run();
    }

    public void toggleStyle(TextStyle style) {
        SelectionRange selection = interactionModel.selectionRange();
        if (selection == null || selection.isCollapsed()) {
            return;
        }

        DocumentEdit edit = documentFormatter.toggle(
                session.document(),
                selection,
                interactionModel.caretPosition(),
                style,
                "Toggle " + style.name().toLowerCase()
        );
        applyEdit(edit);
    }


    public void setParagraphAlignment(Alignment alignment) {
        DocumentEdit edit = documentFormatter.alignParagraphs(
                session.document(),
                interactionModel.selectionRange(),
                interactionModel.caretPosition(),
                alignment,
                "Align Paragraph " + alignment.name().toLowerCase()
        );
        applyEdit(edit);
    }


    public void toggleListKind(ListMarkerKind kind) {
        DocumentEdit edit = documentFormatter.toggleListKind(
                session.document(),
                interactionModel.selectionRange(),
                interactionModel.caretPosition(),
                kind,
                "Toggle " + kind.name().toLowerCase() + " list"
        );
        applyEdit(edit);
    }

    public void increaseListLevel() {
        DocumentEdit edit = documentFormatter.increaseListLevel(
                session.document(),
                interactionModel.selectionRange(),
                interactionModel.caretPosition(),
                "Increase List Level"
        );
        applyEdit(edit);
    }

    public void decreaseListLevel() {
        DocumentEdit edit = documentFormatter.decreaseListLevel(
                session.document(),
                interactionModel.selectionRange(),
                interactionModel.caretPosition(),
                "Decrease List Level"
        );
        applyEdit(edit);
    }


    public void insertImage(DocumentMediaItem mediaItem, double width, double height, String altText) {
        if (mediaItem == null) {
            return;
        }

        TextPosition caret = prepareAtomicBlockInsertion("Prepare Image Insertion");
        ImageBlock imageBlock = new ImageBlock(mediaItem.path(), width, height, altText);
        DocumentEdit edit = documentEditor.insertBlock(
                session.document(),
                caret,
                imageBlock,
                java.util.List.of(mediaItem),
                "Insert Image"
        );
        applyEdit(edit);
    }

    public void insertTable(int rows, int columns) {
        if (rows <= 0 || columns <= 0) {
            return;
        }

        TextPosition caret = prepareAtomicBlockInsertion("Prepare Table Insertion");
        DocumentEdit edit = documentEditor.insertBlock(
                session.document(),
                caret,
                TableBlock.blank(rows, columns),
                java.util.List.of(),
                "Insert Table"
        );
        applyEdit(edit);
    }


    public TableCellSelection activeTableCellSelection() {
        TableCellSelection selection = interactionModel.tableCellSelection();
        return selection == null ? interactionModel.storyCaretTableCellSelection() : selection;
    }

    public TableBlock selectedTableBlock() {
        return documentEditor.tableAt(session.document(), activeTableCellSelection());
    }

    public boolean hasSelectedTable() {
        return selectedTableBlock() != null;
    }

    public void insertTableRowAbove() {
        TableCellSelection selection = activeTableCellSelection();
        if (selection == null) {
            return;
        }
        DocumentEdit edit = documentEditor.insertTableRow(session.document(), selection, false, "Insert Table Row Above");
        applyEditKeepingTableCellSelection(edit, selection);
    }

    public void insertTableRowBelow() {
        TableCellSelection selection = activeTableCellSelection();
        if (selection == null) {
            return;
        }
        TableCellSelection target = new TableCellSelection(selection.blockIndex(), selection.rowIndex() + 1, selection.columnIndex());
        DocumentEdit edit = documentEditor.insertTableRow(session.document(), selection, true, "Insert Table Row Below");
        applyEditKeepingTableCellSelection(edit, target);
    }

    public void deleteTableRow() {
        TableCellSelection selection = activeTableCellSelection();
        TableBlock table = selectedTableBlock();
        if (selection == null || table == null || table.rowCount() <= 1) {
            return;
        }
        TableCellSelection target = new TableCellSelection(
                selection.blockIndex(),
                Math.min(selection.rowIndex(), table.rowCount() - 2),
                Math.min(selection.columnIndex(), table.columnCount() - 1)
        );
        DocumentEdit edit = documentEditor.deleteTableRow(session.document(), selection, "Delete Table Row");
        applyEditKeepingTableCellSelection(edit, target);
    }

    public void insertTableColumnLeft() {
        TableCellSelection selection = activeTableCellSelection();
        if (selection == null) {
            return;
        }
        DocumentEdit edit = documentEditor.insertTableColumn(session.document(), selection, false, "Insert Table Column Left");
        applyEditKeepingTableCellSelection(edit, selection);
    }

    public void insertTableColumnRight() {
        TableCellSelection selection = activeTableCellSelection();
        if (selection == null) {
            return;
        }
        TableCellSelection target = new TableCellSelection(selection.blockIndex(), selection.rowIndex(), selection.columnIndex() + 1);
        DocumentEdit edit = documentEditor.insertTableColumn(session.document(), selection, true, "Insert Table Column Right");
        applyEditKeepingTableCellSelection(edit, target);
    }

    public void deleteTableColumn() {
        TableCellSelection selection = activeTableCellSelection();
        TableBlock table = selectedTableBlock();
        if (selection == null || table == null || table.columnCount() <= 1) {
            return;
        }
        TableCellSelection target = new TableCellSelection(
                selection.blockIndex(),
                Math.min(selection.rowIndex(), table.rowCount() - 1),
                Math.min(selection.columnIndex(), table.columnCount() - 2)
        );
        DocumentEdit edit = documentEditor.deleteTableColumn(session.document(), selection, "Delete Table Column");
        applyEditKeepingTableCellSelection(edit, target);
    }

    public void setSelectedTableHeaderRow(boolean enabled) {
        TableCellSelection selection = activeTableCellSelection();
        if (selection == null) {
            return;
        }
        DocumentEdit edit = documentEditor.setTableHeaderRow(session.document(), selection, enabled, "Toggle Table Header Row");
        applyEditKeepingTableCellSelection(edit, selection);
    }

    public void updateSelectedTableProperties(double widthFraction, double cellPadding, boolean bordersEnabled) {
        TableCellSelection selection = activeTableCellSelection();
        if (selection == null) {
            return;
        }
        DocumentEdit edit = documentEditor.updateTableStyle(
                session.document(),
                selection,
                widthFraction,
                cellPadding,
                bordersEnabled,
                "Edit Table Properties"
        );
        applyEditKeepingTableCellSelection(edit, selection);
    }


    public void appendToSelectedTableCell(String text) {
        TableCellSelection selection = interactionModel.tableCellSelection();
        if (selection == null || text == null || text.isEmpty()) {
            return;
        }
        String current = documentEditor.tableCellPlainText(session.document(), selection);
        replaceTableCellText(selection, current + normalizeUserText(text), "Edit Table Cell");
    }

    public void deleteBackwardInSelectedTableCell() {
        TableCellSelection selection = interactionModel.tableCellSelection();
        if (selection == null) {
            return;
        }
        String current = documentEditor.tableCellPlainText(session.document(), selection);
        if (current.isEmpty()) {
            return;
        }
        int cut = TextOffsets.previousCodePointOffset(current, current.length());
        replaceTableCellText(selection, current.substring(0, cut), "Edit Table Cell");
    }

    public void clearSelectedTableCell() {
        TableCellSelection selection = interactionModel.tableCellSelection();
        if (selection == null) {
            return;
        }
        replaceTableCellText(selection, "", "Clear Table Cell");
    }

    private void replaceTableCellText(TableCellSelection selection, String text, String description) {
        DocumentEdit edit = documentEditor.replaceTableCellText(session.document(), selection, text, description);
        applyEditKeepingTableCellSelection(edit, selection);
    }

    public void replaceAtStoryCaret(String replacement, String description) {
        CaretPosition storyCaret = interactionModel.storyCaretPosition();
        if (storyCaret == null) {
            replaceSelection(replacement, description);
            return;
        }
        TextPosition localCaret = new TextPosition(storyCaret.storyBlockIndex(), storyCaret.offset());
        DocumentEdit edit = documentEditor.replace(
                session.document(),
                storyCaret.storyPath(),
                localCaret,
                localCaret,
                normalizeUserText(replacement),
                description
        );
        applyEdit(edit);
    }

    public void deleteBackwardAtStoryCaret() {
        CaretPosition storyCaret = interactionModel.storyCaretPosition();
        if (storyCaret == null) {
            deleteBackward();
            return;
        }
        TextPosition end = new TextPosition(storyCaret.storyBlockIndex(), storyCaret.offset());
        TextPosition start;
        if (storyCaret.offset() > 0) {
            String current = storyParagraphText(storyCaret);
            start = new TextPosition(
                    storyCaret.storyBlockIndex(),
                    TextOffsets.previousCodePointOffset(current, storyCaret.offset())
            );
        } else if (storyCaret.storyBlockIndex() > 0) {
            int previousParagraph = storyCaret.storyBlockIndex() - 1;
            start = new TextPosition(previousParagraph, documentEditor.storyParagraphLength(session.document(), storyCaret.storyPath(), previousParagraph));
        } else {
            return;
        }
        DocumentEdit edit = documentEditor.replace(session.document(), storyCaret.storyPath(), start, end, "", "Backspace");
        applyEdit(edit);
    }

    public void deleteForwardAtStoryCaret() {
        CaretPosition storyCaret = interactionModel.storyCaretPosition();
        if (storyCaret == null) {
            deleteForward();
            return;
        }
        String current = storyParagraphText(storyCaret);
        int paragraphLength = current.length();
        int paragraphCount = documentEditor.storyParagraphCount(session.document(), storyCaret.storyPath());
        TextPosition start = new TextPosition(storyCaret.storyBlockIndex(), storyCaret.offset());
        TextPosition end;
        if (storyCaret.offset() < paragraphLength) {
            end = new TextPosition(storyCaret.storyBlockIndex(), TextOffsets.nextCodePointOffset(current, storyCaret.offset()));
        } else if (storyCaret.storyBlockIndex() < paragraphCount - 1) {
            end = new TextPosition(storyCaret.storyBlockIndex() + 1, 0);
        } else {
            return;
        }
        DocumentEdit edit = documentEditor.replace(session.document(), storyCaret.storyPath(), start, end, "", "Delete Forward");
        applyEdit(edit);
    }

    public int storyParagraphLength(CaretPosition storyCaret) {
        if (storyCaret == null) {
            return 0;
        }
        return documentEditor.storyParagraphLength(session.document(), storyCaret.storyPath(), storyCaret.storyBlockIndex());
    }

    public int storyParagraphCount(CaretPosition storyCaret) {
        if (storyCaret == null) {
            return 0;
        }
        return documentEditor.storyParagraphCount(session.document(), storyCaret.storyPath());
    }

    private String storyParagraphText(CaretPosition storyCaret) {
        Story story = documentEditor.resolveStory(session.document(), storyCaret.storyPath());
        if (story.paragraphs().isEmpty()) {
            return "";
        }
        int safeIndex = Math.max(0, Math.min(storyCaret.storyBlockIndex(), story.paragraphs().size() - 1));
        return story.paragraphs().get(safeIndex).plainText();
    }


    public void insertFormula(FormulaSourceFormat sourceFormat, String source, String altText) {
        if (source == null || source.isBlank()) {
            return;
        }

        TextPosition caret = prepareAtomicBlockInsertion("Prepare Formula Insertion");
        DocumentEdit edit = documentEditor.insertBlock(
                session.document(),
                caret,
                new FormulaBlock(sourceFormat, source, altText),
                java.util.List.of(),
                "Insert Formula"
        );
        applyEdit(edit);
    }


    public FormulaBlock selectedFormulaBlock() {
        BlockSelection selection = interactionModel.blockSelection();
        if (selection == null) {
            return null;
        }
        int blockIndex = selection.blockIndex();
        if (blockIndex < 0 || blockIndex >= session.document().blocks().size()) {
            return null;
        }
        return session.document().blocks().get(blockIndex) instanceof FormulaBlock formulaBlock ? formulaBlock : null;
    }

    public boolean hasSelectedFormulaBlock() {
        return selectedFormulaBlock() != null;
    }

    public void updateSelectedFormula(FormulaSourceFormat sourceFormat, String source, String altText) {
        BlockSelection selection = interactionModel.blockSelection();
        if (selection == null || source == null || source.isBlank()) {
            return;
        }
        if (selectedFormulaBlock() == null) {
            return;
        }

        DocumentEdit edit = documentEditor.replaceBlock(
                session.document(),
                selection.blockIndex(),
                new FormulaBlock(sourceFormat, source, altText),
                "Edit Formula"
        );
        applyEdit(edit);
    }

    public ImageBlock selectedImageBlock() {
        BlockSelection selection = interactionModel.blockSelection();
        if (selection == null) {
            return null;
        }
        int blockIndex = selection.blockIndex();
        if (blockIndex < 0 || blockIndex >= session.document().blocks().size()) {
            return null;
        }
        return session.document().blocks().get(blockIndex) instanceof ImageBlock imageBlock ? imageBlock : null;
    }

    public boolean hasSelectedImageBlock() {
        return selectedImageBlock() != null;
    }

    public void updateSelectedImageProperties(double width, double height, String altText) {
        BlockSelection selection = interactionModel.blockSelection();
        ImageBlock selected = selectedImageBlock();
        if (selection == null || selected == null) {
            return;
        }

        DocumentEdit edit = documentEditor.replaceBlock(
                session.document(),
                selection.blockIndex(),
                new ImageBlock(selected.source(), width, height, altText),
                "Edit Image Properties"
        );
        applyEdit(edit);
    }

    private TextPosition prepareAtomicBlockInsertion(String selectionRemovalDescription) {
        if (interactionModel.blockSelection() != null) {
            deleteSelectedBlock();
        } else {
            SelectionRange selection = interactionModel.selectionRange();
            if (selection != null && !selection.isCollapsed()) {
                replaceSelection("", selectionRemovalDescription);
            }
        }

        TextPosition caret = interactionModel.caretPosition();
        return caret == null ? new TextPosition(0, 0) : caret;
    }

    public void deleteSelectedBlock() {
        BlockSelection blockSelection = interactionModel.blockSelection();
        if (blockSelection == null) {
            return;
        }
        DocumentEdit edit = documentEditor.removeBlock(session.document(), blockSelection.blockIndex(), "Delete Block");
        applyEdit(edit);
    }

    public void deleteBackward() {
        if (interactionModel.blockSelection() != null) {
            deleteSelectedBlock();
            return;
        }

        SelectionRange selection = interactionModel.selectionRange();
        if (selection != null && !selection.isCollapsed()) {
            replaceSelection("", "Delete Selection");
            return;
        }

        TextPosition caret = interactionModel.caretPosition();
        if (caret == null) {
            return;
        }
        if (caret.paragraphIndex() == 0 && caret.offset() == 0) {
            return;
        }

        TextPosition start = caret.offset() > 0
                ? new TextPosition(
                        caret.paragraphIndex(),
                        TextOffsets.previousCodePointOffset(
                                session.document().paragraphs().get(caret.paragraphIndex()).plainText(),
                                caret.offset()
                        )
                )
                : new TextPosition(caret.paragraphIndex() - 1, session.document().paragraphs().get(caret.paragraphIndex() - 1).plainText().length());

        applyEdit(start, caret, "", "Backspace");
    }

    public void deleteForward() {
        if (interactionModel.blockSelection() != null) {
            deleteSelectedBlock();
            return;
        }

        SelectionRange selection = interactionModel.selectionRange();
        if (selection != null && !selection.isCollapsed()) {
            replaceSelection("", "Delete Selection");
            return;
        }

        TextPosition caret = interactionModel.caretPosition();
        if (caret == null) {
            return;
        }

        String current = session.document().paragraphs().get(caret.paragraphIndex()).plainText();
        int paragraphLength = current.length();
        TextPosition end;
        if (caret.offset() < paragraphLength) {
            end = new TextPosition(caret.paragraphIndex(), TextOffsets.nextCodePointOffset(current, caret.offset()));
        } else if (caret.paragraphIndex() < session.document().paragraphs().size() - 1) {
            end = new TextPosition(caret.paragraphIndex() + 1, 0);
        } else {
            return;
        }

        applyEdit(caret, end, "", "Delete Forward");
    }

    public void replaceSelection(String replacement, String description) {
        if (interactionModel.blockSelection() != null) {
            BlockSelection selectedBlock = interactionModel.blockSelection();
            DocumentEdit removal = documentEditor.removeBlock(session.document(), selectedBlock.blockIndex(), "Replace Block");
            applyEdit(removal);
            if (replacement == null || replacement.isEmpty()) {
                return;
            }
        }

        SelectionRange selection = interactionModel.selectionRange();
        TextPosition caret = interactionModel.caretPosition();
        DocumentEdit edit = documentEditor.replaceIncludingInterleavedBlocks(
                session.document(),
                selection,
                caret,
                normalizeUserText(replacement),
                description
        );
        applyEdit(edit);
    }

    private void applyEdit(TextPosition start, TextPosition end, String replacement, String description) {
        DocumentEdit edit = documentEditor.replace(session.document(), start, end, normalizeUserText(replacement), description);
        applyEdit(edit);
    }

    private String normalizeUserText(String text) {
        return text == null ? "" : Normalizer.normalize(text, Normalizer.Form.NFC);
    }


    private void applyEditKeepingTableCellSelection(DocumentEdit edit, TableCellSelection selection) {
        if (edit == null) {
            return;
        }
        EditCommand command = new EditCommand(
                session,
                interactionModel.caretPosition(),
                interactionModel.selectionRange(),
                interactionModel.storyCaretPosition(),
                edit
        );
        applySessionMutation(command.executeCaretPosition(), command.executeSelectionRange(), command.executeStoryCaretPosition(), () -> session.execute(command));
        interactionModel.setTableCellSelection(selection, command.executeCaretPosition());
    }

    private void applyEdit(DocumentEdit edit) {
        if (edit == null) {
            return;
        }

        EditCommand command = new EditCommand(
                session,
                interactionModel.caretPosition(),
                interactionModel.selectionRange(),
                interactionModel.storyCaretPosition(),
                edit
        );
        applySessionMutation(command.executeCaretPosition(), command.executeSelectionRange(), command.executeStoryCaretPosition(), () -> session.execute(command));
    }

    private void applySessionMutation(TextPosition targetCaret, SelectionRange targetSelection, Runnable mutation) {
        applySessionMutation(targetCaret, targetSelection, null, mutation);
    }

    private void applySessionMutation(TextPosition targetCaret, SelectionRange targetSelection, CaretPosition targetStoryCaret, Runnable mutation) {
        requestFollowCaret.run();
        mutation.run();
        clearPreferredCaretX.run();
        rebuildLayout.accept(targetCaret, targetSelection);
        if (targetStoryCaret != null) {
            interactionModel.setStoryCaret(targetStoryCaret, targetCaret);
        }
    }

    private EditCommand currentEditCommand(io.github.ggeorg.delos.writer.session.UndoableCommand command) {
        return command instanceof EditCommand editCommand ? editCommand : null;
    }

    public String selectedText() {
        SelectionRange selection = interactionModel.selectionRange();
        if (selection == null || selection.isCollapsed()) {
            return "";
        }

        TextPosition start = selection.start();
        TextPosition end = selection.end();
        StringBuilder out = new StringBuilder();

        for (int paragraphIndex = start.paragraphIndex(); paragraphIndex <= end.paragraphIndex(); paragraphIndex++) {
            String paragraphText = session.document().paragraphs().get(paragraphIndex).plainText();
            int localStart = paragraphIndex == start.paragraphIndex() ? start.offset() : 0;
            int localEnd = paragraphIndex == end.paragraphIndex() ? end.offset() : paragraphText.length();
            localStart = Math.max(0, Math.min(localStart, paragraphText.length()));
            localEnd = Math.max(localStart, Math.min(localEnd, paragraphText.length()));
            out.append(paragraphText, localStart, localEnd);
            if (paragraphIndex < end.paragraphIndex()) {
                out.append('\n');
            }
        }

        return out.toString();
    }
}
