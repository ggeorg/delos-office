package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.render.RenderFont;

/**
 * Cache key for the line layout of a single paragraph under a fixed font and width.
 */
record ParagraphLayoutKey(Paragraph paragraph, FontDescriptor font, double maxWidth, double lineGap) {
    static ParagraphLayoutKey of(Paragraph paragraph, RenderFont font, double maxWidth, double lineGap) {
        return new ParagraphLayoutKey(paragraph, FontDescriptor.from(font), maxWidth, lineGap);
    }
}
