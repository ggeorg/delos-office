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
import io.github.ggeorg.delos.writer.editor.EditCommand;
import io.github.ggeorg.delos.writer.session.EditorSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageBlockEditingContractTest {
    @Test
    void replacingImageBlockUpdatesPlacementPropertiesWithoutChangingMediaBytes() {
        DocumentMediaItem mediaItem = DocumentMediaItem.image("media/image-1.png", "image/png", new byte[] {1, 2, 3});
        Document document = Document.fromBlocks("Image", PageStyle.a4Default(), List.of(
                ParagraphBlock.of(Paragraph.of("before")),
                new ImageBlock("media/image-1.png", 120.0, 80.0, "old alt"),
                ParagraphBlock.of(Paragraph.of("after"))
        ), List.of(mediaItem));

        DocumentEdit edit = new DocumentEditor().replaceBlock(
                document,
                1,
                new ImageBlock("media/image-1.png", 240.0, 160.0, "diagram alt text"),
                "Edit Image Properties"
        );

        assertEquals(3, edit.document().blocks().size());
        ImageBlock updated = assertInstanceOf(ImageBlock.class, edit.document().blocks().get(1));
        assertEquals("media/image-1.png", updated.source());
        assertEquals(240.0, updated.width());
        assertEquals(160.0, updated.height());
        assertEquals("diagram alt text", updated.altText());

        assertEquals(1, edit.document().mediaItems().size());
        assertEquals(mediaItem, edit.document().mediaItems().getFirst());
        assertArrayEquals(new byte[] {1, 2, 3}, edit.document().mediaItems().getFirst().bytes());
        assertEquals(List.of("before", "after"), edit.document().paragraphs().stream().map(Paragraph::plainText).toList());
    }

    @Test
    void imagePropertyReplacementIsUndoableThroughTheNormalEditCommandPath() {
        Document original = Document.fromBlocks("Image", PageStyle.a4Default(), List.of(
                ParagraphBlock.of(Paragraph.of("before")),
                new ImageBlock("media/image-1.png", 120.0, 80.0, "old alt"),
                ParagraphBlock.of(Paragraph.of("after"))
        ), List.of(DocumentMediaItem.image("media/image-1.png", "image/png", new byte[] {7, 8, 9})));
        EditorSession session = new EditorSession(original);
        DocumentEdit edit = new DocumentEditor().replaceBlock(
                session.document(),
                1,
                new ImageBlock("media/image-1.png", 300.0, 200.0, "new alt"),
                "Edit Image Properties"
        );

        session.execute(new EditCommand(session, new TextPosition(1, 0), null, edit));

        assertTrue(session.canUndo());
        ImageBlock edited = assertInstanceOf(ImageBlock.class, session.document().blocks().get(1));
        assertEquals(300.0, edited.width());
        assertEquals("new alt", edited.altText());

        session.undo();
        ImageBlock undone = assertInstanceOf(ImageBlock.class, session.document().blocks().get(1));
        assertEquals(120.0, undone.width());
        assertEquals("old alt", undone.altText());

        session.redo();
        ImageBlock redone = assertInstanceOf(ImageBlock.class, session.document().blocks().get(1));
        assertEquals(300.0, redone.width());
        assertEquals("new alt", redone.altText());
    }
}
