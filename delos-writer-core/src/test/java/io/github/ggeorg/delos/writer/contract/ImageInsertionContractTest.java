package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.DocumentMediaItem;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.editor.DocumentEdit;
import io.github.ggeorg.delos.writer.editor.DocumentEditor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class ImageInsertionContractTest {
    @Test
    void insertingImageInMiddleOfParagraphSplitsParagraphAndKeepsMediaSeparate() {
        Document document = new Document("Test", PageStyle.a4Default(), List.of(Paragraph.of("HelloWorld")));
        DocumentMediaItem mediaItem = DocumentMediaItem.image("media/image-1.png", "image/png", new byte[]{1, 2, 3});

        DocumentEdit edit = new DocumentEditor().insertBlock(
                document,
                new TextPosition(0, 5),
                new ImageBlock(mediaItem.path(), 120, 80, "image-1.png"),
                List.of(mediaItem),
                "Insert Image"
        );

        assertEquals(3, edit.document().blocks().size());
        assertEquals("Hello", ((ParagraphBlock) edit.document().blocks().get(0)).paragraph().plainText());
        ImageBlock image = assertInstanceOf(ImageBlock.class, edit.document().blocks().get(1));
        assertEquals("media/image-1.png", image.source());
        assertEquals("World", ((ParagraphBlock) edit.document().blocks().get(2)).paragraph().plainText());
        assertEquals(List.of(mediaItem), edit.document().mediaItems());
        assertEquals(new TextPosition(1, 0), edit.caretPosition());
    }

    @Test
    void insertingImageAtEndOfLastParagraphAddsEditableParagraphAfterImage() {
        Document document = new Document("Test", PageStyle.a4Default(), List.of(Paragraph.of("Hello")));
        DocumentMediaItem mediaItem = DocumentMediaItem.image("media/image-1.png", "image/png", new byte[]{1});

        DocumentEdit edit = new DocumentEditor().insertBlock(
                document,
                new TextPosition(0, 5),
                new ImageBlock(mediaItem.path(), 120, 80, "image-1.png"),
                List.of(mediaItem),
                "Insert Image"
        );

        assertEquals(3, edit.document().blocks().size());
        assertInstanceOf(ImageBlock.class, edit.document().blocks().get(1));
        assertEquals("", ((ParagraphBlock) edit.document().blocks().get(2)).paragraph().plainText());
        assertEquals(new TextPosition(1, 0), edit.caretPosition());
    }
}
