package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RecordingRenderTarget;
import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.RenderImage;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.render.RenderTheme;
import io.github.ggeorg.delos.writer.layout.LaidOutImageBlock;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ImageBlockRendererContractTest {
    private static final RenderTheme THEME = TestRenderThemes.defaultTheme();
    private static final RenderTextMeasurer MEASURER = new FixedRenderTextMeasurer();

    @Test
    void rendersResolvedImageAssetInsteadOfPlaceholder() {
        RecordingRenderTarget target = new RecordingRenderTarget();
        RenderImage image = new RenderImage("media/image-1.png", "image/png", new byte[] {1, 2, 3});

        new DefaultPageRenderer().renderPage(
                target,
                PageRenderContext.pdfExport(imagePage(), THEME, MEASURER)
                        .withImageResolver(source -> source.equals("media/image-1.png")
                                ? Optional.of(image)
                                : Optional.empty())
        );

        assertEquals(1, target.count(RecordingRenderTarget.DrawKind.DRAW_IMAGE));
        assertEquals(0, target.count(RecordingRenderTarget.DrawKind.STROKE_ROUND_RECT));
        assertEquals("media/image-1.png", target.callsOf(RecordingRenderTarget.DrawKind.DRAW_IMAGE).get(0).image().source());
    }

    @Test
    void fallsBackToPlaceholderWhenImageIsUnresolved() {
        RecordingRenderTarget target = new RecordingRenderTarget();

        new DefaultPageRenderer().renderPage(
                target,
                PageRenderContext.pdfExport(imagePage(), THEME, MEASURER)
        );

        assertEquals(0, target.count(RecordingRenderTarget.DrawKind.DRAW_IMAGE));
        assertEquals(1, target.count(RecordingRenderTarget.DrawKind.STROKE_ROUND_RECT));
    }

    private static LaidOutPage imagePage() {
        return new LaidOutPage(0, 500.0, 700.0, List.of(
                new LaidOutImageBlock(0, 72.0, 80.0, 160.0, 90.0, "media/image-1.png", "example")
        ));
    }

    private static final class FixedRenderTextMeasurer implements RenderTextMeasurer {
        @Override
        public RenderFont styledFont(RenderFont baseFont, boolean bold, boolean italic) {
            return new RenderFont(baseFont.family(), baseFont.size(), bold, italic);
        }

        @Override
        public double textWidth(String text, RenderFont font) {
            return text == null ? 0.0 : text.length() * 7.0;
        }

        @Override
        public double charWidth(char ch, RenderFont font) {
            return 7.0;
        }

        @Override
        public double lineHeight(RenderFont font) {
            return font.size() * 1.2;
        }

        @Override
        public double baseline(RenderFont font) {
            return font.size();
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
