package io.github.ggeorg.delos.writer.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoryPathContractTest {
    @Test
    void bodyPathIdentifiesDocumentBodyStory() {
        StoryPath path = StoryPath.body();

        assertInstanceOf(BodyStoryPath.class, path);
        assertTrue(path.isBody());
        assertFalse(path.isTableCell());
        assertEquals("body", path.toString());
    }

    @Test
    void tableCellPathRoundTripsWithCurrentTableCellSelectionAddress() {
        TableCellSelection selection = new TableCellSelection(4, 1, 2);

        TableCellStoryPath path = TableCellStoryPath.from(selection);

        assertFalse(path.isBody());
        assertTrue(path.isTableCell());
        assertEquals(4, path.tableBlockIndex());
        assertEquals(1, path.rowIndex());
        assertEquals(2, path.columnIndex());
        assertEquals(selection, path.toTableCellSelection());
    }

    @Test
    void tableCellPathRejectsNegativeCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> StoryPath.tableCell(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> StoryPath.tableCell(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> StoryPath.tableCell(0, 0, -1));
    }
}
