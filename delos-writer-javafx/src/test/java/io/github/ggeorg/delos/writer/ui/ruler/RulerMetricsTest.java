package io.github.ggeorg.delos.writer.ui.ruler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RulerMetricsTest {
    @Test
    void horizontalRulerCentersPageWhenContentFitsViewport() {
        assertEquals(100.0, RulerMetrics.horizontalPageLeft(800.0, 600.0, 1.0, 0.0), 0.0001);
        assertEquals(25.0, RulerMetrics.horizontalPageLeft(800.0, 600.0, 1.25, 0.0), 0.0001);
    }

    @Test
    void horizontalRulerTracksHorizontalScrollWhenContentIsWiderThanViewport() {
        assertEquals(-40.0, RulerMetrics.horizontalPageLeft(500.0, 600.0, 1.0, 40.0), 0.0001);
        assertEquals(-80.0, RulerMetrics.horizontalPageLeft(500.0, 600.0, 2.0, 40.0), 0.0001);
    }

    @Test
    void verticalRulerComputesVisiblePagesFromOuterPaddingAndPageGap() {
        RulerMetrics.PageRange range = RulerMetrics.visiblePageRange(
                870.0,
                500.0,
                842.0,
                28.0,
                18.0,
                1.0
        );

        assertEquals(0, range.firstPage());
        assertEquals(2, range.lastPage());
    }

    @Test
    void verticalRulerComputesPageTopInViewport() {
        assertEquals(18.0, RulerMetrics.pageTopInViewport(0, 842.0, 28.0, 18.0, 0.0, 1.0), 0.0001);
        assertEquals(888.0, RulerMetrics.pageTopInViewport(1, 842.0, 28.0, 18.0, 0.0, 1.0), 0.0001);
        assertEquals(38.0, RulerMetrics.pageTopInViewport(1, 842.0, 28.0, 18.0, 850.0, 1.0), 0.0001);
    }
}
