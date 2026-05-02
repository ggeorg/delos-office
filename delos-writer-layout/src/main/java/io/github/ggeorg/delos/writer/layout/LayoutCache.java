package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.Paragraph;

import java.util.List;
import java.util.Map;

/**
 * Incremental pagination cache.
 */
record LayoutCache(
    LayoutInputs inputs,
    Map<ParagraphLayoutKey, List<LaidOutLine>> paragraphLayouts,
    LaidOutDocument document
) {
    List<Paragraph> paragraphs() {
        return inputs.paragraphs();
    }

    List<io.github.ggeorg.delos.writer.document.Block> blocks() {
        return inputs.blocks();
    }
}
