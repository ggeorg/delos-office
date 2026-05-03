package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.render.TextDecorationMetrics;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;

import java.util.Objects;

/**
 * Converts PDFBox font descriptor data into Delos line/decorator metrics.
 *
 * <p>This class is deliberately small and boring: PDF layout and PDF drawing
 * should use the same PDF font metrics instead of each caller inventing naked
 * {@code fontSize * ...} constants.</p>
 */
record PdfFontMetrics(
        double lineHeight,
        double baseline,
        TextDecorationMetrics decorations
) {
    private static final double PDF_UNITS_PER_EM = 1000.0;
    private static final double FALLBACK_ASCENT_UNITS = 800.0;
    private static final double FALLBACK_DESCENT_UNITS = 200.0;

    /**
     * Preserve Delos' early PDF readability while scaling actual PDF font
     * ascender/descender boxes, not the naked point size.
     */
    private static final double DESCRIPTOR_LINE_BOX_MULTIPLIER = 1.20;

    static PdfFontMetrics from(PDFont font, double fontSize) {
        Objects.requireNonNull(font, "font");
        if (!Double.isFinite(fontSize) || fontSize <= 0.0) {
            throw new IllegalArgumentException("fontSize must be a positive finite value");
        }

        PDFontDescriptor descriptor = font.getFontDescriptor();
        double ascentUnits = ascentUnits(descriptor);
        double descentUnits = descentUnits(descriptor);
        double capHeightUnits = capHeightUnits(descriptor, ascentUnits);

        double naturalLineHeight = toPoints(ascentUnits + descentUnits, fontSize);
        double lineHeight = Math.max(0.1, naturalLineHeight * DESCRIPTOR_LINE_BOX_MULTIPLIER);
        double extraLeading = Math.max(0.0, lineHeight - naturalLineHeight);
        double baseline = extraLeading / 2.0 + toPoints(ascentUnits, fontSize);
        baseline = clampBaseline(baseline, lineHeight, fontSize);

        double descent = toPoints(descentUnits, fontSize);
        double capHeight = toPoints(capHeightUnits, fontSize);
        return new PdfFontMetrics(
                lineHeight,
                baseline,
                new TextDecorationMetrics(
                        Math.max(0.5, descent * 0.45),
                        Math.max(fontSize * 0.20, capHeight * 0.45),
                        Math.max(0.5, fontSize / 18.0)
                )
        );
    }

    private static double ascentUnits(PDFontDescriptor descriptor) {
        if (descriptor == null) {
            return FALLBACK_ASCENT_UNITS;
        }
        return positiveMetric(descriptor.getAscent(), FALLBACK_ASCENT_UNITS);
    }

    private static double descentUnits(PDFontDescriptor descriptor) {
        if (descriptor == null) {
            return FALLBACK_DESCENT_UNITS;
        }
        double descent = descriptor.getDescent();
        if (!Double.isFinite(descent) || descent == 0.0) {
            return FALLBACK_DESCENT_UNITS;
        }
        return Math.abs(descent);
    }

    private static double capHeightUnits(PDFontDescriptor descriptor, double ascentUnits) {
        if (descriptor == null) {
            return ascentUnits * 0.70;
        }
        return positiveMetric(descriptor.getCapHeight(), ascentUnits * 0.70);
    }

    private static double positiveMetric(double preferred, double fallback) {
        return Double.isFinite(preferred) && preferred > 0.0
                ? preferred
                : Math.max(0.0, fallback);
    }

    private static double toPoints(double pdfUnits, double fontSize) {
        return pdfUnits / PDF_UNITS_PER_EM * fontSize;
    }

    private static double clampBaseline(double baseline, double lineHeight, double fontSize) {
        if (!Double.isFinite(baseline) || baseline <= 0.0) {
            return Math.min(lineHeight, fontSize);
        }
        if (baseline >= lineHeight) {
            return Math.max(0.0, lineHeight - Math.max(0.5, fontSize * 0.05));
        }
        return baseline;
    }
}
