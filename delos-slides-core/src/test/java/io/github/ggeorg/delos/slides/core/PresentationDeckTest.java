package io.github.ggeorg.delos.slides.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class PresentationDeckTest {
    @Test
    void blankDeckContainsOneTitleSlide() {
        PresentationDeck deck = PresentationDeck.blank();

        assertEquals("Untitled", deck.title());
        assertEquals(1, deck.slides().size());
        assertEquals("Title Slide", deck.firstSlide().title());
    }

    @Test
    void deckCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new PresentationDeck("Deck", List.of()));
    }

    @Test
    void addSlideReturnsNewDeck() {
        PresentationDeck deck = PresentationDeck.blank();
        PresentationDeck updated = deck.addSlide(Slide.blank("Agenda"));

        assertEquals(1, deck.slides().size());
        assertEquals(2, updated.slides().size());
        assertEquals("Agenda", updated.slideAt(1).title());
    }
}
