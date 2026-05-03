package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.BlockSelection;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.editor.DocumentEdit;
import io.github.ggeorg.delos.writer.editor.DocumentEditor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class BlockAwareSelectionBaselineContractTest {
    @Test
    void paragraphSelectionAcrossRichBlockPreservesTheRichBlockToday() {
        Document document = Document.fromBlocks("Selection", PageStyle.a4Default(), List.of(
                ParagraphBlock.of(Paragraph.of("Before")),
                new ImageBlock("media/photo.png", 120.0, 80.0, "photo"),
                ParagraphBlock.of(Paragraph.of("After"))
        ));

        SelectionRange selection = new SelectionRange(
                new TextPosition(0, 3),
                new TextPosition(1, 2)
        );

        DocumentEdit edit = new DocumentEditor().replace(
                document,
                selection,
                new TextPosition(1, 2),
                "",
                "Delete Selection"
        );

        assertEquals(2, edit.document().blocks().size());
        ParagraphBlock mergedParagraph = assertInstanceOf(ParagraphBlock.class, edit.document().blocks().get(0));
        assertEquals("Befter", mergedParagraph.paragraph().plainText());
        assertInstanceOf(ImageBlock.class, edit.document().blocks().get(1));
        assertEquals(List.of("Befter"), edit.document().paragraphs().stream().map(Paragraph::plainText).toList());
    }

    @Test
    void blockSelectionUsesDocumentBlockIndexSpaceForWholeRichBlockDeletion() {
        Document document = Document.fromBlocks("Selection", PageStyle.a4Default(), List.of(
                ParagraphBlock.of(Paragraph.of("Before")),
                new ImageBlock("media/photo.png", 120.0, 80.0, "photo"),
                ParagraphBlock.of(Paragraph.of("After"))
        ));

        BlockSelection imageSelection = new BlockSelection(1);
        DocumentEdit edit = new DocumentEditor().removeBlock(
                document,
                imageSelection.blockIndex(),
                "Delete Image"
        );

        assertEquals(2, edit.document().blocks().size());
        assertEquals(List.of("Before", "After"), edit.document().paragraphs().stream().map(Paragraph::plainText).toList());
        assertEquals(new TextPosition(1, 0), edit.caretPosition());
    }

    @Test
    void paragraphBlockRemovalThroughBlockSelectionIsIgnoredToday() {
        Document document = Document.fromBlocks("Selection", PageStyle.a4Default(), List.of(
                ParagraphBlock.of(Paragraph.of("Before")),
                TableBlock.blank(1, 1),
                ParagraphBlock.of(Paragraph.of("After"))
        ));

        BlockSelection paragraphSelection = new BlockSelection(0);
        DocumentEdit edit = new DocumentEditor().removeBlock(
                document,
                paragraphSelection.blockIndex(),
                "Delete Paragraph Block"
        );

        assertEquals(document, edit.document());
        assertEquals(new TextPosition(0, 0), edit.caretPosition());
    }
}
