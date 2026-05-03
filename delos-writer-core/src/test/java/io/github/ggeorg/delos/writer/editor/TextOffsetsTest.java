package io.github.ggeorg.delos.writer.editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TextOffsetsTest {
    @Test
    void previousCodePointOffsetDoesNotSplitSurrogatePairs() {
        String text = "A😀B";

        assertEquals(1, TextOffsets.previousCodePointOffset(text, 3));
        assertEquals(0, TextOffsets.previousCodePointOffset(text, 1));
    }

    @Test
    void nextCodePointOffsetDoesNotSplitSurrogatePairs() {
        String text = "A😀B";

        assertEquals(3, TextOffsets.nextCodePointOffset(text, 1));
        assertEquals(4, TextOffsets.nextCodePointOffset(text, 3));
    }

    @Test
    void offsetsClampAtTextBoundaries() {
        assertEquals(0, TextOffsets.previousCodePointOffset("abc", -100));
        assertEquals(3, TextOffsets.nextCodePointOffset("abc", 100));
    }
}
