package io.github.ggeorg.delos.writer.ui;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UnsavedChangesCoordinatorTest {

    @Test
    void cleanDocumentsProceedWithoutPromptingOrSaving() {
        AtomicInteger prompted = new AtomicInteger();
        AtomicInteger saved = new AtomicInteger();

        boolean proceed = UnsavedChangesCoordinator.canProceed(
                false,
                () -> {
                    prompted.incrementAndGet();
                    return UnsavedChangesCoordinator.Decision.CANCEL;
                },
                () -> {
                    saved.incrementAndGet();
                    return false;
                });

        assertTrue(proceed);
        assertTrue(prompted.get() == 0);
        assertTrue(saved.get() == 0);
    }

    @Test
    void cancelBlocksNavigationWhenDocumentIsDirty() {
        AtomicBoolean saved = new AtomicBoolean(false);

        boolean proceed = UnsavedChangesCoordinator.canProceed(
                true,
                () -> UnsavedChangesCoordinator.Decision.CANCEL,
                () -> {
                    saved.set(true);
                    return true;
                });

        assertFalse(proceed);
        assertFalse(saved.get());
    }

    @Test
    void saveDecisionUsesSaveResult() {
        boolean proceed = UnsavedChangesCoordinator.canProceed(
                true,
                () -> UnsavedChangesCoordinator.Decision.SAVE,
                () -> true);
        assertTrue(proceed);

        boolean blocked = UnsavedChangesCoordinator.canProceed(
                true,
                () -> UnsavedChangesCoordinator.Decision.SAVE,
                () -> false);
        assertFalse(blocked);
    }

    @Test
    void discardAllowsNavigationWithoutSaving() {
        AtomicBoolean saved = new AtomicBoolean(false);

        boolean proceed = UnsavedChangesCoordinator.canProceed(
                true,
                () -> UnsavedChangesCoordinator.Decision.DISCARD,
                () -> {
                    saved.set(true);
                    return true;
                });

        assertTrue(proceed);
        assertFalse(saved.get());
    }
}
