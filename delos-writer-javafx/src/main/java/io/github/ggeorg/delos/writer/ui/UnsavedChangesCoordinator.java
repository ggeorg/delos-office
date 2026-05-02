package io.github.ggeorg.delos.writer.ui;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Small policy helper so pending-changes behavior stays testable and boring.
 */
public final class UnsavedChangesCoordinator {

    public enum Decision {
        SAVE,
        DISCARD,
        CANCEL
    }

    private UnsavedChangesCoordinator() {
    }

    public static boolean canProceed(boolean dirty,
                                     Supplier<Decision> decisionSupplier,
                                     BooleanSupplier saveAction) {
        Objects.requireNonNull(decisionSupplier, "decisionSupplier");
        Objects.requireNonNull(saveAction, "saveAction");
        if (!dirty) {
            return true;
        }

        Decision decision = decisionSupplier.get();
        if (decision == null || decision == Decision.CANCEL) {
            return false;
        }
        if (decision == Decision.DISCARD) {
            return true;
        }
        return saveAction.getAsBoolean();
    }
}
