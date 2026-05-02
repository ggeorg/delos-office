package io.github.ggeorg.delos.writer.editor;

import io.github.ggeorg.delos.writer.document.CaretPosition;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.session.UndoableCommand;
import io.github.ggeorg.delos.writer.session.EditorSession;

import java.util.Objects;

/**
 * Unified undoable command for any document mutation.
 * <p>
 * The command owns document undo/redo state and carries desired caret metadata
 * for the viewport. It deliberately does not mutate {@link EditorInteractionModel};
 * caret restoration must happen after the viewport has rebuilt the layout.
 */
public final class EditCommand implements UndoableCommand {
    private final EditorSession session;
    private final Document oldDocument;
    private final TextPosition oldCaret;
    private final CaretPosition oldStoryCaret;
    private final SelectionRange oldSelection;
    private final DocumentEdit newState;

    public EditCommand(EditorSession session, TextPosition oldCaret, SelectionRange oldSelection, DocumentEdit newState) {
        this(session, oldCaret, oldSelection, null, newState);
    }

    public EditCommand(EditorSession session, TextPosition oldCaret, SelectionRange oldSelection, CaretPosition oldStoryCaret, DocumentEdit newState) {
        this.session = Objects.requireNonNull(session, "session");
        this.newState = Objects.requireNonNull(newState, "newState");
        this.oldDocument = session.document();
        this.oldCaret = oldCaret;
        this.oldStoryCaret = oldStoryCaret;
        this.oldSelection = oldSelection == null || oldSelection.isCollapsed() ? null : oldSelection;
    }

    @Override
    public String description() {
        return newState.description();
    }

    public TextPosition executeCaretPosition() {
        return newState.caretPosition();
    }

    public CaretPosition executeStoryCaretPosition() {
        return newState.storyCaretPosition();
    }

    public SelectionRange executeSelectionRange() {
        return newState.selectionRange();
    }

    public TextPosition undoCaretPosition() {
        return oldCaret;
    }

    public CaretPosition undoStoryCaretPosition() {
        return oldStoryCaret;
    }

    public SelectionRange undoSelectionRange() {
        return oldSelection;
    }

    @Override
    public void execute() {
        session.setDocument(newState.document());
    }

    @Override
    public void undo() {
        session.setDocument(oldDocument);
    }
}
