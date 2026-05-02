package io.github.ggeorg.delos.writer.layout;

import java.util.List;

/**
 * Returns discretionary hyphenation points inside a word.
 *
 * <p>The returned indices are character offsets within the supplied word where
 * a discretionary line break may occur. For a returned index {@code i}, the
 * word may break between {@code word.substring(0, i)} and
 * {@code word.substring(i)}.</p>
 */
public interface Hyphenator {
    Hyphenator NONE = word -> List.of();

    List<Integer> hyphenationPoints(String word);
}
