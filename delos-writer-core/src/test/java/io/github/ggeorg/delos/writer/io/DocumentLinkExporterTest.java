package io.github.ggeorg.delos.writer.io;

import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.TextRun;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentLinkExporterTest {
    @Test
    void htmlExporterEmitsAnchorForLinkedRuns() {
        Document document = documentWithLinkedRun();

        String html = new DocumentHtmlExporter().toHtml(document);

        assertTrue(html.contains("<a href=\"https://example.com/?q=delos&amp;x=1\""));
        assertTrue(html.contains("text-decoration: underline"));
        assertTrue(html.contains(">Delos</a>"));
    }

    @Test
    void markdownExporterEmitsMarkdownLinkForLinkedRuns() {
        Document document = documentWithLinkedRun();

        String markdown = new DocumentMarkdownExporter().toMarkdown(document);

        assertTrue(markdown.contains("[Delos](https://example.com/?q=delos&x=1)"));
    }

    private static Document documentWithLinkedRun() {
        return new Document(
                "Links",
                PageStyle.a4Default(),
                List.of(new Paragraph(List.of(
                        new TextRun("Delos", CharacterStyle.PLAIN.withLinkHref("https://example.com/?q=delos&x=1"))
                )))
        );
    }
}
