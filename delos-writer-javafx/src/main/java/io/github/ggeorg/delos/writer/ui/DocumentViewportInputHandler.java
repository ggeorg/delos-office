package io.github.ggeorg.delos.writer.ui;

import io.github.ggeorg.delos.writer.document.BlockSelection;
import io.github.ggeorg.delos.writer.document.CaretPosition;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.document.StoryPath;
import io.github.ggeorg.delos.writer.document.TableCellSelection;
import io.github.ggeorg.delos.writer.document.TableCellStoryPath;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.editor.EditorInteractionModel;
import io.github.ggeorg.delos.writer.editor.TableCellNavigator;
import io.github.ggeorg.delos.writer.layout.CaretGeometry;
import io.github.ggeorg.delos.writer.layout.CaretLocator;
import io.github.ggeorg.delos.writer.layout.DocumentPositionNavigator;
import io.github.ggeorg.delos.writer.layout.HitTestResult;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Translates raw JavaFX input events into semantic editor actions.
 */
public final class DocumentViewportInputHandler {
    private final Supplier<LaidOutDocument> laidOutDocumentSupplier;
    private final EditorInteractionModel interactionModel;
    private final DocumentPositionNavigator navigator;
    private final CaretLocator caretLocator;
    private final TableCellNavigator tableCellNavigator = new TableCellNavigator();
    private final DocumentViewportEditController editController;
    private final BiFunction<Double, Double, HitTestResult> hitTestFinder;
    private final Runnable focusRequester;
    private final Supplier<Document> documentSupplier;
    private final Supplier<Double> preferredCaretPageXSupplier;
    private final Consumer<Double> preferredCaretPageXSetter;
    private boolean draggingSelection;
    private TextPosition dragAnchor;

    public DocumentViewportInputHandler(
            Supplier<LaidOutDocument> laidOutDocumentSupplier,
            EditorInteractionModel interactionModel,
            DocumentPositionNavigator navigator,
            CaretLocator caretLocator,
            DocumentViewportEditController editController,
            BiFunction<Double, Double, HitTestResult> hitTestFinder,
            Runnable focusRequester,
            Supplier<Document> documentSupplier,
            Supplier<Double> preferredCaretPageXSupplier,
            Consumer<Double> preferredCaretPageXSetter
    ) {
        this.laidOutDocumentSupplier = laidOutDocumentSupplier;
        this.interactionModel = interactionModel;
        this.navigator = navigator;
        this.caretLocator = caretLocator;
        this.editController = editController;
        this.hitTestFinder = hitTestFinder;
        this.focusRequester = focusRequester;
        this.documentSupplier = documentSupplier;
        this.preferredCaretPageXSupplier = preferredCaretPageXSupplier;
        this.preferredCaretPageXSetter = preferredCaretPageXSetter;
    }

    public void handleMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        focusRequester.run();
        preferredCaretPageXSetter.accept(null);

        HitTestResult hit = hitTestFinder.apply(event.getSceneX(), event.getSceneY());
        if (hit == null) {
            return;
        }

        Document document = documentSupplier.get();
        if (hit.storyPosition() != null && hit.storyPosition().storyPath().isTableCell()) {
            interactionModel.setStoryCaret(hit.storyPosition(), tableBoundaryCaret(document, hit.tableCellSelection()));
            draggingSelection = false;
            dragAnchor = null;
            event.consume();
            return;
        }

        if (hit.tableCellSelection() != null) {
            BlockSelection tableBlock = new BlockSelection(hit.tableCellSelection().blockIndex());
            interactionModel.setTableCellSelection(
                    hit.tableCellSelection(),
                    BlockNavigationSupport.textPositionAfterBlock(document, tableBlock)
            );
            draggingSelection = false;
            dragAnchor = null;
            event.consume();
            return;
        }

        BlockSelection blockSelection = BlockNavigationSupport.selectFromHit(document, hit.blockSelection());
        if (blockSelection != null) {
            interactionModel.setBlockSelection(blockSelection, BlockNavigationSupport.textPositionAfterBlock(document, blockSelection));
            draggingSelection = false;
            dragAnchor = null;
            event.consume();
            return;
        }
        if (hit.position() == null) {
            return;
        }

        LaidOutDocument laidOutDocument = laidOutDocumentSupplier.get();
        if (event.getClickCount() >= 3) {
            selectRange(navigator.paragraphRangeAt(laidOutDocument, hit.position()));
            draggingSelection = false;
            dragAnchor = null;
            event.consume();
            return;
        }
        if (event.getClickCount() == 2) {
            selectRange(navigator.wordRangeAt(laidOutDocument, hit.position()));
            draggingSelection = false;
            dragAnchor = null;
            event.consume();
            return;
        }

        if (event.isShiftDown()) {
            dragAnchor = interactionModel.selectionAnchorOrCaret();
            if (dragAnchor == null) {
                dragAnchor = hit.position();
            }
            interactionModel.setSelection(dragAnchor, hit.position());
        } else {
            dragAnchor = hit.position();
            interactionModel.setCaret(hit.position());
        }

        draggingSelection = true;
        event.consume();
    }

    public void handleMouseDragged(MouseEvent event) {
        if (!draggingSelection || dragAnchor == null) {
            return;
        }

        HitTestResult hit = hitTestFinder.apply(event.getSceneX(), event.getSceneY());
        if (hit == null || hit.position() == null) {
            return;
        }

        interactionModel.setSelection(dragAnchor, hit.position());
        event.consume();
    }

    public void handleMouseReleased(MouseEvent event) {
        if (!draggingSelection) {
            return;
        }

        HitTestResult hit = hitTestFinder.apply(event.getSceneX(), event.getSceneY());
        if (hit != null && hit.position() != null && dragAnchor != null) {
            interactionModel.setSelection(dragAnchor, hit.position());
        }

        draggingSelection = false;
        dragAnchor = null;
        event.consume();
    }

    public void handleKeyTyped(KeyEvent event) {
        if (event.isControlDown() || event.isMetaDown()) {
            return;
        }

        String text = event.getCharacter();
        if (text == null || text.isEmpty() || KeyEvent.CHAR_UNDEFINED.equals(text)) {
            return;
        }

        int firstCodePoint = text.codePointAt(0);
        if (Character.isISOControl(firstCodePoint) || firstCodePoint == '\r' || firstCodePoint == '\n' || firstCodePoint == '\t') {
            return;
        }

        if (interactionModel.storyCaretPosition() != null) {
            editController.replaceAtStoryCaret(text, "Insert Text");
        } else if (interactionModel.tableCellSelection() != null) {
            editController.appendToSelectedTableCell(text);
        } else {
            editController.replaceSelection(text, "Insert Text");
        }
        event.consume();
    }

    public void handleKeyPressed(KeyEvent event) {
        LaidOutDocument laidOutDocument = laidOutDocumentSupplier.get();
        if (laidOutDocument == null || (interactionModel.caretPosition() == null && interactionModel.storyCaretPosition() == null)) {
            return;
        }

        if (interactionModel.storyCaretPosition() != null && handleStoryCaretKey(event)) {
            event.consume();
            return;
        }

        if (interactionModel.tableCellSelection() != null && handleSelectedTableCellKey(event)) {
            event.consume();
            return;
        }

        if (interactionModel.blockSelection() != null && handleSelectedBlockKey(event)) {
            event.consume();
            return;
        }

        boolean extendSelection = event.isShiftDown();
        boolean wordNavigation = event.isControlDown() || event.isAltDown();
        TextPosition current = interactionModel.caretPosition();
        TextPosition target = null;

        switch (event.getCode()) {
            case LEFT -> {
                preferredCaretPageXSetter.accept(null);
                if (!extendSelection && !wordNavigation) {
                    BlockSelection previousBlock = BlockNavigationSupport.richBlockBeforeCaret(documentSupplier.get(), current);
                    if (previousBlock != null) {
                        interactionModel.setBlockSelection(previousBlock, BlockNavigationSupport.textPositionBeforeBlock(documentSupplier.get(), previousBlock));
                        event.consume();
                        return;
                    }
                }
                target = wordNavigation
                        ? navigator.moveWordLeft(laidOutDocument, current)
                        : navigator.moveLeft(laidOutDocument, current);
            }
            case RIGHT -> {
                preferredCaretPageXSetter.accept(null);
                if (!extendSelection && !wordNavigation) {
                    BlockSelection nextBlock = BlockNavigationSupport.richBlockAfterCaret(documentSupplier.get(), current);
                    if (nextBlock != null) {
                        interactionModel.setBlockSelection(nextBlock, BlockNavigationSupport.textPositionAfterBlock(documentSupplier.get(), nextBlock));
                        event.consume();
                        return;
                    }
                }
                target = wordNavigation
                        ? navigator.moveWordRight(laidOutDocument, current)
                        : navigator.moveRight(laidOutDocument, current);
            }
            case UP -> {
                if (preferredCaretPageXSupplier.get() == null) {
                    preferredCaretPageXSetter.accept(currentCaretPageX(laidOutDocument));
                }
                Double preferredX = preferredCaretPageXSupplier.get();
                if (preferredX != null) {
                    target = navigator.moveVertical(laidOutDocument, current, -1, preferredX);
                }
            }
            case DOWN -> {
                if (preferredCaretPageXSupplier.get() == null) {
                    preferredCaretPageXSetter.accept(currentCaretPageX(laidOutDocument));
                }
                Double preferredX = preferredCaretPageXSupplier.get();
                if (preferredX != null) {
                    target = navigator.moveVertical(laidOutDocument, current, 1, preferredX);
                }
            }
            case HOME -> {
                preferredCaretPageXSetter.accept(null);
                target = event.isShortcutDown()
                        ? navigator.moveToDocumentStart(laidOutDocument)
                        : navigator.moveToLineStart(laidOutDocument, current);
            }
            case END -> {
                preferredCaretPageXSetter.accept(null);
                target = event.isShortcutDown()
                        ? navigator.moveToDocumentEnd(laidOutDocument)
                        : navigator.moveToLineEnd(laidOutDocument, current);
            }
            case PAGE_UP -> {
                if (preferredCaretPageXSupplier.get() == null) {
                    preferredCaretPageXSetter.accept(currentCaretPageX(laidOutDocument));
                }
                Double preferredX = preferredCaretPageXSupplier.get();
                if (preferredX != null) {
                    target = navigator.movePage(laidOutDocument, current, -1, preferredX);
                }
            }
            case PAGE_DOWN -> {
                if (preferredCaretPageXSupplier.get() == null) {
                    preferredCaretPageXSetter.accept(currentCaretPageX(laidOutDocument));
                }
                Double preferredX = preferredCaretPageXSupplier.get();
                if (preferredX != null) {
                    target = navigator.movePage(laidOutDocument, current, 1, preferredX);
                }
            }
            case BACK_SPACE -> {
                editController.deleteBackward();
                event.consume();
                return;
            }
            case DELETE -> {
                editController.deleteForward();
                event.consume();
                return;
            }
            case ENTER -> {
                editController.replaceSelection("\n", "Split Paragraph");
                event.consume();
                return;
            }
            case TAB -> {
                editController.replaceSelection("    ", "Insert Tab Spaces");
                event.consume();
                return;
            }
            case ESCAPE -> {
                interactionModel.collapseSelectionToCaret();
                event.consume();
                return;
            }
            default -> {
                return;
            }
        }

        if (target != null) {
            interactionModel.moveCaret(target, extendSelection);
            event.consume();
        }
    }

    private boolean handleStoryCaretKey(KeyEvent event) {
        switch (event.getCode()) {
            case LEFT -> {
                preferredCaretPageXSetter.accept(null);
                moveStoryCaretLeft();
                return true;
            }
            case RIGHT -> {
                preferredCaretPageXSetter.accept(null);
                moveStoryCaretRight();
                return true;
            }
            case UP -> {
                moveStoryCaretToCell(tableCellNavigator.aboveCell(documentSupplier.get(), interactionModel.storyCaretTableCellSelection()), false);
                return true;
            }
            case DOWN -> {
                moveStoryCaretToCell(tableCellNavigator.belowCell(documentSupplier.get(), interactionModel.storyCaretTableCellSelection()), true);
                return true;
            }
            case HOME -> {
                CaretPosition current = interactionModel.storyCaretPosition();
                interactionModel.setStoryCaret(new CaretPosition(current.storyPath(), current.storyBlockIndex(), 0), interactionModel.caretPosition());
                preferredCaretPageXSetter.accept(null);
                return true;
            }
            case END -> {
                CaretPosition current = interactionModel.storyCaretPosition();
                int length = editController.storyParagraphLength(current);
                interactionModel.setStoryCaret(new CaretPosition(current.storyPath(), current.storyBlockIndex(), length), interactionModel.caretPosition());
                preferredCaretPageXSetter.accept(null);
                return true;
            }
            case TAB -> {
                TableCellSelection target = event.isShiftDown()
                        ? tableCellNavigator.previousCell(documentSupplier.get(), interactionModel.storyCaretTableCellSelection())
                        : tableCellNavigator.nextCell(documentSupplier.get(), interactionModel.storyCaretTableCellSelection());
                moveStoryCaretToCell(target, event.isShiftDown());
                return true;
            }
            case BACK_SPACE -> {
                editController.deleteBackwardAtStoryCaret();
                return true;
            }
            case DELETE -> {
                editController.deleteForwardAtStoryCaret();
                return true;
            }
            case ENTER -> {
                editController.replaceAtStoryCaret("\n", "Split Cell Paragraph");
                return true;
            }
            case ESCAPE -> {
                TableCellSelection selection = interactionModel.storyCaretTableCellSelection();
                if (selection == null) {
                    interactionModel.collapseSelectionToCaret();
                } else {
                    interactionModel.setCaret(BlockNavigationSupport.textPositionAfterBlock(documentSupplier.get(), new BlockSelection(selection.blockIndex())));
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void moveStoryCaretLeft() {
        CaretPosition current = interactionModel.storyCaretPosition();
        if (current == null) {
            return;
        }
        if (current.offset() > 0) {
            interactionModel.setStoryCaret(new CaretPosition(current.storyPath(), current.storyBlockIndex(), current.offset() - 1), interactionModel.caretPosition());
            return;
        }
        if (current.storyBlockIndex() > 0) {
            int previousParagraph = current.storyBlockIndex() - 1;
            int previousLength = editController.storyParagraphLength(new CaretPosition(current.storyPath(), previousParagraph, 0));
            interactionModel.setStoryCaret(new CaretPosition(current.storyPath(), previousParagraph, previousLength), interactionModel.caretPosition());
            return;
        }
        moveStoryCaretToCell(tableCellNavigator.previousCell(documentSupplier.get(), interactionModel.storyCaretTableCellSelection()), true);
    }

    private void moveStoryCaretRight() {
        CaretPosition current = interactionModel.storyCaretPosition();
        if (current == null) {
            return;
        }
        int currentLength = editController.storyParagraphLength(current);
        if (current.offset() < currentLength) {
            interactionModel.setStoryCaret(new CaretPosition(current.storyPath(), current.storyBlockIndex(), current.offset() + 1), interactionModel.caretPosition());
            return;
        }
        if (current.storyBlockIndex() < editController.storyParagraphCount(current) - 1) {
            interactionModel.setStoryCaret(new CaretPosition(current.storyPath(), current.storyBlockIndex() + 1, 0), interactionModel.caretPosition());
            return;
        }
        moveStoryCaretToCell(tableCellNavigator.nextCell(documentSupplier.get(), interactionModel.storyCaretTableCellSelection()), false);
    }

    private void moveStoryCaretToCell(TableCellSelection target, boolean endOfCell) {
        Document document = documentSupplier.get();
        TableCellSelection current = interactionModel.storyCaretTableCellSelection();
        if (target != null) {
            StoryPath path = TableCellStoryPath.from(target);
            int paragraph = 0;
            int offset = 0;
            if (endOfCell) {
                CaretPosition probe = new CaretPosition(path, 0, 0);
                paragraph = Math.max(0, editController.storyParagraphCount(probe) - 1);
                offset = editController.storyParagraphLength(new CaretPosition(path, paragraph, 0));
            }
            interactionModel.setStoryCaret(new CaretPosition(path, paragraph, offset), tableBoundaryCaret(document, target));
            preferredCaretPageXSetter.accept(null);
            return;
        }
        if (current == null) {
            return;
        }
        BlockSelection tableBlock = new BlockSelection(current.blockIndex());
        TextPosition boundaryCaret = endOfCell
                ? BlockNavigationSupport.textPositionAfterBlock(document, tableBlock)
                : BlockNavigationSupport.textPositionBeforeBlock(document, tableBlock);
        if (boundaryCaret != null) {
            interactionModel.setCaret(boundaryCaret);
        }
        preferredCaretPageXSetter.accept(null);
    }

    private boolean handleSelectedTableCellKey(KeyEvent event) {
        switch (event.getCode()) {
            case LEFT -> {
                moveSelectedTableCell(tableCellNavigator.leftCell(documentSupplier.get(), interactionModel.tableCellSelection()), false);
                return true;
            }
            case RIGHT -> {
                moveSelectedTableCell(tableCellNavigator.rightCell(documentSupplier.get(), interactionModel.tableCellSelection()), true);
                return true;
            }
            case UP -> {
                moveSelectedTableCell(tableCellNavigator.aboveCell(documentSupplier.get(), interactionModel.tableCellSelection()), false);
                return true;
            }
            case DOWN -> {
                moveSelectedTableCell(tableCellNavigator.belowCell(documentSupplier.get(), interactionModel.tableCellSelection()), true);
                return true;
            }
            case TAB -> {
                TableCellSelection target = event.isShiftDown()
                        ? tableCellNavigator.previousCell(documentSupplier.get(), interactionModel.tableCellSelection())
                        : tableCellNavigator.nextCell(documentSupplier.get(), interactionModel.tableCellSelection());
                moveSelectedTableCell(target, !event.isShiftDown());
                return true;
            }
            case BACK_SPACE -> {
                editController.deleteBackwardInSelectedTableCell();
                return true;
            }
            case DELETE -> {
                editController.clearSelectedTableCell();
                return true;
            }
            case ENTER -> {
                editController.appendToSelectedTableCell("\n");
                return true;
            }
            case ESCAPE -> {
                interactionModel.collapseSelectionToCaret();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void moveSelectedTableCell(TableCellSelection target, boolean forwardBoundary) {
        Document document = documentSupplier.get();
        TableCellSelection current = interactionModel.tableCellSelection();
        if (target != null) {
            interactionModel.setTableCellSelection(target, BlockNavigationSupport.textPositionAfterBlock(document, new BlockSelection(target.blockIndex())));
            preferredCaretPageXSetter.accept(null);
            return;
        }
        if (current == null) {
            return;
        }
        BlockSelection tableBlock = new BlockSelection(current.blockIndex());
        TextPosition boundaryCaret = forwardBoundary
                ? BlockNavigationSupport.textPositionAfterBlock(document, tableBlock)
                : BlockNavigationSupport.textPositionBeforeBlock(document, tableBlock);
        if (boundaryCaret != null) {
            interactionModel.setCaret(boundaryCaret);
        }
        preferredCaretPageXSetter.accept(null);
    }

    private boolean handleSelectedBlockKey(KeyEvent event) {
        Document document = documentSupplier.get();
        BlockSelection selected = interactionModel.blockSelection();
        switch (event.getCode()) {
            case LEFT, UP -> {
                TextPosition target = BlockNavigationSupport.textPositionBeforeBlock(document, selected);
                if (target != null) {
                    interactionModel.setCaret(target);
                }
                preferredCaretPageXSetter.accept(null);
                return true;
            }
            case RIGHT, DOWN -> {
                TextPosition target = BlockNavigationSupport.textPositionAfterBlock(document, selected);
                if (target != null) {
                    interactionModel.setCaret(target);
                }
                preferredCaretPageXSetter.accept(null);
                return true;
            }
            case BACK_SPACE, DELETE -> {
                editController.deleteSelectedBlock();
                return true;
            }
            case ESCAPE -> {
                interactionModel.collapseSelectionToCaret();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private TextPosition tableBoundaryCaret(Document document, TableCellSelection selection) {
        if (selection == null) {
            return interactionModel.caretPosition();
        }
        return BlockNavigationSupport.textPositionAfterBlock(document, new BlockSelection(selection.blockIndex()));
    }

    private void selectRange(SelectionRange range) {
        if (range == null) {
            return;
        }
        interactionModel.setSelection(range.anchor(), range.focus());
    }

    private Double currentCaretPageX(LaidOutDocument laidOutDocument) {
        CaretPosition storyCaret = interactionModel.storyCaretPosition();
        CaretGeometry caret = storyCaret == null
                ? caretLocator.locateCaret(laidOutDocument, interactionModel.caretPosition())
                : caretLocator.locateCaret(laidOutDocument, storyCaret);
        return caret == null ? null : caret.x();
    }
}
