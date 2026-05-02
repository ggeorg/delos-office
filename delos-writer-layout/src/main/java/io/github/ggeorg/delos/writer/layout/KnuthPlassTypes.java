package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.CharacterStyle;

import java.util.Objects;

/**
 * Shared token/value types for Delos' non-greedy line breaking pipeline.
 *
 * <p>The vocabulary follows the Knuth-Plass model: boxes, glue, penalties, and
 * resolved breakpoints. Keeping these value types isolated lets paragraph
 * tokenization, scoring, and line materialization evolve independently.</p>
 */
public final class KnuthPlassTypes {
    public static final int POSITIVE_INFINITY = 10_000;
    public static final int FORCED_BREAK_PENALTY = -10_000;

    private KnuthPlassTypes() {
    }

    public interface Item {
        double width();
    }

    public record Box(
            String text,
            CharacterStyle style,
            int startOffset,
            int endOffset,
            double width
    ) implements Item {
        public Box {
            text = Objects.requireNonNullElse(text, "");
            style = Objects.requireNonNull(style, "style");
            if (startOffset < 0) {
                throw new IllegalArgumentException("startOffset must be >= 0");
            }
            if (endOffset < startOffset) {
                throw new IllegalArgumentException("endOffset must be >= startOffset");
            }
            if (width < 0) {
                throw new IllegalArgumentException("width must be >= 0");
            }
        }
    }

    public record Glue(
            String text,
            CharacterStyle style,
            int startOffset,
            int endOffset,
            double width,
            double stretch,
            double shrink
    ) implements Item {
        public Glue {
            text = Objects.requireNonNullElse(text, " ");
            style = Objects.requireNonNullElse(style, CharacterStyle.PLAIN);
            if (startOffset < 0) {
                throw new IllegalArgumentException("startOffset must be >= 0");
            }
            if (endOffset < startOffset) {
                throw new IllegalArgumentException("endOffset must be >= startOffset");
            }
            if (width < 0 || stretch < 0 || shrink < 0) {
                throw new IllegalArgumentException("glue width/stretch/shrink must be >= 0");
            }
        }

        public int breakOffset() {
            return endOffset;
        }
    }

    public record Penalty(
            String text,
            int offset,
            double width,
            int penalty,
            boolean flagged
    ) implements Item {
        public Penalty {
            text = Objects.requireNonNullElse(text, "");
            if (offset < 0) {
                throw new IllegalArgumentException("offset must be >= 0");
            }
            if (width < 0) {
                throw new IllegalArgumentException("width must be >= 0");
            }
        }

        public boolean forcedBreak() {
            return penalty <= FORCED_BREAK_PENALTY;
        }

        public boolean legalBreakpoint() {
            return penalty < POSITIVE_INFINITY;
        }

        public int breakOffset() {
            return offset;
        }
    }

    public record Breakpoint(
            int itemIndex,
            double adjustmentRatio,
            double demerits,
            int lineNumber
    ) {
        public Breakpoint {
            if (itemIndex < -1) {
                throw new IllegalArgumentException("itemIndex must be >= -1");
            }
            if (lineNumber < 0) {
                throw new IllegalArgumentException("lineNumber must be >= 0");
            }
        }
    }
}
