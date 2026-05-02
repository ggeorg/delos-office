package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TableCellStoryPath;
import io.github.ggeorg.delos.writer.layout.ApproximateTextMeasurer;
import io.github.ggeorg.delos.writer.layout.GreedyParagraphLayouter;
import io.github.ggeorg.delos.writer.layout.HitTestResult;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LaidOutImageBlock;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.layout.LaidOutTableBlock;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;
import io.github.ggeorg.delos.writer.layout.PageHitTester;
import io.github.ggeorg.delos.writer.layout.PaginatingDocumentLayoutEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RichBlockHitTestContractTest {
    @Test
    void clickingImageBlockReturnsWholeBlockSelection() {
        Document document = Document.fromBlocks("Images", PageStyle.a4Default(), List.of(
                new ParagraphBlock(Paragraph.of("Before")),
                new ImageBlock("media/photo.png", 120, 80, "photo"),
                new ParagraphBlock(Paragraph.of("After"))
        ));
        LaidOutDocument layout = layout(document);
        LaidOutPage page = layout.pages().getFirst();
        LaidOutImageBlock image = page.blocks().stream()
                .filter(LaidOutImageBlock.class::isInstance)
                .map(LaidOutImageBlock.class::cast)
                .findFirst()
                .orElseThrow();

        HitTestResult hit = new PageHitTester().hitTest(page, image.x() + 4, image.y() + 4);

        assertNotNull(hit.blockSelection());
        assertNull(hit.position());
        assertEquals(1, hit.blockSelection().blockIndex());
    }

    @Test
    void clickingTableCellTextReturnsCellStoryCaret() {
        Document document = Document.fromBlocks("Tables", PageStyle.a4Default(), List.of(
                new ParagraphBlock(Paragraph.of("Before")),
                TableBlock.blank(1, 2),
                new ParagraphBlock(Paragraph.of("After"))
        ));
        LaidOutDocument layout = layout(document);
        LaidOutPage page = layout.pages().getFirst();
        LaidOutTableBlock table = page.blocks().stream()
                .filter(LaidOutTableBlock.class::isInstance)
                .map(LaidOutTableBlock.class::cast)
                .findFirst()
                .orElseThrow();

        HitTestResult hit = new PageHitTester().hitTest(page, table.x() + 4, table.y() + 4);

        assertNull(hit.blockSelection());
        assertNull(hit.position());
        assertNotNull(hit.tableCellSelection());
        assertNotNull(hit.storyPosition());
        assertEquals(1, hit.tableCellSelection().blockIndex());
        assertEquals(0, hit.tableCellSelection().rowIndex());
        assertEquals(0, hit.tableCellSelection().columnIndex());
        TableCellStoryPath storyPath = (TableCellStoryPath) hit.storyPosition().storyPath();
        assertEquals(hit.tableCellSelection().blockIndex(), storyPath.tableBlockIndex());
        assertEquals(0, hit.storyPosition().storyBlockIndex());
        assertEquals(0, hit.storyPosition().offset());
    }

    private static LaidOutDocument layout(Document document) {
        return new PaginatingDocumentLayoutEngine(new GreedyParagraphLayouter(new ApproximateTextMeasurer()))
                .layout(document, LayoutTheme.defaultTheme());
    }
}
