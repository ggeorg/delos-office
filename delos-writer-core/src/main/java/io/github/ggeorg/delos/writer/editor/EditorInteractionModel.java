package io.github.ggeorg.delos.writer.editor;

import io.github.ggeorg.delos.writer.document.BlockSelection;
import io.github.ggeorg.delos.writer.document.CaretPosition;
import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.document.TableCellSelection;
import io.github.ggeorg.delos.writer.document.TableCellStoryPath;
import io.github.ggeorg.delos.writer.document.TextPosition;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central interaction state for the current editor surface.
 * <p>
 * This model deliberately does not use JavaFX properties. Caret and selection
 * changes are explicit: user-navigation methods notify immediately, while
 * {@link #restore(TextPosition, SelectionRange)} is silent so layout code can
 * rebuild first and notify once the new layout is current.
 */
public final class EditorInteractionModel {
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();
    private TextPosition caretPosition;
    private CaretPosition storyCaretPosition;
    private SelectionRange selectionRange;
    private BlockSelection blockSelection;
    private TableCellSelection tableCellSelection;

    public TextPosition caretPosition() {
        return caretPosition;
    }

    public CaretPosition storyCaretPosition() {
        return storyCaretPosition;
    }

    public boolean hasStoryCaret() {
        return storyCaretPosition != null;
    }

    public TableCellSelection storyCaretTableCellSelection() {
        if (storyCaretPosition == null || !(storyCaretPosition.storyPath() instanceof TableCellStoryPath path)) {
            return null;
        }
        return path.toTableCellSelection();
    }

    public SelectionRange selectionRange() {
        return selectionRange;
    }

    public BlockSelection blockSelection() {
        return blockSelection;
    }

    public TableCellSelection tableCellSelection() {
        return tableCellSelection;
    }

    public void addChangeListener(Runnable listener) {
        changeListeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    public void fireChanged() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }

    public void setCaret(TextPosition position) {
        if (position == null) {
            return;
        }
        restore(position, null);
        fireChanged();
    }

    public void setStoryCaret(CaretPosition position, TextPosition legacyBoundaryCaret) {
        if (position == null) {
            return;
        }
        if (position.isBodyStory()) {
            setCaret(position.toLegacyBodyTextPosition());
            return;
        }
        this.caretPosition = legacyBoundaryCaret == null ? this.caretPosition : legacyBoundaryCaret;
        this.storyCaretPosition = position;
        this.selectionRange = null;
        this.blockSelection = null;
        this.tableCellSelection = null;
        fireChanged();
    }

    public void moveCaret(TextPosition position, boolean extendSelection) {
        if (position == null) {
            return;
        }

        TextPosition current = caretPosition();
        if (!extendSelection || current == null) {
            setCaret(position);
            return;
        }

        SelectionRange currentSelection = selectionRange;
        TextPosition anchor = currentSelection == null ? current : currentSelection.anchor();
        setSelection(anchor, position);
    }

    public void setSelection(TextPosition anchor, TextPosition focus) {
        if (anchor == null || focus == null) {
            return;
        }

        SelectionRange resolvedSelection = anchor.compareTo(focus) == 0 ? null : new SelectionRange(anchor, focus);
        restore(focus, resolvedSelection);
        fireChanged();
    }

    public void setBlockSelection(BlockSelection selection, TextPosition caretPosition) {
        if (selection == null) {
            clearBlockSelection();
            return;
        }
        this.caretPosition = caretPosition == null ? this.caretPosition : caretPosition;
        this.storyCaretPosition = null;
        this.selectionRange = null;
        this.blockSelection = selection;
        this.tableCellSelection = null;
        fireChanged();
    }

    public void setTableCellSelection(TableCellSelection selection, TextPosition caretPosition) {
        if (selection == null) {
            clearTableCellSelection();
            return;
        }
        this.caretPosition = caretPosition == null ? this.caretPosition : caretPosition;
        this.storyCaretPosition = null;
        this.selectionRange = null;
        this.blockSelection = null;
        this.tableCellSelection = selection;
        fireChanged();
    }

    public void clearBlockSelection() {
        if (blockSelection == null) {
            return;
        }
        blockSelection = null;
        fireChanged();
    }

    public void clearTableCellSelection() {
        if (tableCellSelection == null) {
            return;
        }
        tableCellSelection = null;
        fireChanged();
    }

    public TextPosition selectionAnchorOrCaret() {
        SelectionRange selection = selectionRange;
        return selection == null ? caretPosition() : selection.anchor();
    }

    public void collapseSelectionToCaret() {
        if (selectionRange == null && blockSelection == null && tableCellSelection == null && storyCaretPosition == null) {
            return;
        }
        restore(caretPosition, null);
        fireChanged();
    }

    public void restore(TextPosition caret, SelectionRange selection) {
        this.caretPosition = caret;
        this.storyCaretPosition = null;
        this.selectionRange = selection == null || selection.isCollapsed() ? null : selection;
        this.blockSelection = null;
        this.tableCellSelection = null;
    }
}
