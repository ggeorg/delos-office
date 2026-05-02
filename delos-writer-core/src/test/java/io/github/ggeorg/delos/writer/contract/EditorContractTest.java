package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.TextRun;
import io.github.ggeorg.delos.writer.session.EditorSession;
import io.github.ggeorg.delos.writer.editor.DocumentEdit;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.editor.DocumentEditor;
import io.github.ggeorg.delos.writer.editor.DocumentFormatter;
import io.github.ggeorg.delos.writer.editor.EditCommand;
import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.editor.TextStyle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorContractTest {
    private final DocumentEditor documentEditor = new DocumentEditor();
    private final DocumentFormatter documentFormatter = new DocumentFormatter();

    @Test
    void replacePreservesContextOutsideEditedRange() {
        Document document = document("aaa", "bbb", "ccc");

        DocumentEdit edit = documentEditor.replace(
                document,
                new TextPosition(1, 1),
                new TextPosition(1, 2),
                "X",
                "Replace selection"
        );

        assertParagraphText(edit.document(), 0, "aaa");
        assertParagraphText(edit.document(), 1, "bXb");
        assertParagraphText(edit.document(), 2, "ccc");
        assertEquals(new TextPosition(1, 2), edit.caretPosition());
        assertNull(edit.selectionRange());
    }

    @Test
    void enterSplitProducesTwoParagraphsAtCorrectOffsets() {
        Document document = document("hello world");

        DocumentEdit edit = documentEditor.replace(
                document,
                new TextPosition(0, 5),
                new TextPosition(0, 5),
                "\n",
                "Split paragraph"
        );

        assertEquals(2, edit.document().paragraphs().size());
        assertParagraphText(edit.document(), 0, "hello");
        assertParagraphText(edit.document(), 1, " world");
        assertEquals(new TextPosition(1, 0), edit.caretPosition());
    }

    @Test
    void toggleBoldPreservesTextAndSelection() {
        Document document = document("abcdef");
        SelectionRange selection = new SelectionRange(new TextPosition(0, 1), new TextPosition(0, 4));
        TextPosition caret = new TextPosition(0, 4);

        DocumentEdit edit = documentFormatter.toggle(
                document,
                selection,
                caret,
                TextStyle.BOLD,
                "Toggle bold"
        );

        assertParagraphText(edit.document(), 0, "abcdef");
        Paragraph paragraph = edit.document().paragraphs().getFirst();
        assertEquals(3, paragraph.runs().size());
        assertEquals("a", paragraph.runs().get(0).text());
        assertFalse(paragraph.runs().get(0).bold());
        assertEquals("bcd", paragraph.runs().get(1).text());
        assertTrue(paragraph.runs().get(1).bold());
        assertEquals("ef", paragraph.runs().get(2).text());
        assertFalse(paragraph.runs().get(2).bold());
        assertEquals(caret, edit.caretPosition());
        assertEquals(selection, edit.selectionRange());
    }

    @Test
    void editCommandRestoresDocumentAndCarriesCaretMetadata() {
        Document original = document("plain text");
        EditorSession session = new EditorSession(original);
        SelectionRange originalSelection = new SelectionRange(new TextPosition(0, 0), new TextPosition(0, 5));

        DocumentEdit edit = documentFormatter.toggle(
                original,
                originalSelection,
                new TextPosition(0, 5),
                TextStyle.BOLD,
                "Toggle bold"
        );

        EditCommand command = new EditCommand(session, new TextPosition(0, 5), originalSelection, edit);
        session.execute(command);

        assertParagraphText(session.document(), 0, "plain text");
        assertTrue(session.document().paragraphs().getFirst().runs().stream().anyMatch(TextRun::bold));
        assertEquals(new TextPosition(0, 5), command.executeCaretPosition());
        assertEquals(originalSelection, command.executeSelectionRange());
        assertTrue(session.canUndo());
        assertFalse(session.canRedo());

        session.undo();
        assertEquals(original, session.document());
        assertEquals(new TextPosition(0, 5), command.undoCaretPosition());
        assertEquals(originalSelection, command.undoSelectionRange());
        assertTrue(session.canRedo());

        session.redo();
        assertParagraphText(session.document(), 0, "plain text");
        assertTrue(session.document().paragraphs().getFirst().runs().stream().anyMatch(TextRun::bold));
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
