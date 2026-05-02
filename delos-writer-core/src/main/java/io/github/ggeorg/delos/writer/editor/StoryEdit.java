package io.github.ggeorg.delos.writer.editor;

import io.github.ggeorg.delos.writer.document.Story;
import io.github.ggeorg.delos.writer.document.TextPosition;

import java.util.Objects;

/**
 * Result of editing one {@link Story}.
 *
 * <p>This is intentionally story-local. The returned caret is expressed in the
 * edited story's paragraph projection while the live editor is still migrating
 * from body-only {@code TextPosition} to structured {@code CaretPosition}.</p>
 */
public record StoryEdit(
        Story story,
        TextPosition caretPosition
) {
    public StoryEdit {
        story = Objects.requireNonNull(story, "story");
        caretPosition = Objects.requireNonNull(caretPosition, "caretPosition");
    }

    public static StoryEdit ofCaret(Story story, TextPosition caretPosition) {
        return new StoryEdit(story, caretPosition);
    }
}
