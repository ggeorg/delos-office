package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TableCellSelection;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.editor.DocumentEdit;
import io.github.ggeorg.delos.writer.editor.DocumentEditor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class TableCellEditingContractTest {
    @Test
    void replacingTableCellTextUpdatesOnlyTheAddressedCell() {
        Document document = Document.fromBlocks("Table", PageStyle.a4Default(), List.of(
                new ParagraphBlock(Paragraph.of("Before")),
                TableBlock.blank(2, 2),
                new ParagraphBlock(Paragraph.of("After"))
        ));

        DocumentEdit edit = new DocumentEditor().replaceTableCellText(
                document,
                new TableCellSelection(1, 1, 0),
                "hello cell",
                "Edit Table Cell"
        );

        TableBlock table = assertInstanceOf(TableBlock.class, edit.document().blocks().get(1));
        assertEquals("", table.rows().get(0).cells().get(0).paragraphs().getFirst().plainText());
        assertEquals("hello cell", table.rows().get(1).cells().get(0).paragraphs().getFirst().plainText());
        assertEquals("", table.rows().get(1).cells().get(1).paragraphs().getFirst().plainText());
        assertEquals(new TextPosition(1, 0), edit.caretPosition());
    }

    @Test
    void replacingTableCellTextPreservesNewlinesAsCellParagraphs() {
        Document document = Document.fromBlocks("Table", PageStyle.a4Default(), List.of(
                new ParagraphBlock(Paragraph.of("Before")),
                TableBlock.blank(1, 1),
                new ParagraphBlock(Paragraph.of("After"))
        ));

        DocumentEdit edit = new DocumentEditor().replaceTableCellText(
                document,
                new TableCellSelection(1, 0, 0),
                "line one\nline two",
                "Edit Table Cell"
        );

        TableBlock table = assertInstanceOf(TableBlock.class, edit.document().blocks().get(1));
        assertEquals(2, table.rows().getFirst().cells().getFirst().paragraphs().size());
        assertEquals("line one", table.rows().getFirst().cells().getFirst().paragraphs().get(0).plainText());
        assertEquals("line two", table.rows().getFirst().cells().getFirst().paragraphs().get(1).plainText());
        assertEquals("line one\nline two", new DocumentEditor().tableCellPlainText(edit.document(), new TableCellSelection(1, 0, 0)));
    }
}
