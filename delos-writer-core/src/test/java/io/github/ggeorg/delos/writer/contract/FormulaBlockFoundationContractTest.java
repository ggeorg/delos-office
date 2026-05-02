package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.BlockKind;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.FormulaBlock;
import io.github.ggeorg.delos.writer.document.FormulaSourceFormat;
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

class FormulaBlockFoundationContractTest {
    @Test
    void formulaBlockIsSourceBasedAndDefaultsToLatex() {
        FormulaBlock formula = new FormulaBlock("E = mc^2", "Einstein mass energy equation");

        assertEquals(BlockKind.FORMULA, formula.kind());
        assertEquals(FormulaSourceFormat.LATEX, formula.sourceFormat());
        assertEquals("E = mc^2", formula.source());
        assertEquals("Einstein mass energy equation", formula.altText());
    }

    @Test
    void formulaBlocksAreSkippedByParagraphProjectionButPreservedInBlockOrder() {
        Document document = Document.fromBlocks("Formula", PageStyle.a4Default(), List.of(
                ParagraphBlock.of(Paragraph.of("before")),
                new FormulaBlock("E = mc^2"),
                ParagraphBlock.of(Paragraph.of("after"))
        ));

        assertEquals(3, document.blocks().size());
        assertInstanceOf(FormulaBlock.class, document.blocks().get(1));
        assertEquals(List.of("before", "after"), document.paragraphs().stream().map(Paragraph::plainText).toList());
    }

    @Test
    void insertingFormulaUsesExistingAtomicBlockInsertionPath() {
        Document document = new Document("Formula", PageStyle.a4Default(), List.of(Paragraph.of("alpha beta")));

        DocumentEdit edit = new DocumentEditor().insertBlock(
                document,
                new TextPosition(0, 5),
                new FormulaBlock("E = mc^2"),
                List.of(),
                "Insert Formula"
        );

        assertEquals(3, edit.document().blocks().size());
        assertInstanceOf(ParagraphBlock.class, edit.document().blocks().get(0));
        assertInstanceOf(FormulaBlock.class, edit.document().blocks().get(1));
        assertInstanceOf(ParagraphBlock.class, edit.document().blocks().get(2));
        assertEquals(List.of("alpha", " beta"), edit.document().paragraphs().stream().map(Paragraph::plainText).toList());
    }
}
