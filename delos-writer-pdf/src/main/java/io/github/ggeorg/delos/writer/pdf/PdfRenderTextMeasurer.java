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
        PDFont pdfFont = fonts.fontFor(resolvedFont, safeText);
        return textWidthWithFont(pdfFont, safeText, resolvedFont);
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

        RenderFont resolvedFont = fonts.resolve(Objects.requireNonNull(font, "font"));
        String safeText = PdfTextSanitizer.sanitize(sourceText);
        PDFont pdfFont = safeText.isEmpty() ? null : fonts.fontFor(resolvedFont, safeText);

        double runningWidth = 0.0;
        int index = 0;
        while (index < sourceText.length()) {
            int codePoint = sourceText.codePointAt(index);
            int next = index + Character.charCount(codePoint);
            double previousWidth = runningWidth;
            String safeSegment = PdfTextSanitizer.sanitize(new String(Character.toChars(codePoint)));
            if (!safeSegment.isEmpty()) {
                runningWidth += textWidthWithFont(pdfFont, safeSegment, resolvedFont);
            }
            for (int i = index + 1; i < next; i++) {
                stops.add(previousWidth);
            }
            stops.add(runningWidth);
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

    private static double textWidthWithFont(PDFont pdfFont, String safeText, RenderFont font) {
        if (safeText == null || safeText.isEmpty()) {
            return 0.0;
        }
        try {
            return pdfFont.getStringWidth(safeText) / 1000.0 * font.size();
        } catch (IOException ex) {
            throw new PdfRenderException(ex);
        }
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
