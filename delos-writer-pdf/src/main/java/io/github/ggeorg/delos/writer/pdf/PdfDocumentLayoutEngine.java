package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.layout.DocumentLayoutEngine;
import io.github.ggeorg.delos.writer.layout.KnuthPlassParagraphLayouter;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;
import io.github.ggeorg.delos.writer.layout.PaginatingDocumentLayoutEngine;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.util.Objects;

/**
 * Headless PDF-compatible Writer layout engine.
 *
 * <p>This engine is deliberately layout-only: it does not create PDF pages and
 * it does not draw anything. It uses PDFBox-backed text metrics so desktop
 * preview, print, export, and future report-server output can agree on line
 * breaks, page breaks, and block positions.</p>
 */
public final class PdfDocumentLayoutEngine implements DocumentLayoutEngine {
    private final PdfFontRegistry fonts;
    private final HeadlessOutputLayoutPolicy outputPolicy;

    public PdfDocumentLayoutEngine() {
        this(new PdfFontRegistry(), HeadlessOutputLayoutPolicy.defaultPolicy());
    }

    public PdfDocumentLayoutEngine(PdfFontRegistry fonts) {
        this(fonts, HeadlessOutputLayoutPolicy.defaultPolicy());
    }

    public PdfDocumentLayoutEngine(PdfFontRegistry fonts, HeadlessOutputLayoutPolicy outputPolicy) {
        this.fonts = Objects.requireNonNull(fonts, "fonts");
        this.outputPolicy = Objects.requireNonNull(outputPolicy, "outputPolicy");
    }

    @Override
    public LaidOutDocument layout(Document document, LayoutTheme theme) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(theme, "theme");
        try (PDDocument measurementDocument = new PDDocument()) {
            PdfFontRegistry measurementFonts = fonts.forDocument(measurementDocument);
            DocumentLayoutEngine layoutEngine = new PaginatingDocumentLayoutEngine(
                    new KnuthPlassParagraphLayouter(new PdfRenderTextMeasurer(measurementFonts))
            );
            return layoutEngine.layout(document, outputPolicy.layoutThemeFor(theme));
        } catch (IOException ex) {
            throw new PdfRenderException(ex);
        }
    }
}
