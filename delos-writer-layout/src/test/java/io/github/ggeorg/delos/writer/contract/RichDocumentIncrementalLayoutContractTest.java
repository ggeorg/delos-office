package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.writer.document.Block;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.layout.ApproximateTextMeasurer;
import io.github.ggeorg.delos.writer.layout.GreedyParagraphLayouter;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LaidOutLine;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;
import io.github.ggeorg.delos.writer.layout.PaginatingDocumentLayoutEngine;
import io.github.ggeorg.delos.writer.layout.ParagraphLayouter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RichDocumentIncrementalLayoutContractTest {
    private static final LayoutTheme THEME = LayoutTheme.defaultTheme();
    private static final PageStyle SMALL_PAGE = new PageStyle(260.0, 260.0, 20.0, 20.0, 20.0, 20.0);

    @Test
    void stableRichBlocksDoNotDisableParagraphLayoutCacheReuse() {
        CountingParagraphLayouter layouter = new CountingParagraphLayouter();
        PaginatingDocumentLayoutEngine engine = new PaginatingDocumentLayoutEngine(layouter);
        Document original = richImageDocument("After image paragraph 8");
        Document edited = richImageDocument("After image paragraph 8 edited");

        engine.layout(original, THEME);
        layouter.reset();
        LaidOutDocument incremental = engine.layout(edited, THEME);

        LaidOutDocument cold = new PaginatingDocumentLayoutEngine(
                new GreedyParagraphLayouter(new ApproximateTextMeasurer())
        ).layout(edited, THEME);

        assertEquals(cold, incremental);
        assertTrue(
                layouter.calls() < edited.paragraphs().size(),
                "stable rich blocks must not force a cold relayout of every top-level paragraph"
        );
        assertEquals(1, layouter.calls(), "only the edited top-level paragraph should need fresh line layout");
    }

    @Test
    void validationModeAcceptsIncrementalRelayoutWithStableImageBlock() {
        PaginatingDocumentLayoutEngine engine = new PaginatingDocumentLayoutEngine(
                new GreedyParagraphLayouter(new ApproximateTextMeasurer()),
                true
        );
        Document original = richImageDocument("After image paragraph 8");
        Document edited = richImageDocument("After image paragraph 8 edited");

        assertDoesNotThrow(() -> {
            engine.layout(original, THEME);
            engine.layout(edited, THEME);
        });
    }

    @Test
    void validationModeAcceptsIncrementalRelayoutWithStableTableBlock() {
        PaginatingDocumentLayoutEngine engine = new PaginatingDocumentLayoutEngine(
                new GreedyParagraphLayouter(new ApproximateTextMeasurer()),
                true
        );
        Document original = richTableDocument("After table paragraph 8");
        Document edited = richTableDocument("After table paragraph 8 edited");

        assertDoesNotThrow(() -> {
            engine.layout(original, THEME);
            engine.layout(edited, THEME);
        });
    }

    private static Document richImageDocument(String changedParagraphText) {
        List<Block> blocks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            blocks.add(ParagraphBlock.of(Paragraph.of("Before image paragraph " + i + " keeps the document flowing across pages.")));
        }
        blocks.add(new ImageBlock("media/sample.png", 120.0, 70.0, "sample"));
        for (int i = 0; i < 12; i++) {
            String text = i == 8
                    ? changedParagraphText
                    : "After image paragraph " + i + " remains unchanged for cache reuse.";
            blocks.add(ParagraphBlock.of(Paragraph.of(text)));
        }
        return Document.fromBlocks("Rich image", SMALL_PAGE, blocks);
    }

    private static Document richTableDocument(String changedParagraphText) {
        List<Block> blocks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            blocks.add(ParagraphBlock.of(Paragraph.of("Before table paragraph " + i + " keeps the document flowing across pages.")));
        }
        blocks.add(TableBlock.blank(2, 2));
        for (int i = 0; i < 12; i++) {
            String text = i == 8
                    ? changedParagraphText
                    : "After table paragraph " + i + " remains unchanged for cache reuse.";
            blocks.add(ParagraphBlock.of(Paragraph.of(text)));
        }
        return Document.fromBlocks("Rich table", SMALL_PAGE, blocks);
    }

    private static final class CountingParagraphLayouter implements ParagraphLayouter {
        private final ParagraphLayouter delegate = new GreedyParagraphLayouter(new ApproximateTextMeasurer());
        private int calls;

        @Override
        public List<LaidOutLine> layoutLines(Paragraph paragraph, RenderFont baseFont, double maxWidth, double lineGap) {
            calls += 1;
            return delegate.layoutLines(paragraph, baseFont, maxWidth, lineGap);
        }

        int calls() {
            return calls;
        }

        void reset() {
            calls = 0;
        }
    }
}
