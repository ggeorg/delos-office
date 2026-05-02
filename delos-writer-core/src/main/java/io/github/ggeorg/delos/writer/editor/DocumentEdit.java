package io.github.ggeorg.delos.writer.editor;

import io.github.ggeorg.delos.writer.document.CaretPosition;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.document.TextPosition;

import java.util.Objects;

/**
 * Immutable snapshot of a document mutation result.
 */
public record DocumentEdit(
        Document document,
        TextPosition caretPosition,
        SelectionRange selectionRange,
        String description,
        CaretPosition storyCaretPosition
) {
    public DocumentEdit(Document document, TextPosition caretPosition, SelectionRange selectionRange, String description) {
        this(document, caretPosition, selectionRange, description, null);
    }

    public DocumentEdit {
        document = Objects.requireNonNull(document, "document");
        caretPosition = Objects.requireNonNull(caretPosition, "caretPosition");
        description = Objects.requireNonNull(description, "description");
    }

    public static DocumentEdit ofCaret(Document document, TextPosition caretPosition, String description) {
        return new DocumentEdit(document, caretPosition, null, description, null);
    }

    public static DocumentEdit ofStoryCaret(Document document, TextPosition boundaryCaretPosition, CaretPosition storyCaretPosition, String description) {
        return new DocumentEdit(document, boundaryCaretPosition, null, description, Objects.requireNonNull(storyCaretPosition, "storyCaretPosition"));
    }

    public static DocumentEdit ofSelection(
            Document document,
            TextPosition caretPosition,
            SelectionRange selectionRange,
            String description
    ) {
        return new DocumentEdit(document, caretPosition, selectionRange, description, null);
    }
}
