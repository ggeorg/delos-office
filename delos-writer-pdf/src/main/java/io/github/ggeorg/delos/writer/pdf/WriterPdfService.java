package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.layout.DocumentLayoutEngine;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;
import io.github.ggeorg.delos.writer.render.DefaultPageRenderer;
import io.github.ggeorg.delos.writer.render.DocumentMediaImageResolver;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Headless Writer PDF generation entrypoint.
 *
 * <p>This service starts from the Writer {@link Document} model, performs a
 * JavaFX-free layout pass using PDFBox-backed text metrics, and then exports the
 * resulting immutable layout through {@link PdfWriterExporter}. It is the server
 * safe path for Delos reporting and should not depend on {@code DelosEditor},
 * {@code PageView}, or any JavaFX class.</p>
 */
public final class WriterPdfService {
    private final PdfFontRegistry fonts;
    private final PdfWriterExporter exporter;
    private final PdfExportOptions options;
    private final HeadlessOutputLayoutPolicy outputPolicy;
    private final DocumentLayoutEngine layoutEngine;

    public WriterPdfService() {
        this(new PdfFontRegistry(), HeadlessOutputLayoutPolicy.defaultPolicy());
    }

    public WriterPdfService(PdfExportOptions options) {
        this(new PdfFontRegistry(), HeadlessOutputLayoutPolicy.fromExportOptions(options));
    }

    public WriterPdfService(PdfFontRegistry fonts, PdfExportOptions options) {
        this(fonts, HeadlessOutputLayoutPolicy.fromExportOptions(options));
    }

    public WriterPdfService(PdfFontRegistry fonts, HeadlessOutputLayoutPolicy outputPolicy) {
        this.fonts = Objects.requireNonNull(fonts, "fonts");
        this.outputPolicy = Objects.requireNonNull(outputPolicy, "outputPolicy");
        this.options = outputPolicy.exportOptions();
        this.exporter = new PdfWriterExporter(new DefaultPageRenderer(), fonts, this.options);
        this.layoutEngine = new PdfDocumentLayoutEngine(fonts, outputPolicy);
    }

    public LaidOutDocument layout(Document document) throws IOException {
        Objects.requireNonNull(document, "document");
        return layout(document, options.layoutTheme());
    }

    public LaidOutDocument layout(Document document, LayoutTheme theme) throws IOException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(theme, "theme");
        try {
            return layoutEngine.layout(document, theme);
        } catch (PdfRenderException ex) {
            throw ex.getCause();
        }
    }

    public void export(Document document, Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream output = Files.newOutputStream(path)) {
            export(document, output);
        }
    }

    public void exportLayout(Document document, LaidOutDocument layout, Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream output = Files.newOutputStream(path)) {
            exportLayout(document, layout, output);
        }
    }

    public void exportLayout(Document document, LaidOutDocument layout, OutputStream output) throws IOException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(output, "output");
        exporter.export(layout, output, DocumentMediaImageResolver.from(document));
    }

    public void export(Document document, OutputStream output) throws IOException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(output, "output");
        exporter.export(layout(document), output, DocumentMediaImageResolver.from(document));
    }
}
