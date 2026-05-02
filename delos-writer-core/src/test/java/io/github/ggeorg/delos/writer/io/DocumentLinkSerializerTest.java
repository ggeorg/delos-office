package io.github.ggeorg.delos.writer.io;

import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.TextRun;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentLinkSerializerTest {
    @Test
    void roundTripsLinkedRunsThroughContentXml() {
        Document original = new Document(
                "Links",
                PageStyle.a4Default(),
                List.of(new Paragraph(List.of(
                        new TextRun("Delos", CharacterStyle.PLAIN.withLinkHref("https://example.com/?q=delos&x=1"))
                )))
        );

        String xml = new DocumentSerializer().toXml(original);
        Document restored = new DocumentSerializer().fromXml(xml);

        assertTrue(xml.contains("link-href=\"https://example.com/?q=delos&amp;x=1\""));
        assertEquals(original, restored);
        assertEquals(
                "https://example.com/?q=delos&x=1",
                restored.paragraphs().getFirst().runs().getFirst().style().linkHref()
        );
    }
}
