package io.github.ggeorg.delos.writer.layout;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small pure-Java fallback hyphenator for Delos v14.
 *
 * <p>This remains intentionally conservative. It supports:
 * <ul>
 *   <li>manual soft hyphen hints ({@code \u00AD})</li>
 *   <li>a curated exception list for common English words</li>
 *   <li>a lightweight heuristic only for long plain alphabetic words</li>
 * </ul>
 *
 * <p>v14 H2 hardens quality by adding word-level caching, explicit exceptions,
 * and guards that skip automatic hyphenation for tokens that are usually ugly
 * or unsafe to break automatically: short words, all-caps tokens, numbers,
 * URLs, and already-hyphenated/code-like text.</p>
 */
public final class SimpleEnglishHyphenator implements Hyphenator {
    private static final int MIN_PREFIX = 3;
    private static final int MIN_SUFFIX = 3;
    private static final int MIN_HEURISTIC_LENGTH = 10;

    private static final Map<String, List<Integer>> EXCEPTION_POINTS = Map.ofEntries(
            Map.entry("demonstration", List.of(2, 5, 9)),
            Map.entry("paragraph", List.of(4)),
            Map.entry("hyphenation", List.of(2, 6)),
            Map.entry("typography", List.of(3, 7)),
            Map.entry("justification", List.of(4, 7, 9))
    );

    private final Map<String, List<Integer>> automaticPointCache = new ConcurrentHashMap<>();

    @Override
    public List<Integer> hyphenationPoints(String word) {
        if (word == null || word.isEmpty()) {
            return List.of();
        }

        Set<Integer> points = new TreeSet<>();
        collectSoftHyphenPoints(word, points);

        String visible = stripSoftHyphens(word);
        if (visible.isEmpty() || shouldSkipAutomaticHyphenation(visible)) {
            return List.copyOf(points);
        }

        String lower = visible.toLowerCase(Locale.ROOT);
        points.addAll(automaticPointCache.computeIfAbsent(lower, this::automaticPointsForVisibleWord));
        return List.copyOf(points);
    }

    private List<Integer> automaticPointsForVisibleWord(String lowerVisibleWord) {
        if (lowerVisibleWord.isEmpty()) {
            return List.of();
        }

        List<Integer> exception = EXCEPTION_POINTS.get(lowerVisibleWord);
        if (exception != null) {
            return exception;
        }

        if (lowerVisibleWord.length() < MIN_HEURISTIC_LENGTH || !isAlphabetic(lowerVisibleWord)) {
            return List.of();
        }

        Set<Integer> points = new LinkedHashSet<>();
        collectHeuristicPoints(lowerVisibleWord, points);
        return List.copyOf(new ArrayList<>(points));
    }

    private void collectSoftHyphenPoints(String word, Set<Integer> points) {
        int visibleIndex = 0;
        int visibleLength = visibleLength(word);
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            if (ch == '\u00AD') {
                if (visibleIndex > 0 && visibleIndex < visibleLength) {
                    points.add(visibleIndex);
                }
            } else {
                visibleIndex++;
            }
        }
    }

    private String stripSoftHyphens(String word) {
        return word.replace("\u00AD", "");
    }

    private int visibleLength(String word) {
        int count = 0;
        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) != '\u00AD') {
                count++;
            }
        }
        return count;
    }

    private boolean shouldSkipAutomaticHyphenation(String visibleWord) {
        if (visibleWord.length() < MIN_PREFIX + MIN_SUFFIX + 1) {
            return true;
        }
        if (looksLikeUrlishToken(visibleWord)) {
            return true;
        }
        if (visibleWord.indexOf('-') >= 0 || visibleWord.indexOf('_') >= 0 || visibleWord.indexOf('/') >= 0) {
            return true;
        }
        if (containsDigit(visibleWord)) {
            return true;
        }
        if (!isAlphabetic(visibleWord)) {
            return true;
        }
        return isAllUpperCase(visibleWord);
    }

    private void collectHeuristicPoints(String lower, Set<Integer> points) {
        for (int i = MIN_PREFIX; i <= lower.length() - MIN_SUFFIX; i++) {
            char left = lower.charAt(i - 1);
            char right = lower.charAt(i);

            if (!Character.isLetter(left) || !Character.isLetter(right)) {
                continue;
            }

            if (looksLikeVowelConsonantVowelBoundary(lower, i)
                    || looksLikeDoubledConsonantBoundary(lower, i)
                    || looksLikePrefixBoundary(lower, i)
                    || looksLikeSuffixBoundary(lower, i)) {
                points.add(i);
            }
        }
    }

    private boolean looksLikeVowelConsonantVowelBoundary(String lower, int i) {
        if (i < 2 || i >= lower.length()) {
            return false;
        }
        char a = lower.charAt(i - 2);
        char b = lower.charAt(i - 1);
        char c = lower.charAt(i);
        return isVowel(a) && !isVowel(b) && isVowel(c);
    }

    private boolean looksLikeDoubledConsonantBoundary(String lower, int i) {
        if (i < 2 || i + 1 >= lower.length()) {
            return false;
        }
        char prev = lower.charAt(i - 1);
        char next = lower.charAt(i);
        char after = lower.charAt(i + 1);
        return prev == next && !isVowel(prev) && isVowel(after);
    }

    private boolean looksLikePrefixBoundary(String lower, int i) {
        return switch (i) {
            case 2, 3, 4 -> lower.startsWith("pre") && i == 3
                    || lower.startsWith("pro") && i == 3
                    || lower.startsWith("con") && i == 3
                    || lower.startsWith("de") && i == 2
                    || lower.startsWith("re") && i == 2
                    || lower.startsWith("un") && i == 2
                    || lower.startsWith("sub") && i == 3;
            default -> false;
        };
    }

    private boolean looksLikeSuffixBoundary(String lower, int i) {
        if (lower.endsWith("ation") && i == lower.length() - 5) {
            return true;
        }
        if (lower.endsWith("ition") && i == lower.length() - 5) {
            return true;
        }
        if (lower.endsWith("tion") && i == lower.length() - 4) {
            return true;
        }
        if (lower.endsWith("ment") && i == lower.length() - 4) {
            return true;
        }
        return lower.endsWith("ing") && i == lower.length() - 3;
    }

    private boolean looksLikeUrlishToken(String word) {
        String lower = word.toLowerCase(Locale.ROOT);
        return lower.startsWith("http")
                || lower.startsWith("www")
                || lower.contains("://")
                || lower.contains(".");
    }

    private boolean containsDigit(String word) {
        for (int i = 0; i < word.length(); i++) {
            if (Character.isDigit(word.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean isAlphabetic(String word) {
        for (int i = 0; i < word.length(); i++) {
            if (!Character.isLetter(word.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isAllUpperCase(String word) {
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

    private boolean isVowel(char ch) {
        return switch (Character.toLowerCase(ch)) {
            case 'a', 'e', 'i', 'o', 'u', 'y' -> true;
            default -> false;
        };
    }
}
