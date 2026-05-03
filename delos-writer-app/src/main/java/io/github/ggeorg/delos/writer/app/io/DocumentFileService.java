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
        Path normalizedTarget = WriterFileChoosers.normalizeExtension(target, ".html");
        WriterFileChoosers.ensureParentDirectory(normalizedTarget);
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
        Path normalizedTarget = WriterFileChoosers.normalizeExtension(target, ".md");
        WriterFileChoosers.ensureParentDirectory(normalizedTarget);
        try (OutputStream outputStream = Files.newOutputStream(normalizedTarget)) {
            markdownExporter.write(document, outputStream);
        }
        return normalizedTarget;
    }

    private Path chooseSavePath(Window owner, Path currentFile, String documentTitle, String dialogTitle, FileChooser.ExtensionFilter filter, String extension) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(dialogTitle);
        chooser.getExtensionFilters().add(filter);
        WriterFileChoosers.configureInitialLocation(chooser, currentFile);
        String candidateName = WriterFileChoosers.sanitizeFileName(
                WriterFileChoosers.suggestedBaseName(currentFile, documentTitle)
        );
        chooser.setInitialFileName(WriterFileChoosers.stripExtensionForDisplay(candidateName, extension));
        var file = chooser.showSaveDialog(owner);
        return file == null ? null : file.toPath();
    }

}
