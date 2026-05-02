package io.github.ggeorg.delos.writer.document;

import java.util.Objects;

/**
 * Story-aware selection range.
 *
 * <p>This mirrors {@link SelectionRange}, but prevents accidental selections
 * spanning different story containers. Cross-story selections should be modeled
 * explicitly later if Delos needs them; they should not fall out accidentally
 * from comparing unrelated caret addresses.</p>
 */
public record CaretSelectionRange(
        CaretPosition anchor,
        CaretPosition focus
) {
    public CaretSelectionRange {
        anchor = Objects.requireNonNull(anchor, "anchor");
        focus = Objects.requireNonNull(focus, "focus");
        if (!anchor.sameStoryAs(focus)) {
            throw new IllegalArgumentException("Selection endpoints must be in the same story");
        }
    }

    public StoryPath storyPath() {
        return anchor.storyPath();
    }

    public CaretPosition start() {
        return CaretPosition.minWithinStory(anchor, focus);
    }

    public CaretPosition end() {
        return CaretPosition.maxWithinStory(anchor, focus);
    }

    public boolean isCollapsed() {
        return anchor.compareWithinStory(focus) == 0;
    }
}
