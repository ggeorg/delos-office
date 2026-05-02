package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RecordingRenderTarget;
import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.render.RenderTheme;
import io.github.ggeorg.delos.render.TextDecorationMetrics;
import io.github.ggeorg.delos.render.TextLayoutResult;
import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.layout.BlockRole;
import io.github.ggeorg.delos.writer.layout.LaidOutLine;
import io.github.ggeorg.delos.writer.layout.LaidOutRun;
import io.github.ggeorg.delos.writer.layout.LaidOutTextBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TextBlockRendererDecorationContractTest {
    private static final RenderTheme THEME = TestRenderThemes.defaultTheme();
    private static final TextDecorationMetrics DECORATIONS = new TextDecorationMetrics(2.0, 5.0, 0.75);

    @Test
    void paintsUnderlineAndStrikethroughFromTextLayoutMetrics() {
        RecordingRenderTarget target = new RecordingRenderTarget();
        CharacterStyle style = CharacterStyle.PLAIN.withUnderline(true).withStrikethrough(true);
        LaidOutRun run = new LaidOutRun("Hello", 0, 5, 0.0, 999.0, style);
        LaidOutLine line = new LaidOutLine(
                "Hello",
                0.0,
                0.0,
                35.0,
                16.0,
                12.0,
                0,
                5,
                List.of(run),
                List.of(0.0, 7.0, 14.0, 21.0, 28.0, 35.0)
        );
        LaidOutTextBlock block = new LaidOutTextBlock(
                BlockRole.BODY,
                10.0,
                20.0,
                200.0,
                16.0,
                0,
                0,
                true,
                true,
                List.of(line)
        );

        new TextBlockRenderer().paint(target, block, THEME, new FixedRenderTextMeasurer(), 0.0, 0.0);

        List<RecordingRenderTarget.DrawCall> lines = target.callsOf(RecordingRenderTarget.DrawKind.STROKE_LINE);
        assertEquals(2, lines.size());

        RecordingRenderTarget.DrawCall underline = lines.get(0);
        assertEquals(10.0, underline.x(), 0.001);
        assertEquals(34.0, underline.y(), 0.001);
        assertEquals(45.0, underline.endX(), 0.001);
        assertEquals(34.0, underline.endY(), 0.001);

        RecordingRenderTarget.DrawCall strikethrough = lines.get(1);
        assertEquals(10.0, strikethrough.x(), 0.001);
        assertEquals(27.0, strikethrough.y(), 0.001);
        assertEquals(45.0, strikethrough.endX(), 0.001);
        assertEquals(27.0, strikethrough.endY(), 0.001);

        assertTrue(target.callsOf(RecordingRenderTarget.DrawKind.SET_LINE_WIDTH).stream()
                .anyMatch(call -> Math.abs(call.lineWidth() - 0.75) < 0.001));
    }

    private static final class FixedRenderTextMeasurer implements RenderTextMeasurer {
        @Override
        public RenderFont styledFont(RenderFont baseFont, boolean bold, boolean italic) {
            return new RenderFont(baseFont.family(), baseFont.size(), bold, italic);
        }

        @Override
        public double textWidth(String text, RenderFont font) {
            return (text == null ? 0 : text.length()) * 7.0;
        }

        @Override
        public double charWidth(char ch, RenderFont font) {
            return 7.0;
        }

        @Override
        public double lineHeight(RenderFont font) {
            return 16.0;
        }

        @Override
        public double baseline(RenderFont font) {
            return 12.0;
        }

        @Override
        public List<Double> caretStops(String text, RenderFont font) {
            int length = text == null ? 0 : text.length();
            return java.util.stream.IntStream.rangeClosed(0, length)
                    .mapToDouble(i -> i * 7.0)
                    .boxed()
                    .toList();
        }

        @Override
        public TextLayoutResult layoutText(String text, RenderFont font) {
            String safeText = text == null ? "" : text;
            return new TextLayoutResult(
                    safeText,
                    font,
                    textWidth(safeText, font),
                    lineHeight(font),
                    baseline(font),
                    caretStops(safeText, font),
                    DECORATIONS
            );
        }
    }
}
