package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RecordingRenderTarget;
import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.writer.layout.LaidOutFormulaBlock;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FormulaBlockRendererContractTest {
    @Test
    void rendererPaintsFormulaBadgePreviewSourceAndPlaceholderChrome() {
        RecordingRenderTarget target = new RecordingRenderTarget();
        LaidOutFormulaBlock formula = new LaidOutFormulaBlock(1, 72.0, 96.0, 360.0, 56.0, "latex", "E = mc^2", "mass energy");
        LaidOutPage page = new LaidOutPage(0, 500.0, 700.0, List.of(formula));

        new DefaultPageRenderer().renderPage(
                target,
                PageRenderContext.editor(page, TestRenderThemes.defaultTheme(), new FixedMeasurer(), PageRenderState.EMPTY)
        );

        assertTrue(target.containsText("ƒx"));
        assertTrue(target.containsText("E = mc²"));
        assertTrue(target.containsText("E = mc^2"));
        assertTrue(target.count(RecordingRenderTarget.DrawKind.STROKE_ROUND_RECT) >= 1);
        assertTrue(target.count(RecordingRenderTarget.DrawKind.STROKE_LINE) >= 1);
    }

    private static final class FixedMeasurer implements RenderTextMeasurer {
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
            return java.util.stream.IntStream.rangeClosed(0, text == null ? 0 : text.length())
                    .mapToDouble(index -> index * 7.0)
                    .boxed()
                    .toList();
        }
    }
}
