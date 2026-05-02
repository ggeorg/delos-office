package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.render.RenderFont;

/**
 * Stable cache descriptor for a render font.
 */
record FontDescriptor(String family, double size, boolean bold, boolean italic) {
    static FontDescriptor from(RenderFont font) {
        return new FontDescriptor(font.family(), font.size(), font.bold(), font.italic());
    }
}
