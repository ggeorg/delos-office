package io.github.ggeorg.delos.calc.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable workbook aggregate.
 */
public record Workbook(String title, List<Sheet> sheets) {
    public Workbook {
        title = Objects.requireNonNullElse(title, "Untitled");
        sheets = List.copyOf(Objects.requireNonNull(sheets, "sheets"));
        if (sheets.isEmpty()) {
            throw new IllegalArgumentException("Workbook must contain at least one sheet");
        }
        assertUniqueSheetNames(sheets);
    }

    public static Workbook blank() {
        return new Workbook("Untitled", List.of(Sheet.named("Sheet1")));
    }

    public Sheet firstSheet() {
        return sheets.get(0);
    }

    public Optional<Sheet> findSheet(String name) {
        String key = normalizeKey(name);
        return sheets.stream()
                .filter(sheet -> normalizeKey(sheet.name()).equals(key))
                .findFirst();
    }

    public Workbook withSheet(Sheet replacement) {
        Objects.requireNonNull(replacement, "replacement");
        List<Sheet> updated = new ArrayList<>(sheets.size());
        boolean replaced = false;
        String replacementKey = normalizeKey(replacement.name());
        for (Sheet sheet : sheets) {
            if (normalizeKey(sheet.name()).equals(replacementKey)) {
                updated.add(replacement);
                replaced = true;
            } else {
                updated.add(sheet);
            }
        }
        if (!replaced) {
            throw new IllegalArgumentException("No sheet named: " + replacement.name());
        }
        return new Workbook(title, updated);
    }

    public Workbook addSheet(Sheet sheet) {
        Objects.requireNonNull(sheet, "sheet");
        if (findSheet(sheet.name()).isPresent()) {
            throw new IllegalArgumentException("Duplicate sheet name: " + sheet.name());
        }
        List<Sheet> updated = new ArrayList<>(sheets);
        updated.add(sheet);
        return new Workbook(title, updated);
    }

    public Workbook withTitle(String title) {
        return new Workbook(title, sheets);
    }

    private static void assertUniqueSheetNames(List<Sheet> sheets) {
        List<String> seen = new ArrayList<>();
        for (Sheet sheet : sheets) {
            Objects.requireNonNull(sheet, "sheet");
            String key = normalizeKey(sheet.name());
            if (seen.contains(key)) {
                throw new IllegalArgumentException("Duplicate sheet name: " + sheet.name());
            }
            seen.add(key);
        }
    }

    private static String normalizeKey(String value) {
        return Objects.requireNonNullElse(value, "").trim().toLowerCase(Locale.ROOT);
    }
}
