package io.github.ggeorg.delos.writer.document;

/**
 * Stable address of an editable story inside a Writer document.
 *
 * <p>The body story is the default editing surface. Table cells expose nested
 * stories so editor operations can target rich in-cell content without
 * flattening table state into the main paragraph list.</p>
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
