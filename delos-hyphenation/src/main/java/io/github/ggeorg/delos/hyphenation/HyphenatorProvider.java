package io.github.ggeorg.delos.hyphenation;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves paragraph language tags to hyphenators with safe fallback behavior.
 */
@FunctionalInterface
public interface HyphenatorProvider {
    Hyphenator forLanguageTag(String languageTag);

    static HyphenatorProvider defaults() {
        return DefaultProvider.INSTANCE;
    }

    static HyphenatorProvider fixed(Hyphenator hyphenator) {
        Hyphenator resolved = hyphenator == null ? Hyphenator.NONE : hyphenator;
        return ignored -> resolved;
    }

    final class DefaultProvider implements HyphenatorProvider {
        private static final DefaultProvider INSTANCE = new DefaultProvider();

        private final Map<String, Hyphenator> cache = new ConcurrentHashMap<>();

        private DefaultProvider() {
        }

        @Override
        public Hyphenator forLanguageTag(String languageTag) {
            String normalized = normalize(languageTag);
            if (normalized == null) {
                return Hyphenator.NONE;
            }
            return cache.computeIfAbsent(normalized, this::loadOrNone);
        }

        private Hyphenator loadOrNone(String normalizedLanguageTag) {
            try {
                return DefaultHyphenators.forLanguageTag(normalizedLanguageTag);
            } catch (IllegalArgumentException ex) {
                return Hyphenator.NONE;
            }
        }

        private static String normalize(String languageTag) {
            if (languageTag == null) {
                return null;
            }
            String candidate = languageTag.trim().replace('_', '-');
            if (candidate.isEmpty()) {
                return null;
            }
            String normalized = Locale.forLanguageTag(candidate).toLanguageTag();
            return "und".equals(normalized) ? candidate.toLowerCase(Locale.ROOT) : normalized;
        }
    }
}
