package io.github.ggeorg.delos.writer.ui;

import io.github.ggeorg.delos.writer.document.TextPosition;
import javafx.application.Platform;
import javafx.geometry.Bounds;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Coordinates caret-follow requests after document/page layout has been
 * synchronized.
 * <p>
 * Edit and navigation code only asks to follow the caret. The coordinator waits
 * until the viewport reports that the current layout is synchronized, then
 * performs one debounced reveal using model-derived caret bounds. The JavaFX
 * scheduler is used only to collapse bursts in the same pulse; correctness does
 * not depend on scene-graph layout transforms being current.
 */
public final class CaretFollowCoordinator {
    private final Supplier<TextPosition> caretSupplier;
    private final Function<TextPosition, Bounds> caretBoundsProvider;
    private final Consumer<Runnable> scheduler;

    private Consumer<Bounds> scrollIntoViewHandler;
    private boolean followCaretRequested;
    private boolean layoutSynchronized;
    private boolean flushScheduled;

    public CaretFollowCoordinator(
            Supplier<TextPosition> caretSupplier,
            Function<TextPosition, Bounds> caretBoundsProvider
    ) {
        this(caretSupplier, caretBoundsProvider, Platform::runLater);
    }

    CaretFollowCoordinator(
            Supplier<TextPosition> caretSupplier,
            Function<TextPosition, Bounds> caretBoundsProvider,
            Consumer<Runnable> scheduler
    ) {
        this.caretSupplier = Objects.requireNonNull(caretSupplier, "caretSupplier");
        this.caretBoundsProvider = Objects.requireNonNull(caretBoundsProvider, "caretBoundsProvider");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    public void setScrollIntoViewHandler(Consumer<Bounds> scrollIntoViewHandler) {
        this.scrollIntoViewHandler = scrollIntoViewHandler;
        scheduleFlushIfReady();
    }

    public void requestFollowCaret() {
        followCaretRequested = true;
        scheduleFlushIfReady();
    }

    public void afterLayoutSync() {
        layoutSynchronized = true;
        scheduleFlushIfReady();
    }

    public void clear() {
        followCaretRequested = false;
        layoutSynchronized = false;
    }

    public boolean isFollowCaretRequested() {
        return followCaretRequested;
    }

    private void scheduleFlushIfReady() {
        if (flushScheduled || !followCaretRequested || !layoutSynchronized || scrollIntoViewHandler == null) {
            return;
        }
        flushScheduled = true;
        scheduler.accept(this::flush);
    }

    private void flush() {
        flushScheduled = false;
        if (!followCaretRequested || !layoutSynchronized || scrollIntoViewHandler == null) {
            scheduleFlushIfReady();
            return;
        }

        TextPosition caret = caretSupplier.get();
        Bounds bounds = caret == null ? null : caretBoundsProvider.apply(caret);
        followCaretRequested = false;
        layoutSynchronized = false;

        if (bounds != null) {
            scrollIntoViewHandler.accept(bounds);
        }
    }
}
