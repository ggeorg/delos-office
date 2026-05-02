package io.github.ggeorg.delos.writer.editor;

import java.util.Objects;

/** Code-point-safe helpers for UTF-16 editor offsets. */
public final class TextOffsets {
    private TextOffsets() {
    }

    public static int previousCodePointOffset(String text, int offset) {
        String safeText = Objects.requireNonNullElse(text, "");
        int safeOffset = clampOffset(safeText, offset);
        if (safeOffset <= 0) {
            return 0;
        }
        return safeText.offsetByCodePoints(safeOffset, -1);
    }

    public static int nextCodePointOffset(String text, int offset) {
        String safeText = Objects.requireNonNullElse(text, "");
        int safeOffset = clampOffset(safeText, offset);
        if (safeOffset >= safeText.length()) {
            return safeText.length();
        }
        return safeText.offsetByCodePoints(safeOffset, 1);
    }

    public static int clampOffset(String text, int offset) {
        String safeText = Objects.requireNonNullElse(text, "");
        return Math.max(0, Math.min(offset, safeText.length()));
    }
}
