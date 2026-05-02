package io.github.ggeorg.delos.writer.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaretPositionContractTest {
    @Test
    void bodyCaretPositionCanBridgeFromAndToLegacyBodyTextPosition() {
        TextPosition legacy = new TextPosition(2, 7);

        CaretPosition structured = CaretPosition.fromLegacyBodyTextPosition(legacy);

        assertTrue(structured.isBodyStory());
        assertEquals(StoryPath.body(), structured.storyPath());
        assertEquals(2, structured.storyBlockIndex());
        assertEquals(7, structured.offset());
        assertEquals(legacy, structured.toLegacyBodyTextPosition());
    }

    @Test
    void tableCellCaretPositionCarriesContainerPathAndLocalStoryPosition() {
        CaretPosition position = CaretPosition.tableCell(3, 1, 2, 0, 5);

        assertFalse(position.isBodyStory());
        assertEquals(new TableCellStoryPath(3, 1, 2), position.storyPath());
        assertEquals(0, position.storyBlockIndex());
        assertEquals(5, position.offset());
    }

    @Test
    void nonBodyCaretCannotConvertToLegacyBodyTextPosition() {
        CaretPosition position = CaretPosition.tableCell(3, 1, 2, 0, 5);

        assertThrows(IllegalStateException.class, position::toLegacyBodyTextPosition);
    }

    @Test
    void caretPositionComparesOnlyWithinSameStory() {
        CaretPosition a = CaretPosition.body(1, 3);
        CaretPosition b = CaretPosition.body(1, 5);
        CaretPosition c = CaretPosition.body(2, 0);

        assertTrue(a.sameStoryAs(b));
        assertTrue(a.compareWithinStory(b) < 0);
        assertTrue(c.compareWithinStory(b) > 0);
        assertEquals(a, CaretPosition.minWithinStory(a, b));
        assertEquals(b, CaretPosition.maxWithinStory(a, b));
    }

    @Test
    void caretPositionRejectsCrossStoryComparison() {
        CaretPosition body = CaretPosition.body(0, 0);
        CaretPosition cell = CaretPosition.tableCell(2, 0, 0, 0, 0);

        assertFalse(body.sameStoryAs(cell));
        assertThrows(IllegalArgumentException.class, () -> body.compareWithinStory(cell));
    }

    @Test
    void caretPositionRejectsNegativeIndexes() {
        assertThrows(IllegalArgumentException.class, () -> CaretPosition.body(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> CaretPosition.body(0, -1));
        assertThrows(IllegalArgumentException.class, () -> CaretPosition.tableCell(0, 0, 0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> CaretPosition.tableCell(0, 0, 0, 0, -1));
    }
}
