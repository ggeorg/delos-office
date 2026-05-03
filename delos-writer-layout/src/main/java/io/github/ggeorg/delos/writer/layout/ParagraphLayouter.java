package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.render.RenderFont;

import java.util.List;

/**
 * Strategy boundary for converting a structured paragraph into visual lines.
 */
public interface ParagraphLayouter {
    List<LaidOutLine> layoutLines(Paragraph paragraph, RenderFont baseFont, double maxWidth, double lineGap);
}
