package io.github.ggeorg.delos.writer.render;

/**
 * Conservative margin protrusion policy for optical punctuation hanging.
 *
 * <p>This second slice remains modest, but it is more robust than the initial
 * baseline: it supports edge punctuation clusters and ignores surrounding
 * whitespace so mixed-style edge runs can still benefit.</p>
 */
public final class MarginProtrusionPolicy {
    public static final MarginProtrusionPolicy DEFAULT = new MarginProtrusionPolicy();

    public double leadingFraction(char ch) {
        return switch (ch) {
            case '"', '\'', '“', '‘', '«', '‹' -> 0.35;
            case '(', '[', '{' -> 0.22;
            default -> 0.0;
        };
    }

    public double trailingFraction(char ch) {
        return switch (ch) {
            case '.', ',' -> 0.30;
            case ';', ':' -> 0.18;
            case '!', '?' -> 0.22;
            case '"', '\'', '”', '’', '»', '›' -> 0.28;
            case ')', ']', '}' -> 0.18;
            case '-', '‐', '‑', '‒', '–', '—' -> 0.18;
            default -> 0.0;
        };
    }

    public boolean hasLeadingProtrusion(String text) {
        return leadingCluster(text).length() > 0;
    }

    public boolean hasTrailingProtrusion(String text) {
        return trailingCluster(text).length() > 0;
    }

    public int firstVisibleIndex(String text) {
        if (text == null || text.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    public int lastVisibleIndex(String text) {
        if (text == null || text.isEmpty()) {
            return -1;
        }
        for (int i = text.length() - 1; i >= 0; i--) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    public EdgeCluster leadingCluster(String text) {
        int start = firstVisibleIndex(text);
        if (start < 0) {
            return EdgeCluster.empty();
        }
        int endExclusive = start;
        while (endExclusive < text.length() && leadingFraction(text.charAt(endExclusive)) > 0) {
            endExclusive++;
        }
        return endExclusive > start ? new EdgeCluster(start, endExclusive) : EdgeCluster.empty();
    }

    public EdgeCluster trailingCluster(String text) {
        int endInclusive = lastVisibleIndex(text);
        if (endInclusive < 0) {
            return EdgeCluster.empty();
        }
        int start = endInclusive;
        while (start >= 0 && trailingFraction(text.charAt(start)) > 0) {
            start--;
        }
        start += 1;
        return start <= endInclusive ? new EdgeCluster(start, endInclusive + 1) : EdgeCluster.empty();
    }

    public record EdgeCluster(int startInclusive, int endExclusive) {
        public EdgeCluster {
            if (startInclusive < 0 || endExclusive < startInclusive) {
                throw new IllegalArgumentException("invalid edge cluster range");
            }
        }

        public static EdgeCluster empty() {
            return new EdgeCluster(0, 0);
        }

        public boolean isEmpty() {
            return startInclusive == endExclusive;
        }

        public int length() {
            return endExclusive - startInclusive;
        }
    }
}
