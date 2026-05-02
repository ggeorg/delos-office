package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.document.TextRun;
import io.github.ggeorg.delos.writer.editor.DocumentEdit;
import io.github.ggeorg.delos.writer.editor.DocumentFormatter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormatterLinkContractTest {
    private final DocumentFormatter formatter = new DocumentFormatter();

    @Test
    void appliesLinkToSelectedTextRange() {
        Document document = document("alpha beta");
        SelectionRange selection = new SelectionRange(new TextPosition(0, 6), new TextPosition(0, 10));
        TextPosition caret = new TextPosition(0, 10);

        DocumentEdit edit = formatter.applyLink(document, selection, caret, " https://example.com/beta ", "Apply link");

        List<TextRun> runs = edit.document().paragraphs().getFirst().runs();
        assertEquals("alpha ", runs.get(0).text());
        assertNull(runs.get(0).linkHref());
        assertEquals("beta", runs.get(1).text());
        assertEquals("https://example.com/beta", runs.get(1).linkHref());
        assertEquals(caret, edit.caretPosition());
        assertEquals(selection, edit.selectionRange());
    }

    @Test
    void removesLinkFromSelectedTextRange() {
        Document document = document("alpha beta");
        SelectionRange selection = new SelectionRange(new TextPosition(0, 6), new TextPosition(0, 10));
        TextPosition caret = new TextPosition(0, 10);

        Document linked = formatter.applyLink(document, selection, caret, "https://example.com/beta", "Apply link").document();
        DocumentEdit unlinked = formatter.removeLink(linked, selection, caret, "Remove link");

        List<TextRun> runs = unlinked.document().paragraphs().getFirst().runs();
        assertEquals("alpha beta", runs.stream().map(TextRun::text).reduce("", String::concat));
        assertTrue(runs.stream().allMatch(run -> run.linkHref() == null));
    }

    @Test
    void emptyLinkHrefIsRejected() {
        Document document = document("alpha beta");
        SelectionRange selection = new SelectionRange(new TextPosition(0, 0), new TextPosition(0, 5));

        assertThrows(IllegalArgumentException.class,
                () -> formatter.applyLink(document, selection, new TextPosition(0, 5), "  ", "Apply link"));
    }

    private static Document document(String... paragraphs) {
        return new Document(
                "Test",
                PageStyle.a4Default(),
                List.of(paragraphs).stream().map(Paragraph::of).toList()
        );
    }
}
