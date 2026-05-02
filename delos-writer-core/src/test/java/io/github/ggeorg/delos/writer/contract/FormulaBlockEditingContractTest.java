package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.FormulaBlock;
import io.github.ggeorg.delos.writer.document.FormulaSourceFormat;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.editor.DocumentEdit;
import io.github.ggeorg.delos.writer.editor.DocumentEditor;
import io.github.ggeorg.delos.writer.editor.EditCommand;
import io.github.ggeorg.delos.writer.session.EditorSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormulaBlockEditingContractTest {
    @Test
    void replacingFormulaBlockUpdatesSourceWithoutChangingDocumentShape() {
        Document document = Document.fromBlocks("Formula", PageStyle.a4Default(), List.of(
                ParagraphBlock.of(Paragraph.of("before")),
                new FormulaBlock("E = mc^2", "old alt"),
                ParagraphBlock.of(Paragraph.of("after"))
        ));

        DocumentEdit edit = new DocumentEditor().replaceBlock(
                document,
                1,
                new FormulaBlock(FormulaSourceFormat.LATEX, "a^2 + b^2 = c^2", "Pythagorean theorem"),
                "Edit Formula"
        );

        assertEquals(3, edit.document().blocks().size());
        FormulaBlock updated = assertInstanceOf(FormulaBlock.class, edit.document().blocks().get(1));
        assertEquals(FormulaSourceFormat.LATEX, updated.sourceFormat());
        assertEquals("a^2 + b^2 = c^2", updated.source());
        assertEquals("Pythagorean theorem", updated.altText());
        assertEquals(List.of("before", "after"), edit.document().paragraphs().stream().map(Paragraph::plainText).toList());
    }

    @Test
    void formulaReplacementIsUndoableThroughTheNormalEditCommandPath() {
        Document original = Document.fromBlocks("Formula", PageStyle.a4Default(), List.of(
                ParagraphBlock.of(Paragraph.of("before")),
                new FormulaBlock("E = mc^2", "old alt"),
                ParagraphBlock.of(Paragraph.of("after"))
        ));
        EditorSession session = new EditorSession(original);
        DocumentEdit edit = new DocumentEditor().replaceBlock(
                session.document(),
                1,
                new FormulaBlock("F = ma", "force equals mass times acceleration"),
                "Edit Formula"
        );

        session.execute(new EditCommand(session, new TextPosition(1, 0), null, edit));

        assertTrue(session.canUndo());
        FormulaBlock edited = assertInstanceOf(FormulaBlock.class, session.document().blocks().get(1));
        assertEquals("F = ma", edited.source());

        session.undo();
        FormulaBlock undone = assertInstanceOf(FormulaBlock.class, session.document().blocks().get(1));
        assertEquals("E = mc^2", undone.source());

        session.redo();
        FormulaBlock redone = assertInstanceOf(FormulaBlock.class, session.document().blocks().get(1));
        assertEquals("F = ma", redone.source());
    }
}
