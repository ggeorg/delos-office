package io.github.ggeorg.delos.writer.ui;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.contract.JavaFxTestSupport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class StatusBarTest extends JavaFxTestSupport {

    @Test
    void updatesDisplayedStatusText() {
        StatusBar bar = onFxThread(StatusBar::new);

        onFxThread(() -> {
            bar.setPageInfo(2, 7);
            bar.setWordCount(1247);
            bar.setLanguage("English");
            bar.setZoomText("100%");
        });

        assertEquals("Page 2 of 7", onFxThread(() -> bar.pageIndicatorLabel().getText()));
        assertEquals("1247 words", onFxThread(() -> bar.wordCountLabel().getText()));
        assertEquals("English", onFxThread(() -> bar.languageLabel().getText()));
        assertEquals("Zoom: 100%", onFxThread(() -> bar.zoomLabel().getText()));
    }

    @Test
    void countsWordsFromDocumentParagraphs() {
        Document document = new Document(
                "Status Test",
                PageStyle.a4Default(),
                List.of(
                        Paragraph.of("One two three"),
                        Paragraph.of("   "),
                        Paragraph.of("Four\nFive"),
                        Paragraph.of("six")
                )
        );

        assertEquals(6, StatusBar.countWords(document));
    }
}
