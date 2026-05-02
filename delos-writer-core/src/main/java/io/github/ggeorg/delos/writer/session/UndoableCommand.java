package io.github.ggeorg.delos.writer.session;

/**
 * Undoable command abstraction for editor session history.
 */
public interface UndoableCommand {

    String description();

    void execute();

    default void undo() {
        throw new UnsupportedOperationException("Undo is not implemented yet.");
    }
}
