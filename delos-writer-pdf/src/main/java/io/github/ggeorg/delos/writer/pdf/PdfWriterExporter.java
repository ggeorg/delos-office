package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.render.RenderImageResolver;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.render.DefaultPageRenderer;
import io.github.ggeorg.delos.writer.render.PageRenderContext;
import io.github.ggeorg.delos.writer.render.PageRenderer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Concrete Writer PDF exporter.
 *
 * <p>H33 intentionally avoids a generic output framework. The exporter consumes
 * the immutable Writer layout and renders every page through the same
 * PageRenderer used by the live editor, with {@code PDF_EXPORT} destination
 * policy suppressing editor-only chrome, selection, and caret overlays.</p>
 */
public final class PdfWriterExporter {
    private final PageRenderer renderer;
    private final PdfFontRegistry fonts;
    private final PdfExportOptions options;

    public PdfWriterExporter() {
        this(new DefaultPageRenderer(), new PdfFontRegistry(), PdfExportOptions.defaultOptions());
    }

    public PdfWriterExporter(PageRenderer renderer, PdfFontRegistry fonts, PdfExportOptions options) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.fonts = Objects.requireNonNull(fonts, "fonts");
        this.options = Objects.requireNonNull(options, "options");
    }

    public void export(LaidOutDocument document, Path path) throws IOException {
        export(document, path, RenderImageResolver.empty());
    }

    public void export(LaidOutDocument document, Path path, RenderImageResolver imageResolver) throws IOException {
        Objects.requireNonNull(path, "path");
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream output = Files.newOutputStream(path)) {
            export(document, output, imageResolver);
        }
    }

    public void export(LaidOutDocument document, OutputStream output) throws IOException {
        export(document, output, RenderImageResolver.empty());
    }

    public void export(LaidOutDocument document, OutputStream output, RenderImageResolver imageResolver) throws IOException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(output, "output");
        RenderImageResolver safeImageResolver = imageResolver == null ? RenderImageResolver.empty() : imageResolver;

        try (PDDocument pdf = new PDDocument()) {
            PdfFontRegistry documentFonts = fonts.forDocument(pdf);
            PdfRenderTextMeasurer measurer = new PdfRenderTextMeasurer(documentFonts);
            for (LaidOutPage laidOutPage : document.pages()) {
                renderPage(pdf, laidOutPage, documentFonts, measurer, safeImageResolver);
            }
            pdf.save(output);
        } catch (PdfRenderException ex) {
            throw ex.getCause();
        }
    }

    private void renderPage(PDDocument pdf,
                            LaidOutPage laidOutPage,
                            PdfFontRegistry pageFonts,
                            PdfRenderTextMeasurer measurer,
                            RenderImageResolver imageResolver) throws IOException {
        PDPage pdfPage = new PDPage(new PDRectangle((float) laidOutPage.width(), (float) laidOutPage.height()));
        pdf.addPage(pdfPage);
        try (PdfRenderTarget target = new PdfRenderTarget(
                pdf,
                new PDPageContentStream(pdf, pdfPage),
                laidOutPage.height(),
                pageFonts
        )) {
            renderer.renderPage(target, PageRenderContext.pdfExport(
                    laidOutPage,
                    options.renderTheme(),
                    measurer
            ).withImageResolver(imageResolver));
        }
    }
}
