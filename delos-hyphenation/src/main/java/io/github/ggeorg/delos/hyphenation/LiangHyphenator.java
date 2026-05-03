package io.github.ggeorg.delos.hyphenation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

/**
 * Dependency-free Liang-pattern hyphenator used by Delos text layout.
 *
 * <p>The implementation applies the same core idea as TeX hyphenation patterns:
 * pattern digits contribute weighted values at candidate word boundaries; odd
 * values become legal discretionary hyphen positions. Pattern data is loaded
 * from classpath resources so language data can evolve independently from the
 * paragraph layout engine.</p>
 *
 * <p>Pattern lookup is trie-based. This avoids scanning every pattern for every
 * word and keeps runtime proportional to the word length times the maximum
 * pattern depth.</p>
 */
public final class LiangHyphenator implements Hyphenator {
    private static final String HYPHENATION_RESOURCE_ROOT =
            "/io/github/ggeorg/delos/hyphenation/patterns/";
    private static final String ENGLISH_PATTERNS_RESOURCE = HYPHENATION_RESOURCE_ROOT + "en-US.tex";

    private static final int DEFAULT_MAX_CACHED_WORDS = 20_000;
    private static final int MAX_CACHEABLE_WORD_LENGTH = 80;

    private final PatternTrie trie;
    private final HyphenationPolicy policy;
    private final HyphenationCache cache;

    public LiangHyphenator(List<String> patternLines, int leftMin, int rightMin) {
        this(patternLines, new HyphenationPolicy(
                leftMin,
                rightMin,
                Math.max(leftMin + rightMin + 1, 1),
                true,
                HyphenationPolicy.english().explicitSoftHyphenPenalty(),
                HyphenationPolicy.english().automaticHyphenPenalty(),
                HyphenationPolicy.english().capitalizedWordPenalty()
        ));
    }

    public LiangHyphenator(List<String> patternLines, HyphenationPolicy policy) {
        this(patternLines, policy, DEFAULT_MAX_CACHED_WORDS);
    }

    LiangHyphenator(List<String> patternLines, HyphenationPolicy policy, int maxCachedWords) {
        this(PatternTrie.compile(patternLines), policy, maxCachedWords);
    }

    private LiangHyphenator(PatternTrie trie, HyphenationPolicy policy) {
        this(trie, policy, DEFAULT_MAX_CACHED_WORDS);
    }

    private LiangHyphenator(PatternTrie trie, HyphenationPolicy policy, int maxCachedWords) {
        this.trie = Objects.requireNonNull(trie, "trie");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.cache = new HyphenationCache(maxCachedWords);
    }

    public static LiangHyphenator english() {
        return english(HyphenationPolicy.english());
    }

    public static LiangHyphenator english(HyphenationPolicy policy) {
        return fromClasspathResource(ENGLISH_PATTERNS_RESOURCE, policy);
    }

    public static LiangHyphenator forLanguageTag(String languageTag) {
        return forLanguageTag(languageTag, HyphenationPolicy.english());
    }

    public static LiangHyphenator forLanguageTag(String languageTag, HyphenationPolicy policy) {
        String normalized = normalizeLanguageTag(languageTag);
        if (normalized.equals("en") || normalized.equals("en-us")) {
            return english(policy);
        }
        return fromClasspathResource(HYPHENATION_RESOURCE_ROOT + resourceName(normalized) + ".tex", policy);
    }

    public static LiangHyphenator fromClasspathResource(String resource, int leftMin, int rightMin) {
        return fromClasspathResource(resource, new HyphenationPolicy(
                leftMin,
                rightMin,
                Math.max(leftMin + rightMin + 1, 1),
                true,
                HyphenationPolicy.english().explicitSoftHyphenPenalty(),
                HyphenationPolicy.english().automaticHyphenPenalty(),
                HyphenationPolicy.english().capitalizedWordPenalty()
        ));
    }

    public static LiangHyphenator fromClasspathResource(String resource, HyphenationPolicy policy) {
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(policy, "policy");
        try (InputStream stream = LiangHyphenator.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalArgumentException("Hyphenation pattern resource not found: " + resource);
            }
            return fromPatternStream(stream, policy);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static LiangHyphenator fromPatternStream(InputStream stream, int leftMin, int rightMin) throws IOException {
        return fromPatternStream(stream, new HyphenationPolicy(
                leftMin,
                rightMin,
                Math.max(leftMin + rightMin + 1, 1),
                true,
                HyphenationPolicy.english().explicitSoftHyphenPenalty(),
                HyphenationPolicy.english().automaticHyphenPenalty(),
                HyphenationPolicy.english().capitalizedWordPenalty()
        ));
    }

    public static LiangHyphenator fromPatternStream(InputStream stream, HyphenationPolicy policy) throws IOException {
        Objects.requireNonNull(stream, "stream");
        Objects.requireNonNull(policy, "policy");
        PatternFile patternFile = PatternFile.read(stream);
        return new LiangHyphenator(PatternTrie.compile(patternFile.patterns()), policy);
    }

    public HyphenationPolicy policy() {
        return policy;
    }

    @Override
    public List<Integer> hyphenationPoints(String word) {
        if (word == null || word.isEmpty()) {
            return List.of();
        }
        if (!isCacheable(word)) {
            return computeHyphenationPoints(word);
        }
        List<Integer> cached = cache.get(word);
        if (cached != null) {
            return cached;
        }
        return cache.putIfAbsent(word, computeHyphenationPoints(word));
    }

    int cachedWordCount() {
        return cache.size();
    }

    boolean hasCachedWord(String word) {
        return cache.contains(word);
    }

    private boolean isCacheable(String sourceWord) {
        if (sourceWord.length() > MAX_CACHEABLE_WORD_LENGTH) {
            return false;
        }
        VisibleWord visibleWord = VisibleWord.from(sourceWord);
        String visible = visibleWord.text();
        if (visible.length() < policy.minWordLength() || visible.length() > MAX_CACHEABLE_WORD_LENGTH) {
            return false;
        }
        return policy.allowsAutomaticHyphenation(visible);
    }

    private List<Integer> computeHyphenationPoints(String sourceWord) {
        VisibleWord visibleWord = VisibleWord.from(sourceWord);
        String visible = visibleWord.text();
        if (visible.length() < policy.leftMin() + policy.rightMin() + 1) {
            return visibleWord.explicitSoftHyphenPoints();
        }
        if (!policy.allowsAutomaticHyphenation(visible)) {
            return visibleWord.explicitSoftHyphenPoints();
        }

        int visibleLength = visible.length();
        int[] values = new int[visibleLength + 3];
        String candidate = "." + visible.toLowerCase(Locale.ROOT) + ".";
        trie.apply(candidate, values);

        Set<Integer> points = new TreeSet<>(visibleWord.explicitSoftHyphenPoints());
        for (int point = policy.leftMin(); point <= visibleLength - policy.rightMin(); point++) {
            int valueIndex = point + 1; // account for leading '.' in the candidate word
            if (valueIndex >= 0 && valueIndex < values.length && values[valueIndex] % 2 == 1) {
                points.add(point);
            }
        }
        return List.copyOf(points);
    }

    private static String normalizeLanguageTag(String languageTag) {
        return languageTag == null || languageTag.isBlank()
                ? "en-us"
                : languageTag.trim().replace('_', '-').toLowerCase(Locale.ROOT);
    }

    private static String resourceName(String normalizedLanguageTag) {
        if (normalizedLanguageTag.equals("en-us")) {
            return "en-US";
        }
        return normalizedLanguageTag;
    }

    private static final class HyphenationCache {
        private final int maxEntries;
        private final LinkedHashMap<String, List<Integer>> entries;

        HyphenationCache(int maxEntries) {
            this.maxEntries = Math.max(0, maxEntries);
            this.entries = new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<Integer>> eldest) {
                    return HyphenationCache.this.maxEntries > 0
                            && size() > HyphenationCache.this.maxEntries;
                }
            };
        }

        synchronized List<Integer> get(String word) {
            return entries.get(word);
        }

        synchronized List<Integer> putIfAbsent(String word, List<Integer> points) {
            if (maxEntries == 0) {
                return List.copyOf(points);
            }
            List<Integer> existing = entries.get(word);
            if (existing != null) {
                return existing;
            }
            List<Integer> computed = List.copyOf(points);
            entries.put(word, computed);
            return computed;
        }

        synchronized int size() {
            return entries.size();
        }

        synchronized boolean contains(String word) {
            return entries.containsKey(word);
        }
    }

    private static final class PatternFile {
        private static final java.util.regex.Pattern TOKEN_PATTERN =
                java.util.regex.Pattern.compile("[\\p{L}.0-9]+");

        private final List<String> patterns;

        private PatternFile(List<String> patterns) {
            this.patterns = List.copyOf(patterns);
        }

        static PatternFile read(InputStream stream) throws IOException {
            StringBuilder cleaned = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    cleaned.append(stripTexComment(line)).append('\n');
                }
            }
            return new PatternFile(tokenize(extractPatternBody(cleaned.toString())));
        }

        List<String> patterns() {
            return patterns;
        }

        private static String stripTexComment(String line) {
            int comment = -1;
            boolean escaped = false;
            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (ch == '%' && !escaped) {
                    comment = i;
                    break;
                }
                escaped = ch == '\\' && !escaped;
                if (ch != '\\') {
                    escaped = false;
                }
            }
            return comment < 0 ? line : line.substring(0, comment);
        }

        private static String extractPatternBody(String text) {
            StringBuilder body = new StringBuilder();
            int searchFrom = 0;
            while (searchFrom < text.length()) {
                int command = text.indexOf("\\patterns", searchFrom);
                if (command < 0) {
                    break;
                }
                int open = text.indexOf('{', command);
                if (open < 0) {
                    break;
                }
                int close = matchingBrace(text, open);
                if (close < 0) {
                    break;
                }
                body.append(text, open + 1, close).append('\n');
                searchFrom = close + 1;
            }
            if (!body.isEmpty()) {
                return body.toString();
            }
            return text;
        }

        private static int matchingBrace(String text, int open) {
            int depth = 0;
            for (int i = open; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (ch == '{') {
                    depth++;
                } else if (ch == '}') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
            return -1;
        }

        private static List<String> tokenize(String text) {
            List<String> tokens = new ArrayList<>();
            Matcher matcher = TOKEN_PATTERN.matcher(text);
            while (matcher.find()) {
                String token = matcher.group();
                if (containsLetter(token) && containsPatternSyntax(token)) {
                    tokens.add(token);
                }
            }
            return tokens;
        }

        private static boolean containsLetter(String token) {
            for (int i = 0; i < token.length(); i++) {
                if (Character.isLetter(token.charAt(i))) {
                    return true;
                }
            }
            return false;
        }

        private static boolean containsPatternSyntax(String token) {
            for (int i = 0; i < token.length(); i++) {
                char ch = token.charAt(i);
                if (Character.isDigit(ch) || ch == '.') {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class PatternTrie {
        private final Node root;

        private PatternTrie(Node root) {
            this.root = root;
        }

        static PatternTrie compile(List<String> patternTokens) {
            Objects.requireNonNull(patternTokens, "patternTokens");
            Node root = new Node();
            for (String token : patternTokens) {
                Pattern pattern = Pattern.compile(token);
                if (pattern != null) {
                    root.add(pattern.letters(), pattern.values());
                }
            }
            return new PatternTrie(root);
        }

        void apply(String candidate, int[] values) {
            for (int start = 0; start < candidate.length(); start++) {
                Node node = root;
                for (int index = start; index < candidate.length(); index++) {
                    node = node.child(candidate.charAt(index));
                    if (node == null) {
                        break;
                    }
                    if (node.values != null) {
                        for (int i = 0; i < node.values.length; i++) {
                            int valueIndex = start + i;
                            if (valueIndex >= 0 && valueIndex < values.length) {
                                values[valueIndex] = Math.max(values[valueIndex], node.values[i]);
                            }
                        }
                    }
                }
            }
        }

        private static final class Node {
            private final Map<Character, Node> children = new HashMap<>();
            private int[] values;

            Node child(char ch) {
                return children.get(ch);
            }

            void add(String letters, int[] patternValues) {
                Node node = this;
                for (int i = 0; i < letters.length(); i++) {
                    char ch = letters.charAt(i);
                    node = node.children.computeIfAbsent(ch, unused -> new Node());
                }
                node.values = patternValues;
            }
        }
    }

    private record Pattern(String letters, int[] values) {
        private static Pattern compile(String token) {
            if (token == null || token.isBlank()) {
                return null;
            }

            String normalized = token.trim().toLowerCase(Locale.ROOT);
            StringBuilder letters = new StringBuilder();
            List<Integer> values = new ArrayList<>();
            values.add(0);

            for (int i = 0; i < normalized.length(); i++) {
                char ch = normalized.charAt(i);
                if (Character.isDigit(ch)) {
                    values.set(values.size() - 1, Character.digit(ch, 10));
                } else if (Character.isLetter(ch) || ch == '.') {
                    letters.append(ch);
                    values.add(0);
                }
            }

            if (letters.isEmpty()) {
                return null;
            }

            int[] valueArray = new int[values.size()];
            for (int i = 0; i < values.size(); i++) {
                valueArray[i] = values.get(i);
            }
            return new Pattern(letters.toString(), valueArray);
        }
    }

    private record VisibleWord(String text, List<Integer> explicitSoftHyphenPoints) {
        private static VisibleWord from(String source) {
            StringBuilder visible = new StringBuilder();
            Set<Integer> softHyphenPoints = new TreeSet<>();
            int visibleIndex = 0;
            int visibleLength = 0;
            for (int i = 0; i < source.length(); i++) {
                if (source.charAt(i) != '\u00AD') {
                    visibleLength++;
                }
            }
            for (int i = 0; i < source.length(); i++) {
                char ch = source.charAt(i);
                if (ch == '\u00AD') {
                    if (visibleIndex > 0 && visibleIndex < visibleLength) {
                        softHyphenPoints.add(visibleIndex);
                    }
                } else {
                    visible.append(ch);
                    visibleIndex++;
                }
            }
            return new VisibleWord(visible.toString(), List.copyOf(softHyphenPoints));
        }
    }
}
