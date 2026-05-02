package io.github.ggeorg.delos.javafx.command;

import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CommandRegistry {
    private final List<EditorCommand> commands = new ArrayList<>();
    private final Map<String, EditorCommand> byId = new LinkedHashMap<>();
    private final Map<KeyCombination, EditorCommand> byKey = new LinkedHashMap<>();

    public void register(EditorCommand command) {
        Objects.requireNonNull(command, "command");
        if (byId.containsKey(command.id())) {
            throw new IllegalArgumentException("Duplicate command id: " + command.id());
        }
        if (command.accelerator() != null && byKey.containsKey(command.accelerator())) {
            throw new IllegalArgumentException("Duplicate accelerator: " + command.accelerator());
        }
        commands.add(command);
        byId.put(command.id(), command);
        if (command.accelerator() != null) {
            byKey.put(command.accelerator(), command);
        }
    }

    public List<EditorCommand> all() {
        return List.copyOf(commands);
    }

    public Optional<EditorCommand> byId(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<EditorCommand> byAccelerator(KeyCombination keyCombination) {
        return Optional.ofNullable(byKey.get(keyCombination));
    }

    public void installAccelerators(Scene scene) {
        if (scene == null) {
            return;
        }
        byKey.forEach((keyCombination, command) -> scene.getAccelerators().put(keyCombination, command::execute));
    }

    public void uninstallAccelerators(Scene scene) {
        if (scene == null) {
            return;
        }
        byKey.keySet().forEach(scene.getAccelerators()::remove);
    }

    public List<EditorCommand> search(String query) {
        if (query == null || query.isBlank()) {
            return all();
        }
        String trimmed = query.trim();
        return commands.stream()
                .map(command -> new ScoredCommand(command, fuzzyScore(command.label(), trimmed)))
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator
                        .comparingInt(ScoredCommand::score).reversed()
                        .thenComparing(scored -> scored.command().label(), String.CASE_INSENSITIVE_ORDER))
                .map(ScoredCommand::command)
                .toList();
    }

    static int fuzzyScore(String label, String query) {
        String lower = label.toLowerCase(Locale.ROOT);
        String queryLower = query.toLowerCase(Locale.ROOT);
        if (lower.startsWith(queryLower)) {
            return 1_000 + queryLower.length();
        }
        int score = 0;
        int labelIndex = 0;
        for (int qi = 0; qi < queryLower.length(); qi++) {
            char expected = queryLower.charAt(qi);
            boolean found = false;
            while (labelIndex < lower.length()) {
                char actual = lower.charAt(labelIndex);
                if (actual == expected) {
                    boolean boundary = labelIndex == 0 || !Character.isLetterOrDigit(lower.charAt(labelIndex - 1));
                    score += boundary ? 10 : 1;
                    labelIndex++;
                    found = true;
                    break;
                }
                labelIndex++;
            }
            if (!found) {
                return 0;
            }
        }
        return score;
    }

    private record ScoredCommand(EditorCommand command, int score) {
    }
}
