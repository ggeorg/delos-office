package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphStyle;
import io.github.ggeorg.delos.writer.layout.BlockRole;
import io.github.ggeorg.delos.writer.layout.GreedyParagraphLayouter;
import io.github.ggeorg.delos.writer.layout.LaidOutBlock;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.layout.LaidOutTextBlock;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;
import io.github.ggeorg.delos.writer.layout.PaginatingDocumentLayoutEngine;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IncrementalListLayoutCorrectnessTest {
    private static final LayoutTheme THEME = LayoutTheme.defaultTheme();
    private static final PageStyle SMALL_PAGE = new PageStyle(260.0, 240.0, 20.0, 20.0, 20.0, 20.0);

    @Test
    void numberedListMarkersSurviveIncrementalRelayoutFromMiddleOfLongList() {
        PaginatingDocumentLayoutEngine engine = new PaginatingDocumentLayoutEngine(new GreedyParagraphLayouter(), true);
        Document original = numberedListDocument("Item");
        Document edited = numberedListDocument("Edited item");

        assertDoesNotThrow(() -> {
            engine.layout(original, THEME);
            engine.layout(edited, THEME);
        });
    }

    @Test
    void incrementalNumberedMarkersMatchTheirParagraphOrdinalAfterMiddleEdit() {
        PaginatingDocumentLayoutEngine engine = new PaginatingDocumentLayoutEngine(new GreedyParagraphLayouter());
        Document original = numberedListDocument("Item");
        Document edited = numberedListDocument("Edited item");

        engine.layout(original, THEME);
        LaidOutDocument incremental = engine.layout(edited, THEME);

        Map<Integer, String> markers = visibleMarkersByParagraph(incremental);
        assertEquals("1.", markers.get(0));
        assertEquals("20.", markers.get(19));
        assertEquals("31.", markers.get(30));
        assertEquals("50.", markers.get(49));
    }

    private static Document numberedListDocument(String changedPrefix) {
        ParagraphStyle numbered = ParagraphStyle.defaultBody().asNumberedListItem(0, 1);
        List<Paragraph> paragraphs = IntStream.range(0, 50)
                .mapToObj(index -> Paragraph.of(numbered, (index == 30 ? changedPrefix : "Item") + " " + (index + 1)))
                .toList();
        return new Document("Numbered List", SMALL_PAGE, paragraphs);
    }

    private static Map<Integer, String> visibleMarkersByParagraph(LaidOutDocument document) {
        Map<Integer, String> markers = new HashMap<>();
        for (LaidOutPage page : document.pages()) {
            for (LaidOutBlock block : page.blocks()) {
                if (block instanceof LaidOutTextBlock textBlock
                        && textBlock.role() == BlockRole.BODY
                        && textBlock.listMarker().visible()) {
                    markers.put(textBlock.sourceParagraphIndex(), textBlock.listMarker().text());
                }
            }
        }
        return markers;
    }
}
