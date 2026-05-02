package io.github.ggeorg.delos.writer.document;

/**
 * Logical caret/selection position in the document model.
 * <p>
 * Positions are expressed as paragraph index + character offset so they stay
 * stable across relayout and page fragmentation.
 */
public record TextPosition(
        int paragraphIndex,
        int offset
) implements Comparable<TextPosition> {
    @Override
    public int compareTo(TextPosition other) {
        if (other == null) {
            return 1;
        }

        int result = Integer.compare(paragraphIndex, other.paragraphIndex);
        if (result != 0) {
            return result;
        }

        return Integer.compare(offset, other.offset);
    }

    public static TextPosition min(TextPosition a, TextPosition b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static TextPosition max(TextPosition a, TextPosition b) {
        return a.compareTo(b) >= 0 ? a : b;
    }
}
