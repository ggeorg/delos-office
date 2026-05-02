package io.github.ggeorg.delos.slides.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SlideTest {
    @Test
    void blankSlideHasTitleElement() {
        Slide slide = Slide.blank("Intro");

        assertEquals("Intro", slide.title());
        assertEquals(1, slide.elementCount());
    }

    @Test
    void textBoxMustHavePositiveSize() {
        assertThrows(IllegalArgumentException.class,
                () -> new TextBoxElement("bad", 0, 0, 0, 20, "Bad"));
    }
}
