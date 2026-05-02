package io.github.ggeorg.delos.document;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Filesystem I/O for registered Delos document formats.
 */
public final class DocumentIo {
    private final DocumentRegistry registry;

    public DocumentIo(DocumentRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public DocumentRegistry registry() {
        return registry;
    }

    public DocumentPackage<?> read(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        DocumentFormat<?> format = registry.findByExtension(path)
                .orElseThrow(() -> new IOException("Unsupported Delos document extension: " + path.getFileName()));
        return read(path, format);
    }

    public <T> DocumentPackage<T> read(Path path, DocumentFormat<T> format) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(format, "format");
        try (InputStream inputStream = Files.newInputStream(path)) {
            return new DocumentPackage<>(path, format, format.read(inputStream));
        }
    }

    public <T> Path write(Path path, DocumentFormat<T> format, T document) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(document, "document");

        Path normalized = format.type().normalize(path);
        Path target = normalized.toAbsolutePath();
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path tempFile = createTempSibling(target);
        boolean moved = false;
        try {
            try (OutputStream outputStream = Files.newOutputStream(tempFile)) {
                format.write(document, outputStream);
                outputStream.flush();
            }
            moveIntoPlace(tempFile, target);
            moved = true;
            return normalized;
        } finally {
            if (!moved) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    private static Path createTempSibling(Path target) throws IOException {
        Path parent = target.getParent();
        String fileName = target.getFileName() == null ? "document" : target.getFileName().toString();
        if (parent == null) {
            return Files.createTempFile("." + fileName + "-", ".tmp");
        }
        return Files.createTempFile(parent, "." + fileName + "-", ".tmp");
    }

    private static void moveIntoPlace(Path tempFile, Path target) throws IOException {
        try {
            Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
