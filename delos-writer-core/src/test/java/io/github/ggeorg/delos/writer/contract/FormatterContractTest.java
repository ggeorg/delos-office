package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.editor.DocumentEdit;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.editor.DocumentFormatter;
import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.editor.TextStyle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormatterContractTest {
    private final DocumentFormatter formatter = new DocumentFormatter();

    @Test
    void collapsedSelectionProducesNoOpEdit() {
        Document document = document("abcdef");
        TextPosition caret = new TextPosition(0, 3);
        SelectionRange collapsed = new SelectionRange(caret, caret);

        DocumentEdit edit = formatter.toggle(document, collapsed, caret, TextStyle.BOLD, "Toggle bold");

        assertSame(document, edit.document());
        assertEquals(caret, edit.caretPosition());
        assertEquals(collapsed, edit.selectionRange());
    }

    @Test
    void toggleUnderlineAcrossParagraphsPreservesTextAndSelection() {
        Document document = document("alpha", "beta", "gamma");
        SelectionRange selection = new SelectionRange(new TextPosition(0, 2), new TextPosition(2, 2));
        TextPosition caret = new TextPosition(2, 2);

        DocumentEdit edit = formatter.toggle(document, selection, caret, TextStyle.UNDERLINE, "Toggle underline");

        assertParagraphText(edit.document(), 0, "alpha");
        assertParagraphText(edit.document(), 1, "beta");
        assertParagraphText(edit.document(), 2, "gamma");
        assertEquals(caret, edit.caretPosition());
        assertEquals(selection, edit.selectionRange());

        Paragraph p0 = edit.document().paragraphs().get(0);
        Paragraph p1 = edit.document().paragraphs().get(1);
        Paragraph p2 = edit.document().paragraphs().get(2);

        assertEquals(2, p0.runs().size());
        assertTrue(!p0.runs().get(0).underline());
        assertTrue(p0.runs().get(1).underline());

        assertEquals(1, p1.runs().size());
        assertTrue(p1.runs().get(0).underline());

        assertEquals(2, p2.runs().size());
        assertTrue(p2.runs().get(0).underline());
        assertTrue(!p2.runs().get(1).underline());
    }

    private static Document document(String... paragraphs) {
        return new Document(
                "Test",
                PageStyle.a4Default(),
                List.of(paragraphs).stream().map(Paragraph::of).toList()
        );
    }

    private static void assertParagraphText(Document document, int index, String expected) {
        assertEquals(expected, document.paragraphs().get(index).plainText());
    }
}
