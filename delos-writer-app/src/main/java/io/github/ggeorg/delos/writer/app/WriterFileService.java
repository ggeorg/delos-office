package io.github.ggeorg.delos.writer.app;

import io.github.ggeorg.delos.document.DocumentIo;
import io.github.ggeorg.delos.document.DocumentPackage;
import io.github.ggeorg.delos.document.DocumentRegistry;
import io.github.ggeorg.delos.writer.app.io.WriterFileChoosers;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.io.DocumentSerializer;
import io.github.ggeorg.delos.writer.io.WriterDocumentExtensions;
import io.github.ggeorg.delos.writer.io.WriterDocumentFormat;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * JavaFX file-chooser adapter for standalone Writer documents.
 *
 * <p>Native Writer files use {@code .dlw}. Legacy plain XML {@code .delos}
 * and {@code .dwrite} files can still be opened and are preserved when saving
 * without Save As. Save As always suggests the native {@code .dlw} extension.</p>
 */
final class WriterFileService {
    private static final FileChooser.ExtensionFilter WRITER_OPEN_FILTER =
            new FileChooser.ExtensionFilter(
                    "All Delos Writer Documents (*.dlw, *.delos, *.dwrite)",
                    "*.dlw", "*.delos", "*.dwrite"
            );
    private static final FileChooser.ExtensionFilter WRITER_NATIVE_FILTER =
            new FileChooser.ExtensionFilter("Delos Writer Document (*.dlw)", "*.dlw");
    private static final FileChooser.ExtensionFilter WRITER_LEGACY_FILTER =
            new FileChooser.ExtensionFilter("Legacy Delos Writer XML (*.delos, *.dwrite)", "*.delos", "*.dwrite");
    private static final FileChooser.ExtensionFilter ALL_FILES_FILTER =
            new FileChooser.ExtensionFilter("All Files (*.*)", "*.*");

    private final WriterDocumentFormat writerFormat;
    private final DocumentSerializer legacySerializer;
    private final DocumentIo documentIo;

    WriterFileService() {
        this(new WriterDocumentFormat(), new DocumentSerializer());
    }

    WriterFileService(WriterDocumentFormat writerFormat) {
        this(writerFormat, new DocumentSerializer());
    }

    WriterFileService(WriterDocumentFormat writerFormat, DocumentSerializer legacySerializer) {
        this.writerFormat = Objects.requireNonNull(writerFormat, "writerFormat");
        this.legacySerializer = Objects.requireNonNull(legacySerializer, "legacySerializer");
        this.documentIo = new DocumentIo(DocumentRegistry.of(writerFormat));
    }

    LoadedWriterDocument open(Window owner, Path initialFile) throws IOException {
        Path selected = chooseOpenPath(owner, initialFile);
        return selected == null ? null : read(selected);
    }

    LoadedWriterDocument read(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        if (isLegacyPlainXmlPath(path)) {
            try (InputStream inputStream = Files.newInputStream(path)) {
                return new LoadedWriterDocument(path, writerFormat.read(inputStream));
            }
        }
        DocumentPackage<?> loaded = documentIo.read(path);
        Object content = loaded.content();
        if (content instanceof Document document) {
            return new LoadedWriterDocument(loaded.path(), document);
        }
        throw new IOException("Unsupported Writer document content: " + content.getClass().getName());
    }

    Path save(Window owner, Path currentFile, Document document, boolean saveAs) throws IOException {
        Objects.requireNonNull(document, "document");
        if (!saveAs && currentFile != null && isLegacyPlainXmlPath(currentFile)) {
            return writeLegacyPlainXml(currentFile, document);
        }

        Path target = saveAs || currentFile == null
                ? chooseSavePath(owner, currentFile, document.title())
                : currentFile;
        return target == null ? null : documentIo.write(target, writerFormat, document);
    }

    private Path writeLegacyPlainXml(Path target, Document document) throws IOException {
        WriterFileChoosers.ensureParentDirectory(target);
        try (OutputStream outputStream = Files.newOutputStream(target)) {
            legacySerializer.write(document, outputStream);
        }
        return target;
    }

    private Path chooseOpenPath(Window owner, Path initialFile) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Writer Document");
        chooser.getExtensionFilters().addAll(WRITER_OPEN_FILTER, WRITER_NATIVE_FILTER, WRITER_LEGACY_FILTER, ALL_FILES_FILTER);
        WriterFileChoosers.configureInitialLocation(chooser, initialFile);
        var file = chooser.showOpenDialog(owner);
        return file == null ? null : file.toPath();
    }

    private Path chooseSavePath(Window owner, Path currentFile, String documentTitle) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Writer Document");
        chooser.getExtensionFilters().add(WRITER_NATIVE_FILTER);
        WriterFileChoosers.configureInitialLocation(chooser, currentFile);
        chooser.setInitialFileName(suggestedSaveFileName(currentFile, documentTitle));
        var file = chooser.showSaveDialog(owner);
        return file == null ? null : file.toPath();
    }

    static String suggestedSaveFileName(Path currentFile, String documentTitle) {
        return WriterFileChoosers.sanitizeFileName(
                WriterFileChoosers.suggestedBaseName(currentFile, documentTitle)
        ) + WriterDocumentExtensions.DOCUMENT;
    }

    static boolean isNativeWriterPath(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        return path.getFileName().toString()
                .toLowerCase(Locale.ROOT)
                .endsWith(WriterDocumentExtensions.DOCUMENT);
    }

    static boolean isLegacyPlainXmlPath(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return filename.endsWith(WriterDocumentExtensions.LEGACY_DELOS_XML)
                || filename.endsWith(WriterDocumentExtensions.LEGACY_DWRITE_XML);
    }

    record LoadedWriterDocument(Path path, Document document) { }
}
