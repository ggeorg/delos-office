package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TableCellSelection;
import io.github.ggeorg.delos.writer.editor.DocumentEdit;
import io.github.ggeorg.delos.writer.editor.DocumentEditor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class TableEditingOperationsContractTest {
    private final DocumentEditor editor = new DocumentEditor();

    @Test
    void insertsAndDeletesRowsWithoutFlatteningTheTableModel() {
        Document document = tableDocument(2, 2);
        TableCellSelection cell = new TableCellSelection(1, 0, 1);

        DocumentEdit inserted = editor.insertTableRow(document, cell, true, "Insert Row Below");
        TableBlock afterInsert = tableAt(inserted.document());
        assertEquals(3, afterInsert.rowCount());
        assertEquals(2, afterInsert.columnCount());

        DocumentEdit deleted = editor.deleteTableRow(inserted.document(), new TableCellSelection(1, 1, 1), "Delete Row");
        TableBlock afterDelete = tableAt(deleted.document());
        assertEquals(2, afterDelete.rowCount());
        assertEquals(2, afterDelete.columnCount());
    }

    @Test
    void insertsAndDeletesColumnsWithoutFlatteningTheTableModel() {
        Document document = tableDocument(2, 2);
        TableCellSelection cell = new TableCellSelection(1, 1, 0);

        DocumentEdit inserted = editor.insertTableColumn(document, cell, true, "Insert Column Right");
        TableBlock afterInsert = tableAt(inserted.document());
        assertEquals(2, afterInsert.rowCount());
        assertEquals(3, afterInsert.columnCount());
        assertEquals(3, afterInsert.columns().size());

        DocumentEdit deleted = editor.deleteTableColumn(inserted.document(), new TableCellSelection(1, 1, 1), "Delete Column");
        TableBlock afterDelete = tableAt(deleted.document());
        assertEquals(2, afterDelete.rowCount());
        assertEquals(2, afterDelete.columnCount());
        assertEquals(2, afterDelete.columns().size());
    }

    @Test
    void updatesHeaderRowAndTableStyleAsModelMetadata() {
        Document document = tableDocument(2, 2);
        TableCellSelection cell = new TableCellSelection(1, 0, 0);

        DocumentEdit header = editor.setTableHeaderRow(document, cell, true, "Header Row");
        TableBlock withHeader = tableAt(header.document());
        assertEquals(1, withHeader.headerRowCount());

        DocumentEdit styled = editor.updateTableStyle(header.document(), cell, 0.75, 12.0, false, "Edit Table Properties");
        TableBlock table = tableAt(styled.document());
        assertEquals(0.75, table.style().widthFraction());
        assertEquals(12.0, table.style().cellPadding());
        assertFalse(table.style().bordersEnabled());
    }

    private static Document tableDocument(int rows, int columns) {
        return Document.fromBlocks("Table", PageStyle.a4Default(), List.of(
                new ParagraphBlock(Paragraph.of("Before")),
                TableBlock.blank(rows, columns),
                new ParagraphBlock(Paragraph.of("After"))
        ));
    }

    private static TableBlock tableAt(Document document) {
        return assertInstanceOf(TableBlock.class, document.blocks().get(1));
    }
}
