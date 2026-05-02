package io.github.ggeorg.delos.writer.layout;

import java.util.List;
import java.util.Map;

/**
 * Result of one pagination pass together with the paragraph layout cache it produced.
 */
record LayoutComputation(
    Map<ParagraphLayoutKey, List<LaidOutLine>> paragraphLayouts,
    LaidOutDocument document
) { }
