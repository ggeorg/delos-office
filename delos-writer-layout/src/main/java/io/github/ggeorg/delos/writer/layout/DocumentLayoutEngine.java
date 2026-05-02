package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.Document;

/**
 * Converts the document model into a positioned layout tree.
 */
public interface DocumentLayoutEngine {
    LaidOutDocument layout(Document document, LayoutTheme theme);
}
