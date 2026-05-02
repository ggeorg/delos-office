package io.github.ggeorg.delos.document;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A loaded document together with its source path and format.
 */
public record DocumentPackage<T>(Path path, DocumentFormat<T> format, T content) {
    public DocumentPackage {
        path = Objects.requireNonNull(path, "path");
        format = Objects.requireNonNull(format, "format");
        content = Objects.requireNonNull(content, "content");
    }

    public DocumentType type() {
        return format.type();
    }
}
