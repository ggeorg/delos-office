package io.github.ggeorg.delos.javafx.command;

import javafx.scene.input.KeyCombination;

import java.util.Objects;
import java.util.function.BooleanSupplier;

public record EditorCommand(
        String id,
        String label,
        String category,
        KeyCombination accelerator,
        Runnable action,
        BooleanSupplier enabled,
        BooleanSupplier active
) {
    public EditorCommand {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(action, "action");
    }

    public boolean isEnabled() {
        return enabled == null || enabled.getAsBoolean();
    }

    public boolean isActive() {
        return active != null && active.getAsBoolean();
    }

    public String acceleratorText() {
        return accelerator != null ? accelerator.getDisplayText() : "";
    }

    public void execute() {
        if (isEnabled()) {
            action.run();
        }
    }
}
