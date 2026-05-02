package io.github.ggeorg.delos.writer.ui;

import io.github.ggeorg.delos.writer.document.TextPosition;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaretFollowCoordinatorTest {
    @Test
    void revealsOnlyAfterRequestAndLayoutSync() {
        AtomicReference<TextPosition> caret = new AtomicReference<>(new TextPosition(0, 0));
        List<Bounds> revealed = new ArrayList<>();
        Queue<Runnable> scheduled = new ArrayDeque<>();
        Bounds expected = new BoundingBox(10.0, 20.0, 2.0, 15.0);

        CaretFollowCoordinator coordinator = new CaretFollowCoordinator(
                caret::get,
                position -> expected,
                scheduled::add
        );
        coordinator.setScrollIntoViewHandler(revealed::add);

        coordinator.requestFollowCaret();
        assertTrue(scheduled.isEmpty(), "request alone must not reveal before layout sync");

        coordinator.afterLayoutSync();
        assertEquals(1, scheduled.size());
        scheduled.remove().run();

        assertEquals(1, revealed.size());
        assertSameBounds(expected, revealed.getFirst());
    }

    @Test
    void collapsesBurstToFinalCaretBeforeScheduledFlushRuns() {
        AtomicReference<TextPosition> caret = new AtomicReference<>(new TextPosition(0, 0));
        List<TextPosition> resolvedCarets = new ArrayList<>();
        List<Bounds> revealed = new ArrayList<>();
        Queue<Runnable> scheduled = new ArrayDeque<>();

        CaretFollowCoordinator coordinator = new CaretFollowCoordinator(
                caret::get,
                position -> {
                    resolvedCarets.add(position);
                    return new BoundingBox(position.paragraphIndex(), position.offset(), 2.0, 15.0);
                },
                scheduled::add
        );
        coordinator.setScrollIntoViewHandler(revealed::add);

        coordinator.requestFollowCaret();
        coordinator.afterLayoutSync();
        caret.set(new TextPosition(3, 0));
        coordinator.requestFollowCaret();
        coordinator.afterLayoutSync();
        caret.set(new TextPosition(4, 2));
        coordinator.requestFollowCaret();
        coordinator.afterLayoutSync();

        assertEquals(1, scheduled.size(), "burst should schedule one reveal flush");
        scheduled.remove().run();

        assertEquals(List.of(new TextPosition(4, 2)), resolvedCarets);
        assertEquals(1, revealed.size());
        assertEquals(4.0, revealed.getFirst().getMinX());
        assertEquals(2.0, revealed.getFirst().getMinY());
    }

    @Test
    void waitsForHandlerWithoutDroppingPendingRequest() {
        AtomicReference<TextPosition> caret = new AtomicReference<>(new TextPosition(2, 1));
        List<Bounds> revealed = new ArrayList<>();
        Queue<Runnable> scheduled = new ArrayDeque<>();
        Bounds expected = new BoundingBox(30.0, 40.0, 2.0, 15.0);

        CaretFollowCoordinator coordinator = new CaretFollowCoordinator(
                caret::get,
                position -> expected,
                scheduled::add
        );

        coordinator.requestFollowCaret();
        coordinator.afterLayoutSync();
        assertTrue(scheduled.isEmpty(), "no handler means no scheduled reveal yet");

        coordinator.setScrollIntoViewHandler(revealed::add);
        assertEquals(1, scheduled.size());
        scheduled.remove().run();

        assertEquals(1, revealed.size());
        assertSameBounds(expected, revealed.getFirst());
    }

    private static void assertSameBounds(Bounds expected, Bounds actual) {
        assertEquals(expected.getMinX(), actual.getMinX());
        assertEquals(expected.getMinY(), actual.getMinY());
        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
    }
}
