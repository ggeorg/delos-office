package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.layout.SimpleEnglishHyphenator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleEnglishHyphenatorTest {
    @Test
    void detectsExplicitSoftHyphenHintsAsDiscretionaryBreaks() {
        List<Integer> points = new SimpleEnglishHyphenator().hyphenationPoints("de\u00ADmonstration");

        assertTrue(points.contains(2), "expected explicit soft hyphen to become a break opportunity");
    }

    @Test
    void returnsCuratedBreaksForCommonEnglishWords() {
        SimpleEnglishHyphenator hyphenator = new SimpleEnglishHyphenator();

        assertEquals(List.of(2, 5, 9), hyphenator.hyphenationPoints("demonstration"));
        assertEquals(List.of(4), hyphenator.hyphenationPoints("paragraph"));
        assertEquals(List.of(2, 6), hyphenator.hyphenationPoints("hyphenation"));
    }


    @Test
    void mergesExplicitSoftHyphensWithCuratedBreaksUniquelyAndInOrder() {
        List<Integer> points = new SimpleEnglishHyphenator().hyphenationPoints("de\u00ADmonstration");

        assertEquals(List.of(2, 5, 9), points);
    }

    @Test
    void skipsAutomaticHyphenationForUnsafeTokens() {
        SimpleEnglishHyphenator hyphenator = new SimpleEnglishHyphenator();

        assertEquals(List.of(), hyphenator.hyphenationPoints("NASA"));
        assertEquals(List.of(), hyphenator.hyphenationPoints("A320neo"));
        assertEquals(List.of(), hyphenator.hyphenationPoints("https://example.com"));
        assertEquals(List.of(), hyphenator.hyphenationPoints("already-hyphenated"));
    }

    @Test
    void keepsResultsStableAcrossRepeatedCalls() {
        SimpleEnglishHyphenator hyphenator = new SimpleEnglishHyphenator();

        List<Integer> first = hyphenator.hyphenationPoints("demonstration");
        List<Integer> second = hyphenator.hyphenationPoints("demonstration");

        assertFalse(first.isEmpty(), "expected a cached word to have non-empty points");
        assertEquals(first, second, "cached and uncached calls should agree");
    }
}
