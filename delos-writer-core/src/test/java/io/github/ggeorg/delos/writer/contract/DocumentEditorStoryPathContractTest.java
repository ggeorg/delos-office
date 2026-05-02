package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.CaretPosition;
import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.Story;
import io.github.ggeorg.delos.writer.document.StoryPath;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TableCellSelection;
import io.github.ggeorg.delos.writer.document.TableCellStoryPath;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.document.TextRun;
import io.github.ggeorg.delos.writer.editor.DocumentEdit;
import io.github.ggeorg.delos.writer.editor.DocumentEditor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class DocumentEditorStoryPathContractTest {
    @Test
    void resolvesAndReplacesBodyStory() {
        Document document = Document.fromBlocks("Story", PageStyle.a4Default(), List.of(
                ParagraphBlock.of(Paragraph.of("alpha"))
        ));
        DocumentEditor editor = new DocumentEditor();

        assertEquals(document.body(), editor.resolveStory(document, StoryPath.body()));

        Document updated = editor.replaceStory(document, StoryPath.body(), Story.ofParagraphs(List.of(Paragraph.of("beta"))));

        assertEquals("beta", updated.paragraphs().getFirst().plainText());
    }

    @Test
    void resolvesAndReplacesTableCellStory() {
        Document document = Document.fromBlocks("Table", PageStyle.a4Default(), List.of(
                ParagraphBlock.of(Paragraph.of("Before")),
                TableBlock.blank(1, 1),
                ParagraphBlock.of(Paragraph.of("After"))
        ));
        DocumentEditor editor = new DocumentEditor();
        TableCellStoryPath path = StoryPath.tableCell(1, 0, 0);

        assertEquals("", editor.resolveStory(document, path).paragraphs().getFirst().plainText());

        Document updated = editor.replaceStory(document, path, Story.ofParagraphs(List.of(Paragraph.of("inside"))));
        TableBlock table = assertInstanceOf(TableBlock.class, updated.blocks().get(1));

        assertEquals("inside", table.rows().getFirst().cells().getFirst().paragraphs().getFirst().plainText());
    }

    @Test
    void bodyTextReplacementRoutesThroughStoryEditor() {
        Document document = Document.fromBlocks("Body", PageStyle.a4Default(), List.of(
                ParagraphBlock.of(Paragraph.of("hello world"))
        ));

        DocumentEdit edit = new DocumentEditor().replace(
                document,
                new TextPosition(0, 5),
                new TextPosition(0, 5),
                " brave",
                "Insert Text"
        );

        assertEquals("hello brave world", edit.document().paragraphs().getFirst().plainText());
        assertEquals(new TextPosition(0, 11), edit.caretPosition());
    }

    @Test
    void coarseTableCellEditingRoutesThroughStoryEditor() {
        Document document = Document.fromBlocks("Table", PageStyle.a4Default(), List.of(
                ParagraphBlock.of(Paragraph.of("Before")),
                TableBlock.blank(1, 1),
                ParagraphBlock.of(Paragraph.of("After"))
        ));

        DocumentEdit edit = new DocumentEditor().replaceTableCellText(
                document,
                new TableCellSelection(1, 0, 0),
                "line one\nline two",
                "Edit Table Cell"
        );

        TableBlock table = assertInstanceOf(TableBlock.class, edit.document().blocks().get(1));
        assertEquals(2, table.rows().getFirst().cells().getFirst().content().blocks().size());
        assertEquals("line one\nline two", new DocumentEditor().tableCellPlainText(edit.document(), new TableCellSelection(1, 0, 0)));
    }

    @Test
    void tableCellCaretReplacementPreservesExistingParagraphRuns() {
        CharacterStyle bold = CharacterStyle.PLAIN.withBold(true);
        CharacterStyle italic = CharacterStyle.PLAIN.withItalic(true);
        Document document = Document.fromBlocks("Table", PageStyle.a4Default(), List.of(
                ParagraphBlock.of(Paragraph.of("Before")),
                new TableBlock(List.of(new io.github.ggeorg.delos.writer.document.TableRow(List.of(
                        new io.github.ggeorg.delos.writer.document.TableCell(Story.ofParagraphs(List.of(
                                new Paragraph(List.of(new TextRun("ab", bold), new TextRun("cd", italic)))
                        )))
                )))),
                ParagraphBlock.of(Paragraph.of("After"))
        ));
        StoryPath cellPath = StoryPath.tableCell(1, 0, 0);

        DocumentEdit edit = new DocumentEditor().replace(
                document,
                cellPath,
                new TextPosition(0, 2),
                new TextPosition(0, 2),
                "X",
                "Insert Cell Text"
        );

        TableBlock table = assertInstanceOf(TableBlock.class, edit.document().blocks().get(1));
        Paragraph paragraph = table.rows().getFirst().cells().getFirst().paragraphs().getFirst();

        assertEquals("abXcd", paragraph.plainText());
        assertEquals(new TextRun("ab", bold), paragraph.runs().get(0));
        assertEquals(TextRun.plain("X"), paragraph.runs().get(1));
        assertEquals(new TextRun("cd", italic), paragraph.runs().get(2));
        assertEquals(new CaretPosition(cellPath, 0, 3), edit.storyCaretPosition());
        assertEquals(new TextPosition(1, 0), edit.caretPosition());
    }

    @Test
    void nestedTableInTableCellStoryIsPolicyDisabledForV1() {
        Document document = Document.fromBlocks("Table", PageStyle.a4Default(), List.of(
                ParagraphBlock.of(Paragraph.of("Before")),
                TableBlock.blank(1, 1),
                ParagraphBlock.of(Paragraph.of("After"))
        ));

        assertThrows(IllegalArgumentException.class, () -> new DocumentEditor().replaceStory(
                document,
                StoryPath.tableCell(1, 0, 0),
                Story.ofBlocks(List.of(TableBlock.blank(1, 1)))
        ));
    }
}
