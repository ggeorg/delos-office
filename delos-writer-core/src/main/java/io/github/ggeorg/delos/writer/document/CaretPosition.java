package io.github.ggeorg.delos.writer.document;

import java.util.Objects;

/**
 * Container-aware caret/text position.
 *
 * <p>The current {@link TextPosition} is intentionally body-paragraph-only:
 * {@code paragraphIndex + offset}. That cannot scale to table cells, headers,
 * footers, footnotes, captions, or text boxes without adding special fields for
 * every container type.</p>
 *
 * <p>{@code CaretPosition} separates the two concerns:</p>
 * <ul>
 *     <li>{@link #storyPath()} identifies the editable story container.</li>
 *     <li>{@link #storyBlockIndex()} and {@link #offset()} identify a text
 *     position inside that story.</li>
 * </ul>
 *
 * <p>v74 introduces this value object as a foundation. The live editor can be
 * migrated from {@link TextPosition} in a later version.</p>
 */
public record CaretPosition(
        StoryPath storyPath,
        int storyBlockIndex,
        int offset
) {
    public CaretPosition {
        storyPath = Objects.requireNonNull(storyPath, "storyPath");
        if (storyBlockIndex < 0) {
            throw new IllegalArgumentException("storyBlockIndex must be >= 0");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0");
        }
    }

    public static CaretPosition body(int storyBlockIndex, int offset) {
        return new CaretPosition(StoryPath.body(), storyBlockIndex, offset);
    }

    public static CaretPosition tableCell(
            int tableBlockIndex,
            int rowIndex,
            int columnIndex,
            int storyBlockIndex,
            int offset
    ) {
        return new CaretPosition(
                StoryPath.tableCell(tableBlockIndex, rowIndex, columnIndex),
                storyBlockIndex,
                offset
        );
    }

    /**
     * Migration bridge from the old body-only caret model.
     *
     * <p>During migration, the legacy paragraph index is carried as the story
     * block index. Code should only use this for body-story positions. Once the
     * editor stops using paragraph projection, this bridge can be removed.</p>
     */
    public static CaretPosition fromLegacyBodyTextPosition(TextPosition position) {
        Objects.requireNonNull(position, "position");
        return body(position.paragraphIndex(), position.offset());
    }

    /**
     * Migration bridge back to the old body-only caret model.
     */
    public TextPosition toLegacyBodyTextPosition() {
        if (!storyPath.isBody()) {
            throw new IllegalStateException("Only body-story caret positions can be converted to TextPosition");
        }
        return new TextPosition(storyBlockIndex, offset);
    }

    public boolean isBodyStory() {
        return storyPath.isBody();
    }

    public boolean sameStoryAs(CaretPosition other) {
        return other != null && storyPath.equals(other.storyPath);
    }

    /**
     * Compares two positions only when both are inside the same story.
     *
     * <p>There is no universal ordering between the body story and a table-cell
     * story. Selection normalization must stay inside one story unless a future
     * explicit cross-story selection model is introduced.</p>
     */
    public int compareWithinStory(CaretPosition other) {
        Objects.requireNonNull(other, "other");
        if (!sameStoryAs(other)) {
            throw new IllegalArgumentException("Cannot compare caret positions from different stories");
        }

        int blockResult = Integer.compare(storyBlockIndex, other.storyBlockIndex);
        if (blockResult != 0) {
            return blockResult;
        }
        return Integer.compare(offset, other.offset);
    }

    public static CaretPosition minWithinStory(CaretPosition a, CaretPosition b) {
        return a.compareWithinStory(b) <= 0 ? a : b;
    }

    public static CaretPosition maxWithinStory(CaretPosition a, CaretPosition b) {
        return a.compareWithinStory(b) >= 0 ? a : b;
    }
}
