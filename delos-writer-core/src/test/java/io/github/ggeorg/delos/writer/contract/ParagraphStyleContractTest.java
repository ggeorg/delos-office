package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Alignment;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphStyle;
import io.github.ggeorg.delos.writer.editor.DocumentEdit;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.editor.DocumentEditor;
import io.github.ggeorg.delos.writer.editor.DocumentFormatter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParagraphStyleContractTest {

    @Test
    void alignParagraphs_updatesCaretParagraphWhenSelectionCollapsed() {
        Document document = new Document(
                "Test",
                PageStyle.a4Default(),
                List.of(
                        Paragraph.of("First"),
                        Paragraph.of("Second")
                )
        );

        DocumentFormatter formatter = new DocumentFormatter();
        DocumentEdit edit = formatter.alignParagraphs(
                document,
                null,
                new TextPosition(1, 2),
                Alignment.CENTER,
                "Align Paragraph center"
        );

        assertEquals(Alignment.LEFT, edit.document().paragraphs().get(0).style().alignment());
        assertEquals(Alignment.CENTER, edit.document().paragraphs().get(1).style().alignment());
        assertEquals(new TextPosition(1, 2), edit.caretPosition());
    }

    @Test
    void splitParagraph_preservesParagraphStyleOnBothSides() {
        ParagraphStyle style = ParagraphStyle.defaultBody()
                .withAlignment(Alignment.RIGHT)
                .withFirstLineIndent(18);
        Document document = new Document(
                "Test",
                PageStyle.a4Default(),
                List.of(Paragraph.of(style, "abcdef"))
        );

        DocumentEditor editor = new DocumentEditor();
        DocumentEdit edit = editor.replace(
                document,
                new TextPosition(0, 3),
                new TextPosition(0, 3),
                "\n",
                "Split Paragraph"
        );

        assertEquals(2, edit.document().paragraphs().size());
        assertEquals(style, edit.document().paragraphs().get(0).style());
        assertEquals(style, edit.document().paragraphs().get(1).style());
        assertEquals("abc", edit.document().paragraphs().get(0).plainText());
        assertEquals("def", edit.document().paragraphs().get(1).plainText());
    }
}
