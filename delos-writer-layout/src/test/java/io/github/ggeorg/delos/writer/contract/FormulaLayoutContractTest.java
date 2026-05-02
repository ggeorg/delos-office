package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.FormulaBlock;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.layout.ApproximateTextMeasurer;
import io.github.ggeorg.delos.writer.layout.GreedyParagraphLayouter;
import io.github.ggeorg.delos.writer.layout.HitTestResult;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LaidOutFormulaBlock;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;
import io.github.ggeorg.delos.writer.layout.PageHitTester;
import io.github.ggeorg.delos.writer.layout.PaginatingDocumentLayoutEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormulaLayoutContractTest {
    @Test
    void laysOutFormulaAsAtomicBlockInPageFlow() {
        Document document = Document.fromBlocks("Formula", PageStyle.a4Default(), List.of(
                new ParagraphBlock(Paragraph.of("Before")),
                new FormulaBlock("E = mc^2", "mass energy"),
                new ParagraphBlock(Paragraph.of("After"))
        ));

        LaidOutPage page = layout(document).pages().getFirst();

        LaidOutFormulaBlock formula = assertInstanceOf(LaidOutFormulaBlock.class, page.blocks().get(1));
        assertEquals(1, formula.sourceBlockIndex());
        assertEquals("latex", formula.sourceFormat());
        assertEquals("E = mc^2", formula.source());
        assertEquals("mass energy", formula.altText());
        assertTrue(formula.width() > 0.0);
        assertTrue(formula.height() >= 56.0);
    }

    @Test
    void clickingFormulaBlockReturnsWholeBlockSelection() {
        Document document = Document.fromBlocks("Formula", PageStyle.a4Default(), List.of(
                new ParagraphBlock(Paragraph.of("Before")),
                new FormulaBlock("E = mc^2"),
                new ParagraphBlock(Paragraph.of("After"))
        ));
        LaidOutPage page = layout(document).pages().getFirst();
        LaidOutFormulaBlock formula = page.blocks().stream()
                .filter(LaidOutFormulaBlock.class::isInstance)
                .map(LaidOutFormulaBlock.class::cast)
                .findFirst()
                .orElseThrow();

        HitTestResult hit = new PageHitTester().hitTest(page, formula.x() + 4, formula.y() + 4);

        assertNotNull(hit.blockSelection());
        assertNull(hit.position());
        assertEquals(1, hit.blockSelection().blockIndex());
    }

    private static LaidOutDocument layout(Document document) {
        return new PaginatingDocumentLayoutEngine(new GreedyParagraphLayouter(new ApproximateTextMeasurer()))
                .layout(document, LayoutTheme.defaultTheme());
    }
}
