package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Block;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class DocumentBlockContractTest {

    @Test
    void paragraphDocumentProjectsToParagraphBlocks() {
        Document document = new Document(
                "Blocks",
                PageStyle.a4Default(),
                List.of(Paragraph.of("alpha"), Paragraph.of("beta"))
        );

        List<Block> blocks = document.blocks();

        assertEquals(2, blocks.size());
        ParagraphBlock first = assertInstanceOf(ParagraphBlock.class, blocks.get(0));
        ParagraphBlock second = assertInstanceOf(ParagraphBlock.class, blocks.get(1));
        assertEquals("alpha", first.paragraph().plainText());
        assertEquals("beta", second.paragraph().plainText());
    }

    @Test
    void fromBlocksPreservesMixedRichBlocks() {
        Document document = Document.fromBlocks(
                "Blocks",
                PageStyle.a4Default(),
                List.of(
                        ParagraphBlock.of(Paragraph.of("one")),
                        new ImageBlock("media/image-1.png", 120, 80, "Diagram"),
                        ParagraphBlock.of(Paragraph.of("two"))
                )
        );

        assertEquals(3, document.blocks().size());
        assertInstanceOf(ImageBlock.class, document.blocks().get(1));
        assertEquals(List.of("one", "two"), document.paragraphs().stream().map(Paragraph::plainText).toList());
    }
}
