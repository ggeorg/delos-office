package io.github.ggeorg.delos.writer.document;

/**
 * Address of a reusable editable story container.
 *
 * <p>A document has many possible stories over time: the main body, table cells,
 * headers, footers, footnotes, text boxes, and captions. A caret position should
 * not encode every container kind directly. Instead, it points at a {@code StoryPath}
 * and then at a block/offset inside that story.</p>
 *
 * <p>v74 introduces the address model only. The existing editor pipeline still
 * uses {@link TextPosition} until the StoryPath migration is wired in later.</p>
 */
public sealed interface StoryPath permits BodyStoryPath, TableCellStoryPath {
    /**
     * Returns the path for the document body story.
     */
    static BodyStoryPath body() {
        return BodyStoryPath.INSTANCE;
    }

    /**
     * Returns the path for a table-cell story in top-level document block space.
     */
    static TableCellStoryPath tableCell(int tableBlockIndex, int rowIndex, int columnIndex) {
        return new TableCellStoryPath(tableBlockIndex, rowIndex, columnIndex);
    }

    /**
     * True when this path points at the document body story.
     */
    boolean isBody();

    /**
     * True when this path points at a table-cell story.
     */
    default boolean isTableCell() {
        return this instanceof TableCellStoryPath;
    }
}
