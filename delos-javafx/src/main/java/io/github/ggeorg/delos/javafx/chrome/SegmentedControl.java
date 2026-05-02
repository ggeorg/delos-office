package io.github.ggeorg.delos.javafx.chrome;

import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Small Pages-style segmented selector used inside inspectors and compact app chrome.
 *
 * <p>The control owns only generic selection UI. Application-specific behavior remains
 * in the caller via {@link #setOnSelectionChanged(Consumer)}.</p>
 */
public class SegmentedControl extends HBox {
    private final ToggleGroup group = new ToggleGroup();
    private final Map<String, ToggleButton> buttonsById = new LinkedHashMap<>();
    private Consumer<String> onSelectionChanged;
    private boolean selecting;

    public SegmentedControl(Collection<SegmentedOption> options) {
        super(0.0);
        getStyleClass().add("delos-segmented-control");
        Objects.requireNonNull(options, "options");
        if (options.isEmpty()) {
            throw new IllegalArgumentException("SegmentedControl requires at least one option");
        }
        for (SegmentedOption option : options) {
            addOption(option);
        }
        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (selecting) {
                return;
            }
            if (newToggle == null && oldToggle != null) {
                selecting = true;
                try {
                    oldToggle.setSelected(true);
                } finally {
                    selecting = false;
                }
                return;
            }
            selectedId().ifPresent(id -> {
                if (onSelectionChanged != null) {
                    onSelectionChanged.accept(id);
                }
            });
        });
        select(options.iterator().next().id());
    }

    public Optional<String> selectedId() {
        if (group.getSelectedToggle() == null || group.getSelectedToggle().getUserData() == null) {
            return Optional.empty();
        }
        return Optional.of(group.getSelectedToggle().getUserData().toString());
    }

    public void select(String id) {
        ToggleButton button = buttonsById.get(Objects.requireNonNull(id, "id"));
        if (button == null) {
            throw new IllegalArgumentException("Unknown segmented option: " + id);
        }
        selecting = true;
        try {
            group.selectToggle(button);
        } finally {
            selecting = false;
        }
    }

    public void setOnSelectionChanged(Consumer<String> onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
    }

    public int optionCount() {
        return buttonsById.size();
    }

    private void addOption(SegmentedOption option) {
        ToggleButton button = new ToggleButton(option.title());
        button.setUserData(option.id());
        button.setToggleGroup(group);
        button.setFocusTraversable(false);
        button.getStyleClass().add("delos-segmented-button");
        if (buttonsById.putIfAbsent(option.id(), button) != null) {
            throw new IllegalArgumentException("Duplicate segmented option id: " + option.id());
        }
        getChildren().add(button);
    }
}
