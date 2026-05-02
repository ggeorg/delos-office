package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.ListMarkerKind;
import io.github.ggeorg.delos.writer.document.ParagraphListStyle;
import io.github.ggeorg.delos.writer.document.ParagraphStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParagraphListStyleContractTest {
    @Test
    void defaultParagraphStyleIsNotAListItem() {
        ParagraphStyle style = ParagraphStyle.defaultBody();

        assertFalse(style.isListItem());
        assertEquals(ListMarkerKind.NONE, style.listStyle().kind());
    }

    @Test
    void supportsBulletAndNumberedListMetadata() {
        ParagraphStyle bullet = ParagraphStyle.defaultBody().asBulletListItem(1);
        ParagraphStyle numbered = ParagraphStyle.defaultBody().asNumberedListItem(2, 4);

        assertTrue(bullet.isListItem());
        assertEquals(ListMarkerKind.BULLET, bullet.listStyle().kind());
        assertEquals(1, bullet.listStyle().level());
        assertEquals(ListMarkerKind.NUMBERED, numbered.listStyle().kind());
        assertEquals(2, numbered.listStyle().level());
        assertEquals(4, numbered.listStyle().start());
    }

    @Test
    void rejectsInvalidListMetadata() {
        assertThrows(IllegalArgumentException.class, () -> ParagraphListStyle.bullet(-1));
        assertThrows(IllegalArgumentException.class, () -> ParagraphListStyle.numbered(0, 0));
    }
}
