package io.github.ggeorg.delos.writer.ui;

import io.github.ggeorg.delos.writer.document.CaretPosition;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.Story;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TableCell;
import io.github.ggeorg.delos.writer.document.TableCellSelection;
import io.github.ggeorg.delos.writer.document.TableRow;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.editor.DocumentEditor;
import io.github.ggeorg.delos.writer.editor.DocumentFormatter;
import io.github.ggeorg.delos.writer.editor.EditorInteractionModel;
import io.github.ggeorg.delos.writer.layout.DocumentPositionNavigator;
import io.github.ggeorg.delos.writer.session.EditorSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DocumentViewportUnicodeDeletionContractTest {
    @Test
    void bodyBackspaceDeletesWholeEmojiCodePoint() {
        EditorSession session = new EditorSession(documentWithBodyText("A😀B"));
        EditorInteractionModel interaction = new EditorInteractionModel();
        interaction.setCaret(new TextPosition(0, "A😀".length()));

        controller(session, interaction).deleteBackward();

        assertEquals("AB", session.document().paragraphs().getFirst().plainText());
    }

    @Test
    void bodyDeleteForwardDeletesWholeEmojiCodePoint() {
        EditorSession session = new EditorSession(documentWithBodyText("A😀B"));
        EditorInteractionModel interaction = new EditorInteractionModel();
        interaction.setCaret(new TextPosition(0, "A".length()));

        controller(session, interaction).deleteForward();

        assertEquals("AB", session.document().paragraphs().getFirst().plainText());
    }

    @Test
    void tableCellStoryBackspaceDeletesWholeEmojiCodePoint() {
        Document document = documentWithTableCellText("A😀B");
        EditorSession session = new EditorSession(document);
        EditorInteractionModel interaction = new EditorInteractionModel();
        interaction.setStoryCaret(CaretPosition.tableCell(1, 0, 0, 0, "A😀".length()), new TextPosition(1, 0));

        controller(session, interaction).deleteBackwardAtStoryCaret();

        assertEquals("AB", tableCellText(session.document()));
    }

    @Test
    void tableCellStoryDeleteForwardDeletesWholeEmojiCodePoint() {
        Document document = documentWithTableCellText("A😀B");
        EditorSession session = new EditorSession(document);
        EditorInteractionModel interaction = new EditorInteractionModel();
        interaction.setStoryCaret(CaretPosition.tableCell(1, 0, 0, 0, "A".length()), new TextPosition(1, 0));

        controller(session, interaction).deleteForwardAtStoryCaret();

        assertEquals("AB", tableCellText(session.document()));
    }

    private static DocumentViewportEditController controller(EditorSession session, EditorInteractionModel interaction) {
        return new DocumentViewportEditController(
                session,
                interaction,
                new DocumentPositionNavigator(),
                new DocumentEditor(),
                new DocumentFormatter(),
                () -> null,
                (caret, selection) -> { },
                () -> { },
                () -> { }
        );
    }

    private static Document documentWithBodyText(String text) {
        return new Document("Unicode", PageStyle.a4Default(), List.of(Paragraph.of(text)));
    }

    private static Document documentWithTableCellText(String text) {
        TableBlock table = new TableBlock(List.of(new TableRow(List.of(new TableCell(Story.ofParagraphs(List.of(Paragraph.of(text))))))));
        return Document.fromBlocks(
                "Unicode Table",
                PageStyle.a4Default(),
                List.of(new ParagraphBlock(Paragraph.of("before")), table, new ParagraphBlock(Paragraph.of("after")))
        );
    }

    private static String tableCellText(Document document) {
        return new DocumentEditor().tableCellPlainText(document, new TableCellSelection(1, 0, 0));
    }
}
