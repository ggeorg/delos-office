package io.github.ggeorg.delos.base.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable database-project aggregate for Delos Base.
 */
public record DatabaseProject(String title, List<DatabaseObject> objects) {
    public DatabaseProject {
        title = normalizeTitle(title);
        objects = List.copyOf(Objects.requireNonNull(objects, "objects"));
        assertUniqueObjects(objects);
    }

    public static DatabaseProject blank() {
        return new DatabaseProject("Untitled", List.of());
    }

    public DatabaseProject withTitle(String title) {
        return new DatabaseProject(title, objects);
    }

    public DatabaseProject addObject(DatabaseObject object) {
        Objects.requireNonNull(object, "object");
        ArrayList<DatabaseObject> updated = new ArrayList<>(objects);
        updated.add(object);
        return new DatabaseProject(title, updated);
    }

    public List<DatabaseObject> objectsOfKind(DatabaseObjectKind kind) {
        Objects.requireNonNull(kind, "kind");
        return objects.stream()
                .filter(object -> object.kind() == kind)
                .toList();
    }

    public Optional<DatabaseObject> findObject(DatabaseObjectKind kind, String name) {
        String key = objectKey(kind, name);
        return objects.stream()
                .filter(object -> objectKey(object.kind(), object.name()).equals(key))
                .findFirst();
    }

    public int objectCount() {
        return objects.size();
    }

    private static void assertUniqueObjects(List<DatabaseObject> objects) {
        ArrayList<String> seen = new ArrayList<>();
        for (DatabaseObject object : objects) {
            Objects.requireNonNull(object, "object");
            String key = objectKey(object.kind(), object.name());
            if (seen.contains(key)) {
                throw new IllegalArgumentException("Duplicate database object: " + object.displayName());
            }
            seen.add(key);
        }
    }

    private static String objectKey(DatabaseObjectKind kind, String name) {
        return Objects.requireNonNull(kind, "kind").name()
                + ":"
                + Objects.requireNonNullElse(name, "").trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeTitle(String value) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        return normalized.isEmpty() ? "Untitled" : normalized;
    }
}
