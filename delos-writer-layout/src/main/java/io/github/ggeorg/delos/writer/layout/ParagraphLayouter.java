package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.render.RenderFont;

import java.util.List;

/**
 * Strategy boundary for converting a structured paragraph into visual lines.
 *
 * <p>v12 keeps the greedy implementation as the stable baseline while making
 * room for alternative strategies such as Knuth-Plass without disturbing the
 * pagination and rendering layers.</p>
 */
public interface ParagraphLayouter {
    List<LaidOutLine> layoutLines(Paragraph paragraph, RenderFont baseFont, double maxWidth, double lineGap);
}
