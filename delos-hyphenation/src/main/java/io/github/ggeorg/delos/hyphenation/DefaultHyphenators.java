package io.github.ggeorg.delos.hyphenation;

/** Factory methods for Delos' built-in hyphenation policies. */
public final class DefaultHyphenators {
    private static final Hyphenator ENGLISH = LiangHyphenator.english();

    private DefaultHyphenators() {
    }

    public static Hyphenator english() {
        return ENGLISH;
    }

    public static Hyphenator english(HyphenationPolicy policy) {
        return LiangHyphenator.english(policy);
    }

    public static Hyphenator forLanguageTag(String languageTag) {
        return forLanguageTag(languageTag, HyphenationPolicy.english());
    }

    public static Hyphenator forLanguageTag(String languageTag, HyphenationPolicy policy) {
        String normalized = languageTag == null || languageTag.isBlank()
                ? "en-us"
                : languageTag.trim().replace('_', '-').toLowerCase(java.util.Locale.ROOT);
        if (normalized.equals("en") || normalized.equals("en-us")) {
            return english(policy);
        }
        return LiangHyphenator.forLanguageTag(normalized, policy);
    }
}
