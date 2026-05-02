package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.TextDecorationMetrics;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.render.TextLayoutResult;
import io.github.ggeorg.delos.writer.layout.TextMeasurer;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * PDFBox-backed text measurer for PDF export rendering decisions and headless
 * Writer layout.
 */
public final class PdfRenderTextMeasurer implements RenderTextMeasurer, TextMeasurer {
    private final PdfFontResolver fonts;

    public PdfRenderTextMeasurer(PdfFontResolver fonts) {
        this.fonts = Objects.requireNonNull(fonts, "fonts");
    }

    @Override
    public RenderFont styledFont(RenderFont baseFont, boolean bold, boolean italic) {
        RenderFont safeBase = fonts.resolve(Objects.requireNonNull(baseFont, "baseFont"));
        return fonts.resolve(new RenderFont(safeBase.family(), safeBase.size(), bold, italic));
    }

    @Override
    public double textWidth(String text, RenderFont font) {
        String safeText = PdfTextSanitizer.sanitize(text);
        if (safeText.isEmpty()) {
            return 0.0;
        }
        RenderFont resolvedFont = fonts.resolve(Objects.requireNonNull(font, "font"));
        try {
            PDFont pdfFont = fonts.fontFor(resolvedFont, safeText);
            return pdfFont.getStringWidth(safeText) / 1000.0 * resolvedFont.size();
        } catch (IOException ex) {
            throw new PdfRenderException(ex);
        }
    }

    @Override
    public double charWidth(char ch, RenderFont font) {
        return textWidth(String.valueOf(ch), font);
    }

    @Override
    public double lineHeight(RenderFont font) {
        return metricsFor(font, " ").lineHeight();
    }

    @Override
    public double baseline(RenderFont font) {
        return metricsFor(font, " ").baseline();
    }

    @Override
    public List<Double> caretStops(String text, RenderFont font) {
        String sourceText = text == null ? "" : text;
        List<Double> stops = new ArrayList<>(sourceText.length() + 1);
        stops.add(0.0);
        if (sourceText.isEmpty()) {
            return List.copyOf(stops);
        }

        double previous = 0.0;
        int index = 0;
        while (index < sourceText.length()) {
            int next = sourceText.offsetByCodePoints(index, 1);
            double nextWidth = textWidth(sourceText.substring(0, next), font);
            for (int i = index + 1; i < next; i++) {
                stops.add(previous);
            }
            stops.add(nextWidth);
            previous = nextWidth;
            index = next;
        }
        return List.copyOf(stops);
    }

    @Override
    public TextLayoutResult layoutText(String text, RenderFont font) {
        String sourceText = text == null ? "" : text;
        return new TextLayoutResult(
                sourceText,
                fonts.resolve(Objects.requireNonNull(font, "font")),
                textWidth(sourceText, font),
                lineHeight(font),
                baseline(font),
                caretStops(sourceText, font),
                decorationMetrics(font, sourceText)
        );
    }

    private TextDecorationMetrics decorationMetrics(RenderFont font, String text) {
        return metricsFor(font, text).decorations();
    }

    private PdfFontMetrics metricsFor(RenderFont font, String text) {
        RenderFont resolvedFont = fonts.resolve(Objects.requireNonNull(font, "font"));
        String safeText = PdfTextSanitizer.sanitize(text);
        String sample = safeText.isEmpty() ? " " : safeText;
        PDFont pdfFont = fonts.fontFor(resolvedFont, sample);
        return PdfFontMetrics.from(pdfFont, resolvedFont.size());
    }
}
