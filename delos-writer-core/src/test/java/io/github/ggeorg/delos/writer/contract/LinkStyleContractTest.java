package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.TextRun;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkStyleContractTest {
    @Test
    void characterStyleStoresNormalizedLinkHref() {
        CharacterStyle style = CharacterStyle.PLAIN.withLinkHref("  https://example.com/path  ");

        assertEquals("https://example.com/path", style.linkHref());
        assertTrue(style.linked());
    }

    @Test
    void blankLinkHrefClearsLink() {
        CharacterStyle style = CharacterStyle.PLAIN.withLinkHref("https://example.com").withLinkHref("  ");

        assertNull(style.linkHref());
        assertFalse(style.linked());
    }

    @Test
    void textRunExposesLinkHrefThroughStyle() {
        TextRun run = TextRun.plain("Delos").withLinkHref("https://delos.example");

        assertEquals("https://delos.example", run.linkHref());
        assertTrue(run.linked());
    }
}
