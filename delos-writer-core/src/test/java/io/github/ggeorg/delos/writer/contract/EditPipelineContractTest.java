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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditPipelineContractTest {
    private final DocumentEditor editor = new DocumentEditor();
    private final DocumentFormatter formatter = new DocumentFormatter();

    @Test
    void replaceEditCommandExecuteUndoRedoRoundTripsDocumentState() {
        Document original = document("hello world");
        EditorSession session = new EditorSession(original);

        DocumentEdit edit = editor.replace(original, new TextPosition(0, 5), new TextPosition(0, 5), "!", "Insert bang");
        EditCommand command = new EditCommand(session, new TextPosition(0, 5), null, edit);
        session.execute(command);

        assertParagraphText(session.document(), 0, "hello! world");
        assertEquals(new TextPosition(0, 6), command.executeCaretPosition());
        assertNull(command.executeSelectionRange());

        session.undo();
        assertEquals(original, session.document());
        assertEquals(new TextPosition(0, 5), command.undoCaretPosition());
        assertNull(command.undoSelectionRange());

        session.redo();
        assertParagraphText(session.document(), 0, "hello! world");
        assertEquals(new TextPosition(0, 6), command.executeCaretPosition());
        assertNull(command.executeSelectionRange());
    }

    @Test
    void splittingStyledParagraphAtStartPreservesOriginalTextAfterInsertedBlankParagraphs() {
        Document original = new Document(
                "Untitled",
                PageStyle.a4Default(),
                List.of(new Paragraph(List.of(
                        new TextRun("Delos Writer ", true, false, false),
                        new TextRun("now supports styled runs", false, true, false)
                )))
        );

        DocumentEdit firstSplit = editor.replace(
                original,
                new TextPosition(0, 0),
                new TextPosition(0, 0),
                "\n",
                "Split Paragraph"
        );
        DocumentEdit secondSplit = editor.replace(
                firstSplit.document(),
                firstSplit.caretPosition(),
                firstSplit.caretPosition(),
                "\n",
                "Split Paragraph"
        );

        assertEquals(3, secondSplit.document().paragraphs().size());
        assertParagraphText(secondSplit.document(), 0, "");
        assertParagraphText(secondSplit.document(), 1, "");
        assertParagraphText(secondSplit.document(), 2, "Delos Writer now supports styled runs");
        assertTrue(secondSplit.document().paragraphs().get(2).runs().getFirst().bold());
        assertTrue(secondSplit.document().paragraphs().get(2).runs().get(1).italic());
        assertEquals(new TextPosition(2, 0), secondSplit.caretPosition());
    }

    @Test
    void formatEditCommandExecuteUndoRedoCarriesSelectionMetadata() {
        Document original = document("format me");
        EditorSession session = new EditorSession(original);
        SelectionRange selection = new SelectionRange(new TextPosition(0, 0), new TextPosition(0, 6));

        DocumentEdit edit = formatter.toggle(original, selection, new TextPosition(0, 6), TextStyle.ITALIC, "Toggle italic");
        EditCommand command = new EditCommand(session, new TextPosition(0, 6), selection, edit);
        session.execute(command);

        assertParagraphText(session.document(), 0, "format me");
        assertTrue(session.document().paragraphs().get(0).runs().stream().anyMatch(run -> run.italic()));
        assertEquals(new TextPosition(0, 6), command.executeCaretPosition());
        assertEquals(selection, command.executeSelectionRange());

        session.undo();
        assertEquals(original, session.document());
        assertEquals(new TextPosition(0, 6), command.undoCaretPosition());
        assertEquals(selection, command.undoSelectionRange());

        session.redo();
        assertTrue(session.document().paragraphs().get(0).runs().stream().anyMatch(run -> run.italic()));
        assertEquals(selection, command.executeSelectionRange());
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
