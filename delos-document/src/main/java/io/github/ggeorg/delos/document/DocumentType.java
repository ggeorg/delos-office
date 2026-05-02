package io.github.ggeorg.delos.document;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Stable metadata for a Delos document family.
 */
public record DocumentType(String id, String displayName, String extension, String mediaType) {
    public DocumentType {
        id = requireNonBlank(id, "id");
        displayName = requireNonBlank(displayName, "displayName");
        extension = normalizeExtension(extension);
        mediaType = requireNonBlank(mediaType, "mediaType");
    }

    public String wildcard() {
        return "*" + extension;
    }

    public boolean matches(Path path) {
        Objects.requireNonNull(path, "path");
        return path.getFileName().toString()
                .toLowerCase(Locale.ROOT)
                .endsWith(extension.toLowerCase(Locale.ROOT));
    }

    public Path normalize(Path path) {
        Objects.requireNonNull(path, "path");
        if (matches(path)) {
            return path;
        }
        String filename = path.getFileName().toString();
        return path.resolveSibling(filename + extension);
    }

    private static String normalizeExtension(String value) {
        String normalized = requireNonBlank(value, "extension");
        return normalized.startsWith(".") ? normalized : "." + normalized;
    }

    private static String requireNonBlank(String value, String name) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
