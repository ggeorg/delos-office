package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.FormulaBlock;
import io.github.ggeorg.delos.writer.document.HorizontalRuleBlock;
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

class MixedBlockSelectionDeletionContractTest {
    @Test
    void explicitMixedSelectionDeletesInterleavedImageAndMergesParagraphFragments() {
        Document document = Document.fromBlocks("Selection", PageStyle.a4Default(), List.of(
                ParagraphBlock.of(Paragraph.of("Before")),
                new ImageBlock("media/photo.png", 120.0, 80.0, "photo"),
                ParagraphBlock.of(Paragraph.of("After"))
        ));

        SelectionRange selection = new SelectionRange(
                new TextPosition(0, 3),
                new TextPosition(1, 2)
        );

        DocumentEdit edit = new DocumentEditor().replaceIncludingInterleavedBlocks(
                document,
                selection,
                new TextPosition(1, 2),
                "",
                "Delete Mixed Selection"
        );

        assertEquals(1, edit.document().blocks().size());
        ParagraphBlock paragraphBlock = assertInstanceOf(ParagraphBlock.class, edit.document().blocks().get(0));
        assertEquals("Befter", paragraphBlock.paragraph().plainText());
        assertEquals(new TextPosition(0, 3), edit.caretPosition());
    }

    @Test
    void explicitMixedSelectionCanInsertReplacementWhileDeletingRichBlocks() {
        Document document = Document.fromBlocks("Selection", PageStyle.a4Default(), List.of(
                ParagraphBlock.of(Paragraph.of("Alpha")),
                new FormulaBlock("x^2", "latex"),
                new HorizontalRuleBlock(),
                ParagraphBlock.of(Paragraph.of("Omega"))
        ));

        SelectionRange selection = new SelectionRange(
                new TextPosition(0, 2),
                new TextPosition(1, 2)
        );

        DocumentEdit edit = new DocumentEditor().replaceIncludingInterleavedBlocks(
                document,
                selection,
                new TextPosition(1, 2),
                "X",
                "Replace Mixed Selection"
        );

        assertEquals(1, edit.document().blocks().size());
        ParagraphBlock paragraphBlock = assertInstanceOf(ParagraphBlock.class, edit.document().blocks().get(0));
        assertEquals("AlXega", paragraphBlock.paragraph().plainText());
        assertEquals(new TextPosition(0, 3), edit.caretPosition());
    }

    @Test
    void explicitMixedSelectionDeletesTablesBetweenParagraphBoundaries() {
        Document document = Document.fromBlocks("Selection", PageStyle.a4Default(), List.of(
                ParagraphBlock.of(Paragraph.of("Left")),
                TableBlock.blank(2, 2),
                ParagraphBlock.of(Paragraph.of("Right"))
        ));

        SelectionRange selection = new SelectionRange(
                new TextPosition(0, 4),
                new TextPosition(1, 0)
        );

        DocumentEdit edit = new DocumentEditor().replaceIncludingInterleavedBlocks(
                document,
                selection,
                new TextPosition(1, 0),
                "",
                "Delete Mixed Selection"
        );

        assertEquals(1, edit.document().blocks().size());
        ParagraphBlock paragraphBlock = assertInstanceOf(ParagraphBlock.class, edit.document().blocks().get(0));
        assertEquals("LeftRight", paragraphBlock.paragraph().plainText());
        assertEquals(new TextPosition(0, 4), edit.caretPosition());
    }

    @Test
    void paragraphOnlyReplaceApiStillPreservesInterleavedRichBlocks() {
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
                "Delete Paragraph Selection"
        );

        assertEquals(2, edit.document().blocks().size());
        assertInstanceOf(ImageBlock.class, edit.document().blocks().get(1));
        assertEquals(List.of("Befter"), edit.document().paragraphs().stream().map(Paragraph::plainText).toList());
    }
}
