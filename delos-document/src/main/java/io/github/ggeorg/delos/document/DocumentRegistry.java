package io.github.ggeorg.delos.document;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Small deterministic registry of native Delos document formats.
 */
public final class DocumentRegistry {
    private final List<DocumentFormat<?>> formats;

    public DocumentRegistry(List<? extends DocumentFormat<?>> formats) {
        Objects.requireNonNull(formats, "formats");
        if (formats.isEmpty()) {
            throw new IllegalArgumentException("At least one document format is required");
        }
        this.formats = List.copyOf(formats);
        assertUniqueTypes(this.formats);
    }

    public static DocumentRegistry of(DocumentFormat<?> first, DocumentFormat<?>... rest) {
        Objects.requireNonNull(first, "first");
        List<DocumentFormat<?>> all = new ArrayList<>();
        all.add(first);
        if (rest != null) {
            for (DocumentFormat<?> format : rest) {
                all.add(Objects.requireNonNull(format, "format"));
            }
        }
        return new DocumentRegistry(all);
    }

    public List<DocumentFormat<?>> formats() {
        return formats;
    }

    public Optional<DocumentFormat<?>> findByTypeId(String typeId) {
        String normalized = Objects.requireNonNullElse(typeId, "").trim();
        return formats.stream()
                .filter(format -> format.type().id().equals(normalized))
                .findFirst();
    }

    public Optional<DocumentFormat<?>> findByExtension(Path path) {
        Objects.requireNonNull(path, "path");
        return formats.stream()
                .filter(format -> format.type().matches(path))
                .findFirst();
    }

    private static void assertUniqueTypes(List<DocumentFormat<?>> formats) {
        List<String> ids = new ArrayList<>();
        List<String> extensions = new ArrayList<>();
        for (DocumentFormat<?> format : formats) {
            Objects.requireNonNull(format, "format");
            String id = format.type().id();
            String extension = format.type().extension().toLowerCase(java.util.Locale.ROOT);
            if (ids.contains(id)) {
                throw new IllegalArgumentException("Duplicate document type id: " + id);
            }
            if (extensions.contains(extension)) {
                throw new IllegalArgumentException("Duplicate document extension: " + extension);
            }
            ids.add(id);
            extensions.add(extension);
        }
    }
}
