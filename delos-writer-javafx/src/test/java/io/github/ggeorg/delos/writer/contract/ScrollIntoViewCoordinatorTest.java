package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.ui.ScrollIntoViewCoordinator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScrollIntoViewCoordinatorTest {
    @Test
    void keepsVisibleTargetInPlace() {
        double offset = ScrollIntoViewCoordinator.adjustedOffset(100.0, 300.0, 1200.0, 140.0, 180.0, 24.0);
        assertEquals(100.0, offset, 0.0001);
    }

    @Test
    void scrollsUpWhenTargetIsAboveComfortMargin() {
        double offset = ScrollIntoViewCoordinator.adjustedOffset(200.0, 300.0, 1200.0, 180.0, 220.0, 24.0);
        assertEquals(156.0, offset, 0.0001);
    }

    @Test
    void scrollsDownWhenTargetFallsBelowViewport() {
        double offset = ScrollIntoViewCoordinator.adjustedOffset(100.0, 300.0, 1200.0, 360.0, 410.0, 24.0);
        assertEquals(134.0, offset, 0.0001);
    }

    @Test
    void normalizedRoundTripMatchesCurrentOffset() {
        double current = ScrollIntoViewCoordinator.currentOffset(0.25, 200.0, 1000.0);
        assertEquals(200.0, current, 0.0001);
        assertEquals(0.25, ScrollIntoViewCoordinator.normalizedValue(current, 200.0, 1000.0), 0.0001);
    }
}
