package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.ListMarkerKind;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.editor.DocumentEdit;
import io.github.ggeorg.delos.writer.editor.DocumentFormatter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentFormatterListContractTest {
    private final DocumentFormatter formatter = new DocumentFormatter();

    @Test
    void togglesBulletListOnCurrentParagraphWithoutInjectingBulletText() {
        Document document = document("alpha");

        DocumentEdit enabled = formatter.toggleListKind(
                document,
                null,
                new TextPosition(0, 0),
                ListMarkerKind.BULLET,
                "Bulleted List"
        );

        Paragraph paragraph = enabled.document().paragraphs().getFirst();
        assertTrue(paragraph.style().isListItem());
        assertEquals(ListMarkerKind.BULLET, paragraph.style().listStyle().kind());
        assertEquals("alpha", paragraph.plainText());

        DocumentEdit disabled = formatter.toggleListKind(
                enabled.document(),
                null,
                new TextPosition(0, 0),
                ListMarkerKind.BULLET,
                "Bulleted List"
        );

        assertFalse(disabled.document().paragraphs().getFirst().style().isListItem());
        assertEquals("alpha", disabled.document().paragraphs().getFirst().plainText());
    }

    @Test
    void appliesNumberedListToSelectedParagraphs() {
        Document document = document("alpha", "beta", "gamma");
        SelectionRange selection = new SelectionRange(new TextPosition(0, 0), new TextPosition(1, 4));

        DocumentEdit edit = formatter.toggleListKind(
                document,
                selection,
                new TextPosition(1, 4),
                ListMarkerKind.NUMBERED,
                "Numbered List"
        );

        assertEquals(ListMarkerKind.NUMBERED, edit.document().paragraphs().get(0).style().listStyle().kind());
        assertEquals(ListMarkerKind.NUMBERED, edit.document().paragraphs().get(1).style().listStyle().kind());
        assertEquals(ListMarkerKind.NONE, edit.document().paragraphs().get(2).style().listStyle().kind());
    }

    @Test
    void changesListLevelAndOutdentsLevelZeroBackToBodyText() {
        Document document = document("alpha");
        Document numbered = formatter.toggleListKind(
                document,
                null,
                new TextPosition(0, 0),
                ListMarkerKind.NUMBERED,
                "Numbered List"
        ).document();

        Document indented = formatter.increaseListLevel(
                numbered,
                null,
                new TextPosition(0, 0),
                "Increase List Level"
        ).document();

        assertEquals(1, indented.paragraphs().getFirst().style().listStyle().level());

        Document outdented = formatter.decreaseListLevel(
                indented,
                null,
                new TextPosition(0, 0),
                "Decrease List Level"
        ).document();

        assertEquals(0, outdented.paragraphs().getFirst().style().listStyle().level());

        Document body = formatter.decreaseListLevel(
                outdented,
                null,
                new TextPosition(0, 0),
                "Decrease List Level"
        ).document();

        assertFalse(body.paragraphs().getFirst().style().isListItem());
    }

    private static Document document(String... paragraphs) {
        return new Document(
                "Test",
                PageStyle.a4Default(),
                List.of(paragraphs).stream().map(Paragraph::of).toList()
        );
    }
}
