package io.github.ggeorg.delos.writer.render.fx;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.TextDecorationMetrics;
import io.github.ggeorg.delos.render.TextLayoutResult;
import io.github.ggeorg.delos.writer.layout.TextMeasurer;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JavaFX-backed {@link TextMeasurer} implementation.
 *
 * <p>This is intentionally an adapter. The layout package depends on the
 * {@code TextMeasurer} interface only; JavaFX Toolkit measurement is isolated
 * here so the layout engine can later be moved into a JavaFX-free module.</p>
 */
public class JavaFxTextMeasurer implements TextMeasurer {
    private final Text helper = new Text();
    private final Map<StyledFontKey, RenderFont> styledFontCache = new HashMap<>();
    private final Map<FontDescriptor, FontMetrics> fontMetricsCache = new HashMap<>();
    private final Map<FontDescriptor, Map<Character, Double>> charWidthCache = new HashMap<>();
    private final Map<TextWidthKey, Double> textWidthCache = new HashMap<>();
    private final Map<CaretStopsKey, List<Double>> caretStopsCache = new HashMap<>();
    private final Map<TextWidthKey, TextLayoutResult> layoutCache = new HashMap<>();

    @Override
    public RenderFont styledFont(RenderFont baseFont, boolean bold, boolean italic) {
        StyledFontKey key = StyledFontKey.of(baseFont, bold, italic);
        return styledFontCache.computeIfAbsent(key, unused -> new RenderFont(key.family(), key.size(), bold, italic));
    }

    @Override
    public double textWidth(String text, RenderFont font) {
        String safeText = text == null ? "" : text;
        if (safeText.isEmpty()) {
            return 0;
        }
        if (safeText.length() == 1) {
            return charWidth(safeText.charAt(0), font);
        }

        FontDescriptor descriptor = FontDescriptor.from(font);
        TextWidthKey key = new TextWidthKey(safeText, descriptor);
        return textWidthCache.computeIfAbsent(key, unused -> measureTextWidth(safeText, font));
    }

    @Override
    public double charWidth(char ch, RenderFont font) {
        FontDescriptor descriptor = FontDescriptor.from(font);
        Map<Character, Double> perFont = charWidthCache.computeIfAbsent(descriptor, unused -> new HashMap<>());
        return perFont.computeIfAbsent(ch, key -> measureSingleChar(key, font));
    }

    @Override
    public double lineHeight(RenderFont font) {
        return metrics(font).lineHeight();
    }

    @Override
    public double baseline(RenderFont font) {
        return metrics(font).baseline();
    }

    @Override
    public List<Double> caretStops(String text, RenderFont font) {
        String safeText = text == null ? "" : text;
        FontDescriptor descriptor = FontDescriptor.from(font);
        CaretStopsKey key = new CaretStopsKey(safeText, descriptor);
        return caretStopsCache.computeIfAbsent(key, unused -> computeCaretStops(safeText, font));
    }

    @Override
    public TextLayoutResult layoutText(String text, RenderFont font) {
        String safeText = text == null ? "" : text;
        FontDescriptor descriptor = FontDescriptor.from(font);
        TextWidthKey key = new TextWidthKey(safeText, descriptor);
        return layoutCache.computeIfAbsent(key, unused -> new TextLayoutResult(
                safeText,
                font,
                textWidth(safeText, font),
                lineHeight(font),
                baseline(font),
                caretStops(safeText, font),
                metrics(font).decorations()
        ));
    }

    protected Font toFxFont(RenderFont font) {
        Objects.requireNonNull(font, "font");
        return Font.font(
                font.family(),
                font.bold() ? FontWeight.BOLD : FontWeight.NORMAL,
                font.italic() ? FontPosture.ITALIC : FontPosture.REGULAR,
                font.size()
        );
    }

    private double measureTextWidth(String text, RenderFont font) {
        helper.setFont(toFxFont(font));
        helper.setText(text);
        return helper.getLayoutBounds().getWidth();
    }

    private double measureSingleChar(char ch, RenderFont font) {
        helper.setFont(toFxFont(font));
        helper.setText(String.valueOf(ch));
        return helper.getLayoutBounds().getWidth();
    }

    private List<Double> computeCaretStops(String text, RenderFont font) {
        List<Double> stops = new ArrayList<>(text.length() + 1);
        stops.add(0.0);
        if (text.isEmpty()) {
            return List.copyOf(stops);
        }

        double previous = 0.0;
        int index = 0;
        while (index < text.length()) {
            int next = text.offsetByCodePoints(index, 1);
            double nextWidth = textWidth(text.substring(0, next), font);
            for (int i = index + 1; i < next; i++) {
                stops.add(previous);
            }
            stops.add(nextWidth);
            previous = nextWidth;
            index = next;
        }
        return List.copyOf(stops);
    }

    private FontMetrics metrics(RenderFont font) {
        FontDescriptor descriptor = FontDescriptor.from(font);
        return fontMetricsCache.computeIfAbsent(descriptor, unused -> {
            helper.setFont(toFxFont(font));
            helper.setText("Ay");
            double lineHeight = helper.getLayoutBounds().getHeight();
            double baseline = -helper.getLayoutBounds().getMinY();
            double size = font.size();
            return new FontMetrics(
                    lineHeight,
                    baseline,
                    new TextDecorationMetrics(
                            Math.max(0.5, size * 0.08),
                            Math.max(0.5, baseline * 0.38),
                            Math.max(0.5, size / 18.0)
                    )
            );
        });
    }

    private record StyledFontKey(String family, double size, boolean bold, boolean italic) {
        static StyledFontKey of(RenderFont font, boolean bold, boolean italic) {
            Objects.requireNonNull(font, "font");
            return new StyledFontKey(font.family(), font.size(), bold, italic);
        }
    }

    private record FontDescriptor(String family, double size, boolean bold, boolean italic) {
        static FontDescriptor from(RenderFont font) {
            Objects.requireNonNull(font, "font");
            return new FontDescriptor(font.family(), font.size(), font.bold(), font.italic());
        }
    }

    private record TextWidthKey(String text, FontDescriptor font) {
    }

    private record CaretStopsKey(String text, FontDescriptor font) {
    }

    private record FontMetrics(double lineHeight, double baseline, TextDecorationMetrics decorations) {
    }
}
