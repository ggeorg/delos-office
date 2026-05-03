package io.github.ggeorg.delos.hyphenation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HyphenatorProviderTest {
    @Test
    void defaultProviderLoadsKnownEnglishPatterns() {
        HyphenatorProvider provider = HyphenatorProvider.defaults();

        assertFalse(provider.forLanguageTag("en-US").hyphenationPoints("hyphenation").isEmpty());
        assertFalse(provider.forLanguageTag("en_US").hyphenationPoints("pagination").isEmpty());
    }

    @Test
    void defaultProviderFallsBackToNoneForMissingOrBlankLanguages() {
        HyphenatorProvider provider = HyphenatorProvider.defaults();

        assertTrue(provider.forLanguageTag("el-GR").hyphenationPoints("hyphenation").isEmpty(),
                "missing bundled language resources must not crash layout");
        assertTrue(provider.forLanguageTag(null).hyphenationPoints("hyphenation").isEmpty());
        assertTrue(provider.forLanguageTag("   ").hyphenationPoints("hyphenation").isEmpty());
    }

    @Test
    void fixedProviderIgnoresLanguageTag() {
        Hyphenator fixed = word -> java.util.List.of(2);
        HyphenatorProvider provider = HyphenatorProvider.fixed(fixed);

        assertFalse(provider.forLanguageTag("zz-ZZ").hyphenationPoints("abcdef").isEmpty());
    }
}
