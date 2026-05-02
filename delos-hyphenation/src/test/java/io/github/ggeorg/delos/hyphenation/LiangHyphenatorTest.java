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
}
