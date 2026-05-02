package io.github.ggeorg.delos.writer.app.io;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.io.DocumentHtmlExporter;
import io.github.ggeorg.delos.writer.io.DocumentMarkdownExporter;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Export-only file chooser adapter for formats that are not Writer native packages.
 *
 * <p>Native Writer open/save belongs to {@code WriterFileService}. This class is
 * intentionally limited to HTML and Markdown export so the app has only one
 * native Writer save path: {@code .dlw}.</p>
 */
public final class DocumentFileService {
    private static final FileChooser.ExtensionFilter HTML_FILTER =
            new FileChooser.ExtensionFilter("HTML (*.html)", "*.html");
    private static final FileChooser.ExtensionFilter MARKDOWN_FILTER =
            new FileChooser.ExtensionFilter("Markdown (*.md)", "*.md");

    private final DocumentHtmlExporter htmlExporter;
    private final DocumentMarkdownExporter markdownExporter;

    public DocumentFileService() {
        this(new DocumentHtmlExporter(), new DocumentMarkdownExporter());
    }

    public DocumentFileService(DocumentHtmlExporter htmlExporter, DocumentMarkdownExporter markdownExporter) {
        this.htmlExporter = Objects.requireNonNull(htmlExporter, "htmlExporter");
        this.markdownExporter = Objects.requireNonNull(markdownExporter, "markdownExporter");
    }

    public Path exportHtml(Window owner, Path currentFile, Document document) throws IOException {
        Objects.requireNonNull(document, "document");
        Path target = chooseSavePath(owner, currentFile, document.title(), "Export HTML", HTML_FILTER, ".html");
        if (target == null) {
            return null;
        }
        Path normalizedTarget = normalizeExtension(target, ".html");
        Path parent = normalizedTarget.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream outputStream = Files.newOutputStream(normalizedTarget)) {
            htmlExporter.write(document, outputStream);
        }
        return normalizedTarget;
    }

    public Path exportMarkdown(Window owner, Path currentFile, Document document) throws IOException {
        Objects.requireNonNull(document, "document");
        Path target = chooseSavePath(owner, currentFile, document.title(), "Export Markdown", MARKDOWN_FILTER, ".md");
        if (target == null) {
            return null;
        }
        Path normalizedTarget = normalizeExtension(target, ".md");
        Path parent = normalizedTarget.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream outputStream = Files.newOutputStream(normalizedTarget)) {
            markdownExporter.write(document, outputStream);
        }
        return normalizedTarget;
    }

    private Path chooseSavePath(Window owner, Path currentFile, String documentTitle, String dialogTitle, FileChooser.ExtensionFilter filter, String extension) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(dialogTitle);
        chooser.getExtensionFilters().add(filter);
        configureInitialLocation(chooser, currentFile);
        String candidateName = sanitizeFileName(exportBaseName(currentFile, documentTitle));
        chooser.setInitialFileName(toDisplayFileName(candidateName, extension));
        var file = chooser.showSaveDialog(owner);
        return file == null ? null : file.toPath();
    }

    private static void configureInitialLocation(FileChooser chooser, Path path) {
        if (path == null) {
            return;
        }
        Path absolute = path.toAbsolutePath();
        Path parent = Files.isDirectory(absolute) ? absolute : absolute.getParent();
        if (parent != null && Files.isDirectory(parent)) {
            chooser.setInitialDirectory(parent.toFile());
        }
    }

    private static String toDisplayFileName(String candidateName, String extension) {
        if (candidateName.endsWith(extension)) {
            return candidateName.substring(0, candidateName.length() - extension.length());
        }
        return candidateName;
    }

    private static Path normalizeExtension(Path path, String extension) {
        String filename = path.getFileName().toString();
        if (filename.endsWith(extension)) {
            return path;
        }
        return path.resolveSibling(filename + extension);
    }

    private static String exportBaseName(Path currentFile, String documentTitle) {
        if (currentFile != null) {
            String filename = currentFile.getFileName().toString();
            int extensionIndex = filename.lastIndexOf('.');
            if (extensionIndex > 0) {
                return filename.substring(0, extensionIndex);
            }
            return filename;
        }
        return documentTitle;
    }

    private static String sanitizeFileName(String value) {
        String candidate = value == null || value.isBlank() ? "Untitled" : value.trim();
        candidate = candidate.replaceAll("[\\\\/:*?\"<>|]", "-");
        return candidate.isBlank() ? "Untitled" : candidate;
    }
}
