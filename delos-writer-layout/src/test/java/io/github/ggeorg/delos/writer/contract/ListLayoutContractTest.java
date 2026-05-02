package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphStyle;
import io.github.ggeorg.delos.writer.document.TextRun;
import io.github.ggeorg.delos.writer.layout.GreedyParagraphLayouter;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LaidOutTextBlock;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;
import io.github.ggeorg.delos.writer.layout.PaginatingDocumentLayoutEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListLayoutContractTest {
    @Test
    void listMarkerIsLayoutMetadataNotParagraphText() {
        Paragraph paragraph = new Paragraph(
                ParagraphStyle.defaultBody().asBulletListItem(0),
                List.of(TextRun.plain("First item"))
        );
        Document document = new Document("Lists", PageStyle.a4Default(), List.of(paragraph));

        LaidOutDocument layout = new PaginatingDocumentLayoutEngine(new GreedyParagraphLayouter())
                .layout(document, LayoutTheme.defaultTheme());

        LaidOutTextBlock block = (LaidOutTextBlock) layout.pages().getFirst().blocks().getFirst();
        assertTrue(block.listMarker().visible());
        assertEquals("•", block.listMarker().text());
        assertEquals("First item", block.lines().getFirst().text());
    }
}
