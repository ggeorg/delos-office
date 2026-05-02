package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TableCellSelection;
import io.github.ggeorg.delos.writer.editor.TableCellNavigator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TableCellNavigatorContractTest {
    private final TableCellNavigator navigator = new TableCellNavigator();

    @Test
    void tabOrderMovesRowMajorThroughTheTable() {
        Document document = documentWithTable(2, 2);

        assertEquals(new TableCellSelection(1, 0, 1), navigator.nextCell(document, new TableCellSelection(1, 0, 0)));
        assertEquals(new TableCellSelection(1, 1, 0), navigator.nextCell(document, new TableCellSelection(1, 0, 1)));
        assertEquals(new TableCellSelection(1, 1, 1), navigator.nextCell(document, new TableCellSelection(1, 1, 0)));
        assertNull(navigator.nextCell(document, new TableCellSelection(1, 1, 1)));
    }

    @Test
    void shiftTabOrderMovesBackwardThroughTheTable() {
        Document document = documentWithTable(2, 2);

        assertEquals(new TableCellSelection(1, 1, 0), navigator.previousCell(document, new TableCellSelection(1, 1, 1)));
        assertEquals(new TableCellSelection(1, 0, 1), navigator.previousCell(document, new TableCellSelection(1, 1, 0)));
        assertEquals(new TableCellSelection(1, 0, 0), navigator.previousCell(document, new TableCellSelection(1, 0, 1)));
        assertNull(navigator.previousCell(document, new TableCellSelection(1, 0, 0)));
    }

    @Test
    void arrowNavigationMovesOnlyToAdjacentCells() {
        Document document = documentWithTable(3, 3);
        TableCellSelection middle = new TableCellSelection(1, 1, 1);

        assertEquals(new TableCellSelection(1, 1, 0), navigator.leftCell(document, middle));
        assertEquals(new TableCellSelection(1, 1, 2), navigator.rightCell(document, middle));
        assertEquals(new TableCellSelection(1, 0, 1), navigator.aboveCell(document, middle));
        assertEquals(new TableCellSelection(1, 2, 1), navigator.belowCell(document, middle));
    }

    @Test
    void invalidOrBoundaryMovesReturnNull() {
        Document document = documentWithTable(1, 1);
        TableCellSelection onlyCell = new TableCellSelection(1, 0, 0);

        assertTrue(navigator.isValid(document, onlyCell));
        assertNull(navigator.leftCell(document, onlyCell));
        assertNull(navigator.rightCell(document, onlyCell));
        assertNull(navigator.aboveCell(document, onlyCell));
        assertNull(navigator.belowCell(document, onlyCell));
        assertNull(navigator.nextCell(document, onlyCell));
        assertNull(navigator.previousCell(document, onlyCell));
        assertNull(navigator.nextCell(document, new TableCellSelection(0, 0, 0)));
    }

    private static Document documentWithTable(int rows, int columns) {
        return Document.fromBlocks("Table", PageStyle.a4Default(), List.of(
                new ParagraphBlock(Paragraph.of("Before")),
                TableBlock.blank(rows, columns),
                new ParagraphBlock(Paragraph.of("After"))
        ));
    }
}
