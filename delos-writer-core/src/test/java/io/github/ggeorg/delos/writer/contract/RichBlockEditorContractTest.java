package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.ImageBlock;
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

class RichBlockEditorContractTest {
    @Test
    void textEditsPreserveRichBlocksInDocumentOrder() {
        Document document = Document.fromBlocks("Test", PageStyle.a4Default(), List.of(
                new ParagraphBlock(Paragraph.of("Hello")),
                new ImageBlock("media/photo.png", 120, 80, "photo"),
                new ParagraphBlock(Paragraph.of("World")),
                TableBlock.blank(1, 2)
        ));

        DocumentEdit edit = new DocumentEditor().replace(
                document,
                new TextPosition(0, 5),
                new TextPosition(0, 5),
                "!",
                "Insert punctuation"
        );

        assertEquals(4, edit.document().blocks().size());
        assertInstanceOf(ParagraphBlock.class, edit.document().blocks().get(0));
        assertInstanceOf(ImageBlock.class, edit.document().blocks().get(1));
        assertInstanceOf(ParagraphBlock.class, edit.document().blocks().get(2));
        assertInstanceOf(TableBlock.class, edit.document().blocks().get(3));
        assertEquals("Hello!", edit.document().paragraphs().getFirst().plainText());
        assertEquals("World", edit.document().paragraphs().get(1).plainText());
    }

    @Test
    void removingRichBlockKeepsParagraphProjectionUsable() {
        Document document = Document.fromBlocks("Test", PageStyle.a4Default(), List.of(
                new ParagraphBlock(Paragraph.of("Before")),
                new ImageBlock("media/photo.png", 120, 80, "photo"),
                new ParagraphBlock(Paragraph.of("After"))
        ));

        DocumentEdit edit = new DocumentEditor().removeBlock(document, 1, "Delete Block");

        assertEquals(2, edit.document().blocks().size());
        assertEquals(List.of("Before", "After"), edit.document().paragraphs().stream().map(Paragraph::plainText).toList());
        assertEquals(new TextPosition(1, 0), edit.caretPosition());
    }
}
