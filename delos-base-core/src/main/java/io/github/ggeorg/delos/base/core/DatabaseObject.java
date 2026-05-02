package io.github.ggeorg.delos.base.core;

import java.util.Objects;

/**
 * Immutable database object entry.
 */
public record DatabaseObject(DatabaseObjectKind kind, String name) {
    public DatabaseObject {
        kind = Objects.requireNonNull(kind, "kind");
        name = normalizeName(name);
    }

    public static DatabaseObject table(String name) {
        return new DatabaseObject(DatabaseObjectKind.TABLE, name);
    }

    public static DatabaseObject query(String name) {
        return new DatabaseObject(DatabaseObjectKind.QUERY, name);
    }

    public static DatabaseObject form(String name) {
        return new DatabaseObject(DatabaseObjectKind.FORM, name);
    }

    public static DatabaseObject report(String name) {
        return new DatabaseObject(DatabaseObjectKind.REPORT, name);
    }

    public String displayName() {
        return kind.displayName() + " / " + name;
    }

    @Override
    public String toString() {
        return displayName();
    }

    private static String normalizeName(String value) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Database object name must not be blank");
        }
        return normalized;
    }
}
