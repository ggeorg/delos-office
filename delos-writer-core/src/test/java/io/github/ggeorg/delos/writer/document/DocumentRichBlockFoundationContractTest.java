package io.github.ggeorg.delos.writer.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DocumentRichBlockFoundationContractTest {
    @Test
    void paragraphDocumentsExposeBlockProjection() {
        Document document = new Document("Blocks", PageStyle.a4Default(), List.of(Paragraph.of("alpha"), Paragraph.of("beta")));

        List<Block> blocks = document.blocks();

        assertEquals(2, blocks.size());
        assertEquals(BlockKind.PARAGRAPH, blocks.getFirst().kind());
        assertEquals(BlockKind.PARAGRAPH, blocks.getLast().kind());
    }

    @Test
    void transitionalFactoryAcceptsParagraphBlocksForLiveEditorProjection() {
        Document document = Document.fromBlocks("Paragraphs", PageStyle.a4Default(), List.of(
                ParagraphBlock.of(Paragraph.of("alpha")),
                ParagraphBlock.of(Paragraph.of("beta"))
        ));

        assertEquals(2, document.paragraphs().size());
        assertEquals("alpha", document.paragraphs().getFirst().plainText());
    }

    @Test
    void blockFactoryPreservesImageBlocksWithoutDroppingParagraphProjection() {
        Document document = Document.fromBlocks(
                "Rich",
                PageStyle.a4Default(),
                List.<Block>of(ParagraphBlock.of(Paragraph.of("alpha")), new ImageBlock("media/image.png", 120, 80))
        );

        assertEquals(2, document.blocks().size());
        assertEquals(BlockKind.IMAGE, document.blocks().getLast().kind());
        assertEquals(1, document.paragraphs().size());
    }

    @Test
    void tableFoundationUsesParagraphsInsideCells() {
        TableBlock table = TableBlock.blank(2, 3);

        assertEquals(BlockKind.TABLE, table.kind());
        assertEquals(2, table.rowCount());
        assertEquals(3, table.columnCount());
        assertEquals(1, table.rows().getFirst().cells().getFirst().paragraphs().size());
    }
}
