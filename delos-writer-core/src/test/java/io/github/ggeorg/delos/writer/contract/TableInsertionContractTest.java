package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.editor.DocumentEdit;
import io.github.ggeorg.delos.writer.editor.DocumentEditor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class TableInsertionContractTest {
    @Test
    void insertingTableInMiddleOfParagraphSplitsParagraphAroundTable() {
        Document document = new Document("Test", PageStyle.a4Default(), List.of(Paragraph.of("HelloWorld")));

        DocumentEdit edit = new DocumentEditor().insertBlock(
                document,
                new TextPosition(0, 5),
                TableBlock.blank(2, 3),
                List.of(),
                "Insert Table"
        );

        assertEquals(3, edit.document().blocks().size());
        assertEquals("Hello", ((ParagraphBlock) edit.document().blocks().get(0)).paragraph().plainText());
        TableBlock table = assertInstanceOf(TableBlock.class, edit.document().blocks().get(1));
        assertEquals(2, table.rowCount());
        assertEquals(3, table.columnCount());
        assertEquals("World", ((ParagraphBlock) edit.document().blocks().get(2)).paragraph().plainText());
        assertEquals(List.of(), edit.document().mediaItems());
        assertEquals(new TextPosition(1, 0), edit.caretPosition());
    }

    @Test
    void insertingTableAtEndOfLastParagraphAddsEditableParagraphAfterTable() {
        Document document = new Document("Test", PageStyle.a4Default(), List.of(Paragraph.of("Hello")));

        DocumentEdit edit = new DocumentEditor().insertBlock(
                document,
                new TextPosition(0, 5),
                TableBlock.blank(1, 2),
                List.of(),
                "Insert Table"
        );

        assertEquals(3, edit.document().blocks().size());
        assertInstanceOf(TableBlock.class, edit.document().blocks().get(1));
        assertEquals("", ((ParagraphBlock) edit.document().blocks().get(2)).paragraph().plainText());
        assertEquals(new TextPosition(1, 0), edit.caretPosition());
    }
}
