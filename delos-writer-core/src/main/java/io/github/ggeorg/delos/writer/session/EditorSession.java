package io.github.ggeorg.delos.writer.session;

import io.github.ggeorg.delos.writer.document.Document;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds the active document and command history.
 */
public final class EditorSession {
    private final Deque<UndoableCommand> undoStack = new ArrayDeque<>();
    private final Deque<UndoableCommand> redoStack = new ArrayDeque<>();
    private final List<Runnable> stateListeners = new CopyOnWriteArrayList<>();
    private Document document;
    private Document cleanDocument;

    public EditorSession(Document document) {
        this.document = Objects.requireNonNull(document, "document");
        this.cleanDocument = this.document;
    }

    public Document document() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = Objects.requireNonNull(document, "document");
    }

    public void loadDocument(Document document) {
        this.document = Objects.requireNonNull(document, "document");
        this.cleanDocument = this.document;
        undoStack.clear();
        redoStack.clear();
        notifyStateChanged();
    }

    public void markClean() {
        cleanDocument = document;
        notifyStateChanged();
    }

    public boolean isDirty() {
        return !document.equals(cleanDocument);
    }

    public void addStateListener(Runnable listener) {
        stateListeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeStateListener(Runnable listener) {
        stateListeners.remove(listener);
    }

    public void execute(UndoableCommand command) {
        Objects.requireNonNull(command, "command");
        command.execute();
        undoStack.push(command);
        redoStack.clear();
        notifyStateChanged();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public UndoableCommand peekUndoCommand() {
        return undoStack.peek();
    }

    public UndoableCommand peekRedoCommand() {
        return redoStack.peek();
    }

    public void undo() {
        if (undoStack.isEmpty()) {
            return;
        }
        UndoableCommand command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        notifyStateChanged();
    }

    public void redo() {
        if (redoStack.isEmpty()) {
            return;
        }
        UndoableCommand command = redoStack.pop();
        command.execute();
        undoStack.push(command);
        notifyStateChanged();
    }

    public int undoDepth() {
        return undoStack.size();
    }

    private void notifyStateChanged() {
        for (Runnable listener : stateListeners) {
            listener.run();
        }
    }
}
