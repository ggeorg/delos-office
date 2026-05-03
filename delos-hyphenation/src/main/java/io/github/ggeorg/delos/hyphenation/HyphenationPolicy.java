package io.github.ggeorg.delos.hyphenation;

import java.util.Locale;
import java.util.Objects;

/**
 * Quality policy for automatic hyphenation.
 *
 * <p>The Liang algorithm answers "where could this word legally break?" This
 * policy answers "should Delos offer automatic break points for this kind of
 * token, and how expensive should an automatic break be for the line breaker?"
 * Keeping that distinction explicit prevents layout code from scattering magic
 * numbers and ad-hoc token filters.</p>
 */
public record HyphenationPolicy(
        int leftMin,
        int rightMin,
        int minWordLength,
        boolean autoHyphenateCapitalizedWords,
        int explicitSoftHyphenPenalty,
        int automaticHyphenPenalty,
        int capitalizedWordPenalty
) {
    public static final HyphenationPolicy ENGLISH = new HyphenationPolicy(
            2,
            3,
            7,
            false,
            25,
            600,
            1_200
    );

    public HyphenationPolicy {
        leftMin = Math.max(1, leftMin);
        rightMin = Math.max(1, rightMin);
        minWordLength = Math.max(leftMin + rightMin + 1, minWordLength);
        explicitSoftHyphenPenalty = Math.max(0, explicitSoftHyphenPenalty);
        automaticHyphenPenalty = Math.max(0, automaticHyphenPenalty);
        capitalizedWordPenalty = Math.max(automaticHyphenPenalty, capitalizedWordPenalty);
    }

    public static HyphenationPolicy english() {
        return ENGLISH;
    }

    public boolean allowsAutomaticHyphenation(String visibleWord) {
        String word = Objects.requireNonNullElse(visibleWord, "");
        if (word.length() < minWordLength) {
            return false;
        }
        if (word.indexOf('-') >= 0 || word.indexOf('_') >= 0 || word.indexOf('/') >= 0) {
            return false;
        }
        if (looksLikeUrlishToken(word) || containsDigit(word) || !isAlphabetic(word)) {
            return false;
        }
        if (isAllUpperCase(word)) {
            return false;
        }
        return autoHyphenateCapitalizedWords || !isCapitalized(word);
    }

    public int penaltyFor(String visibleWord, boolean explicitSoftHyphen) {
        if (explicitSoftHyphen) {
            return explicitSoftHyphenPenalty;
        }
        return isCapitalized(Objects.requireNonNullElse(visibleWord, ""))
                ? capitalizedWordPenalty
                : automaticHyphenPenalty;
    }

    private static boolean looksLikeUrlishToken(String word) {
        String lower = word.toLowerCase(Locale.ROOT);
        return lower.startsWith("http")
                || lower.startsWith("www")
                || lower.contains("://")
                || lower.contains(".");
    }

    private static boolean containsDigit(String word) {
        for (int i = 0; i < word.length(); i++) {
            if (Character.isDigit(word.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAlphabetic(String word) {
        for (int i = 0; i < word.length(); i++) {
            if (!Character.isLetter(word.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAllUpperCase(String word) {
        boolean sawLetter = false;
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            if (Character.isLetter(ch)) {
                sawLetter = true;
                if (!Character.isUpperCase(ch)) {
                    return false;
                }
            }
        }
        return sawLetter;
    }

    private static boolean isCapitalized(String word) {
        if (word.isEmpty()) {
            return false;
        }
        int first = word.codePointAt(0);
        return Character.isUpperCase(first);
    }
}
