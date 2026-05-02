package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.render.TextLayoutResult;
import io.github.ggeorg.delos.writer.layout.TextMeasurer;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.util.List;

/**
 * Long-lived PDF-compatible text measurer for the interactive Writer preview.
 *
 * <p>{@link PdfDocumentLayoutEngine} intentionally opens a short-lived PDFBox
 * document for each immutable layout pass. The JavaFX preview also needs a
 * render-time measurer for decorations, caret/protrusion helpers, and overlay
 * geometry. This class owns one measurement document for that live preview so
 * the visible editor does not silently fall back to JavaFX text metrics while
 * PDF export uses PDFBox metrics.</p>
 */
public final class PdfPreviewTextMeasurer implements RenderTextMeasurer, TextMeasurer, AutoCloseable {
    private final PDDocument measurementDocument;
    private final PdfRenderTextMeasurer delegate;
    private boolean closed;

    public PdfPreviewTextMeasurer() {
        this(new PdfFontRegistry());
    }

    public PdfPreviewTextMeasurer(PdfFontRegistry fonts) {
        this.measurementDocument = new PDDocument();
        this.delegate = new PdfRenderTextMeasurer(fonts.forDocument(measurementDocument));
    }

    @Override
    public RenderFont styledFont(RenderFont baseFont, boolean bold, boolean italic) {
        assertOpen();
        return delegate.styledFont(baseFont, bold, italic);
    }

    @Override
    public double textWidth(String text, RenderFont font) {
        assertOpen();
        return delegate.textWidth(text, font);
    }

    @Override
    public double charWidth(char ch, RenderFont font) {
        assertOpen();
        return delegate.charWidth(ch, font);
    }

    @Override
    public double lineHeight(RenderFont font) {
        assertOpen();
        return delegate.lineHeight(font);
    }

    @Override
    public double baseline(RenderFont font) {
        assertOpen();
        return delegate.baseline(font);
    }

    @Override
    public List<Double> caretStops(String text, RenderFont font) {
        assertOpen();
        return delegate.caretStops(text, font);
    }

    @Override
    public TextLayoutResult layoutText(String text, RenderFont font) {
        assertOpen();
        return delegate.layoutText(text, font);
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        measurementDocument.close();
    }

    private void assertOpen() {
        if (closed) {
            throw new IllegalStateException("PDF preview text measurer is closed");
        }
    }
}
