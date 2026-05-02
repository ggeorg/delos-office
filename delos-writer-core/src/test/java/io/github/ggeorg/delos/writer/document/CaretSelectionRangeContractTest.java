package io.github.ggeorg.delos.writer.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaretSelectionRangeContractTest {
    @Test
    void selectionNormalizesWithinOneStory() {
        CaretPosition anchor = CaretPosition.body(2, 10);
        CaretPosition focus = CaretPosition.body(1, 4);

        CaretSelectionRange range = new CaretSelectionRange(anchor, focus);

        assertEquals(StoryPath.body(), range.storyPath());
        assertEquals(focus, range.start());
        assertEquals(anchor, range.end());
        assertFalse(range.isCollapsed());
    }

    @Test
    void selectionCanExistInsideTableCellStory() {
        CaretPosition anchor = CaretPosition.tableCell(5, 1, 1, 0, 2);
        CaretPosition focus = CaretPosition.tableCell(5, 1, 1, 0, 6);

        CaretSelectionRange range = new CaretSelectionRange(anchor, focus);

        assertEquals(new TableCellStoryPath(5, 1, 1), range.storyPath());
        assertEquals(anchor, range.start());
        assertEquals(focus, range.end());
    }

    @Test
    void collapsedSelectionIsRecognized() {
        CaretPosition position = CaretPosition.body(0, 0);

        CaretSelectionRange range = new CaretSelectionRange(position, position);

        assertTrue(range.isCollapsed());
        assertEquals(position, range.start());
        assertEquals(position, range.end());
    }

    @Test
    void selectionRejectsEndpointsFromDifferentStories() {
        CaretPosition body = CaretPosition.body(0, 0);
        CaretPosition cell = CaretPosition.tableCell(2, 0, 0, 0, 0);

        assertThrows(IllegalArgumentException.class, () -> new CaretSelectionRange(body, cell));
    }
}
