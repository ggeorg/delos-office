package io.github.ggeorg.delos.javafx;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Small shared policy helper for pending-changes workflows.
 *
 * <p>This class is deliberately UI-toolkit neutral: applications provide the
 * prompt and save actions. Keeping the decision logic here lets Writer, Calc,
 * Slides, Base, and third-party Delos apps reuse the same tested behavior
 * without copying save/discard/cancel branching into each app shell.</p>
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
