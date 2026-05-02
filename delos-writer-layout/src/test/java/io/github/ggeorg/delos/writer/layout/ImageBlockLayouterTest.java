package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.ImageBlock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageBlockLayouterTest {
    private static final double EPSILON = 0.0001;

    @Test
    void usesDefaultSizeWhenImageHasNoExplicitSize() {
        ImageBlock image = new ImageBlock("media/image-1.png", 0, 0, "diagram");

        LaidOutImageBlock laidOut = new ImageBlockLayouter().layout(2, image, 10, 20, 500);

        assertEquals(2, laidOut.sourceBlockIndex());
        assertEquals(10, laidOut.x(), EPSILON);
        assertEquals(20, laidOut.y(), EPSILON);
        assertEquals(240, laidOut.width(), EPSILON);
        assertEquals(160, laidOut.height(), EPSILON);
        assertEquals("media/image-1.png", laidOut.source());
        assertEquals("diagram", laidOut.altText());
    }

    @Test
    void scalesOversizedImageToFitAvailableWidth() {
        ImageBlock image = new ImageBlock("media/wide.png", 600, 300, "wide image");

        LaidOutImageBlock laidOut = new ImageBlockLayouter().layout(4, image, 0, 0, 300);

        assertEquals(300, laidOut.width(), EPSILON);
        assertEquals(150, laidOut.height(), EPSILON);
    }

    @Test
    void preservesExplicitSizeWhenItFits() {
        ImageBlock image = new ImageBlock("media/small.png", 120, 90, "small image");

        LaidOutImageBlock laidOut = new ImageBlockLayouter().layout(1, image, 0, 0, 300);

        assertEquals(120, laidOut.width(), EPSILON);
        assertEquals(90, laidOut.height(), EPSILON);
    }
}
