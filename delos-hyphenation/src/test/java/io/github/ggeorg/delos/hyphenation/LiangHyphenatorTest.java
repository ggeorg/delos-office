package io.github.ggeorg.delos.hyphenation;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LiangHyphenatorTest {
    @Test
    void appliesLiangPatternWeightsAndPreservesSoftHyphenHints() {
        LiangHyphenator hyphenator = new LiangHyphenator(List.of("de1mon1stra1tion"), 2, 3);

        assertEquals(List.of(2, 5, 9), hyphenator.hyphenationPoints("demonstration"));
        assertEquals(List.of(2, 5, 9), hyphenator.hyphenationPoints("de\u00ADmonstration"));
    }

    @Test
    void readsTexPatternBlocksAndIgnoresTexCommandsAndComments() throws IOException {
        String tex = """
                % metadata and comments are not patterns
                \\patterns{ % text after percent is ignored
                  de1mon1stra1tion
                  pag1i1na1tion
                }
                \\endinput
                """;
        LiangHyphenator hyphenator = LiangHyphenator.fromPatternStream(
                new ByteArrayInputStream(tex.getBytes(StandardCharsets.UTF_8)),
                new HyphenationPolicy(2, 3, 6, true, 25, 600, 1200)
        );

        assertEquals(List.of(2, 5, 9), hyphenator.hyphenationPoints("demonstration"));
        assertEquals(List.of(3, 4, 6), hyphenator.hyphenationPoints("pagination"));
    }

    @Test
    void defaultEnglishHyphenatorLoadsRealTexClasspathPatterns() {
        List<Integer> points = LiangHyphenator.english().hyphenationPoints("hyphenation");

        assertFalse(points.isEmpty(), "real en-US TeX patterns should hyphenate ordinary English words");
        assertTrue(points.stream().allMatch(point -> point >= 2 && point <= "hyphenation".length() - 3));
    }

    @Test
    void missingPatternResourcesFailFast() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> LiangHyphenator.fromClasspathResource("/io/github/ggeorg/delos/hyphenation/patterns/missing.tex", HyphenationPolicy.english())
        );

        assertTrue(exception.getMessage().contains("Hyphenation pattern resource not found"));
    }

    @Test
    void languageTagFactoryUsesEnglishPatternsForCommonEnglishTags() {
        assertFalse(DefaultHyphenators.forLanguageTag("en-US").hyphenationPoints("hyphenation").isEmpty());
        assertFalse(DefaultHyphenators.forLanguageTag("en_US").hyphenationPoints("pagination").isEmpty());
    }

    @Test
    void skipsUnsafeAutomaticTokensButKeepsManualSoftHyphen() {
        LiangHyphenator hyphenator = LiangHyphenator.english();

        assertEquals(List.of(), hyphenator.hyphenationPoints("NASA"));
        assertEquals(List.of(), hyphenator.hyphenationPoints("A320neo"));
        assertEquals(List.of(), hyphenator.hyphenationPoints("https://example.com"));
        assertEquals(List.of(), hyphenator.hyphenationPoints("already-hyphenated"));
        assertEquals(List.of(2), hyphenator.hyphenationPoints("de\u00ADmo"));
    }

    @Test
    void defaultPolicyDoesNotAutoHyphenateCapitalizedWords() {
        LiangHyphenator hyphenator = LiangHyphenator.english();

        assertTrue(hyphenator.hyphenationPoints("Paragraph").isEmpty());
        assertFalse(hyphenator.hyphenationPoints("paragraph").isEmpty());
    }

    @Test
    void explicitPolicyCanAllowCapitalizedWords() {
        HyphenationPolicy policy = new HyphenationPolicy(2, 3, 7, true, 25, 600, 1200);
        LiangHyphenator hyphenator = LiangHyphenator.english(policy);

        assertFalse(hyphenator.hyphenationPoints("Paragraph").isEmpty());
    }

    @Test
    void cacheIsBoundedAndEvictsLeastRecentlyUsedWords() {
        LiangHyphenator hyphenator = new LiangHyphenator(
                List.of("a1bcdefgh", "b1bcdefgh", "c1bcdefgh", "d1bcdefgh"),
                new HyphenationPolicy(1, 1, 3, true, 25, 600, 1200),
                3
        );

        hyphenator.hyphenationPoints("abcdefgh");
        hyphenator.hyphenationPoints("bbcdefgh");
        hyphenator.hyphenationPoints("cbcdefgh");
        assertEquals(3, hyphenator.cachedWordCount());

        // Touch the first word so the second word becomes the eldest entry.
        hyphenator.hyphenationPoints("abcdefgh");
        hyphenator.hyphenationPoints("dbcdefgh");

        assertEquals(3, hyphenator.cachedWordCount());
        assertTrue(hyphenator.hasCachedWord("abcdefgh"));
        assertFalse(hyphenator.hasCachedWord("bbcdefgh"));
        assertTrue(hyphenator.hasCachedWord("cbcdefgh"));
        assertTrue(hyphenator.hasCachedWord("dbcdefgh"));
    }

    @Test
    void cacheCanBeDisabledForMemorySensitiveCallers() {
        LiangHyphenator hyphenator = new LiangHyphenator(
                List.of("a1bcdefgh"),
                new HyphenationPolicy(1, 1, 3, true, 25, 600, 1200),
                0
        );

        assertEquals(List.of(1), hyphenator.hyphenationPoints("abcdefgh"));
        assertEquals(0, hyphenator.cachedWordCount());
    }

    @Test
    void unsafeAndOversizedTokensAreNotCached() {
        LiangHyphenator hyphenator = new LiangHyphenator(
                List.of("de1mon1stra1tion"),
                HyphenationPolicy.english(),
                10
        );

        hyphenator.hyphenationPoints("NASA");
        hyphenator.hyphenationPoints("A320neo");
        hyphenator.hyphenationPoints("https://example.com");
        hyphenator.hyphenationPoints("already-hyphenated");
        hyphenator.hyphenationPoints("x".repeat(81));

        assertEquals(0, hyphenator.cachedWordCount());
    }

    @Test
    void safeOrdinaryWordsAreCachedWithoutChangingHyphenationResult() {
        LiangHyphenator hyphenator = new LiangHyphenator(
                List.of("de1mon1stra1tion"),
                HyphenationPolicy.english(),
                10
        );

        assertEquals(List.of(2, 5, 9), hyphenator.hyphenationPoints("demonstration"));
        assertTrue(hyphenator.hasCachedWord("demonstration"));
        assertEquals(List.of(2, 5, 9), hyphenator.hyphenationPoints("demonstration"));
        assertEquals(1, hyphenator.cachedWordCount());
    }

}
