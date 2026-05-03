package io.github.ggeorg.delos.writer.app.io;

import javafx.stage.FileChooser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Shared file chooser and export-path helpers for Delos Writer. */
public final class WriterFileChoosers {
    private WriterFileChoosers() {
    }

    public static void configureInitialLocation(FileChooser chooser, Path path) {
        if (chooser == null || path == null) {
            return;
        }
        Path absolute = path.toAbsolutePath();
        Path parent = Files.isDirectory(absolute) ? absolute : absolute.getParent();
        if (parent != null && Files.isDirectory(parent)) {
            chooser.setInitialDirectory(parent.toFile());
        }
    }

    public static void ensureParentDirectory(Path target) throws IOException {
        if (target == null) {
            return;
        }
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    public static Path normalizeExtension(Path path, String extension) {
        if (path == null) {
            return null;
        }
        String safeExtension = extension == null ? "" : extension;
        if (safeExtension.isBlank()) {
            return path;
        }
        String normalizedExtension = safeExtension.startsWith(".") ? safeExtension : "." + safeExtension;
        String filename = path.getFileName().toString();
        if (filename.toLowerCase(Locale.ROOT).endsWith(normalizedExtension.toLowerCase(Locale.ROOT))) {
            return path;
        }
        return path.resolveSibling(filename + normalizedExtension);
    }

    public static String stripExtensionForDisplay(String candidateName, String extension) {
        String safeCandidate = sanitizeFileName(candidateName);
        String safeExtension = extension == null ? "" : extension;
        if (safeExtension.isBlank()) {
            return safeCandidate;
        }
        String normalizedExtension = safeExtension.startsWith(".") ? safeExtension : "." + safeExtension;
        if (safeCandidate.toLowerCase(Locale.ROOT).endsWith(normalizedExtension.toLowerCase(Locale.ROOT))) {
            return safeCandidate.substring(0, safeCandidate.length() - normalizedExtension.length());
        }
        return safeCandidate;
    }

    public static String suggestedBaseName(Path currentFile, String documentTitle) {
        if (currentFile != null && currentFile.getFileName() != null) {
            String filename = currentFile.getFileName().toString();
            int extensionIndex = filename.lastIndexOf('.');
            if (extensionIndex > 0) {
                return filename.substring(0, extensionIndex);
            }
            return filename;
        }
        return documentTitle;
    }

    public static String sanitizeFileName(String value) {
        String candidate = value == null || value.isBlank() ? "Untitled" : value.trim();
        candidate = candidate.replaceAll("[\\\\/:*?\"<>|]", "-");
        return candidate.isBlank() ? "Untitled" : candidate;
    }
}
