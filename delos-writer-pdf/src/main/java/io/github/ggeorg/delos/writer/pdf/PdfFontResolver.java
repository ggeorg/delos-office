package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.render.RenderFont;
import org.apache.pdfbox.pdmodel.font.PDFont;

/**
 * Resolves Delos render fonts to the concrete PDF fonts used for measurement
 * and output.
 *
 * <p>The same resolver must be used by PDF layout measurement and PDF drawing;
 * otherwise exported pages can wrap or paginate differently from the measured
 * layout.</p>
 */
public interface PdfFontResolver {
    /**
     * Returns the canonical PDF-facing font descriptor for the supplied Delos
     * font. The returned descriptor keeps size and style, but maps generic or
     * application families such as {@code System} and {@code Serif} to concrete
     * PDF families such as {@code Helvetica} or {@code Times}.
     */
    RenderFont resolve(RenderFont font);

    /**
     * Returns the ordinary PDF font for the supplied font descriptor.
     */
    PDFont fontFor(RenderFont font);

    /**
     * Returns a PDF font that can encode {@code text}, using a Unicode fallback
     * font when needed and available.
     */
    PDFont fontFor(RenderFont font, String text);
}
