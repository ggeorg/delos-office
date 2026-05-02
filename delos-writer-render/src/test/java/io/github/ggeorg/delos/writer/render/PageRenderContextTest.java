package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.render.RenderTheme;
import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.layout.BlockRole;
import io.github.ggeorg.delos.writer.layout.CaretGeometry;
import io.github.ggeorg.delos.writer.layout.LaidOutLine;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.layout.LaidOutRun;
import io.github.ggeorg.delos.writer.layout.LaidOutTextBlock;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PageRenderContextTest {
    private static final RenderTheme THEME = TestRenderThemes.defaultTheme();
    private static final RenderTextMeasurer MEASURER = new FixedRenderTextMeasurer();

    @Test
    void editorContextDrawsChromeAndEditorOverlays() {
        PageRenderContext context = PageRenderContext.editor(
                samplePage(),
                THEME,
                MEASURER,
                stateWithOverlays()
        );

        assertTrue(context.drawPageChrome());
        assertTrue(context.drawSelection());
        assertTrue(context.drawCaret());
        assertTrue(context.drawsEditorOverlays());
    }

    @Test
    void printPreviewDrawsPageChromeButNoEditorOverlays() {
        PageRenderContext context = PageRenderContext.printPreview(samplePage(), THEME, MEASURER);

        assertTrue(context.drawPageChrome());
        assertFalse(context.drawSelection());
        assertFalse(context.drawCaret());
        assertFalse(context.drawsEditorOverlays());
    }

    @Test
    void pdfExportForcesFinalOutputPolicyEvenWhenCallerRequestsOverlays() {
        PageRenderContext context = new PageRenderContext(
                samplePage(),
                THEME,
                MEASURER,
                stateWithOverlays(),
                PageRenderDestination.PDF_EXPORT,
                1.0,
                true,
                true,
                true
        );

        assertFalse(context.drawPageChrome());
        assertFalse(context.drawSelection());
        assertFalse(context.drawCaret());
        assertFalse(context.drawsEditorOverlays());
    }

    private static PageRenderState stateWithOverlays() {
        return new PageRenderState(
                new CaretGeometry(72.0, 80.0, 16.0),
                new SelectionRange(new TextPosition(0, 1), new TextPosition(0, 5)),
                true
        );
    }

    private static LaidOutPage samplePage() {
        String text = "Hello Delos";
        List<Double> stops = caretStops(text.length(), 7.0);
        LaidOutRun run = new LaidOutRun(text, 0, text.length(), 0.0, 70.0, CharacterStyle.PLAIN);
        LaidOutLine line = new LaidOutLine(
                text,
                0.0,
                0.0,
                70.0,
                16.0,
                12.0,
                0,
                text.length(),
                List.of(run),
                stops
        );
        LaidOutTextBlock block = new LaidOutTextBlock(
                BlockRole.BODY,
                72.0,
                80.0,
                360.0,
                16.0,
                0,
                0,
                true,
                true,
                List.of(line)
        );
        return new LaidOutPage(0, 500.0, 700.0, List.of(block));
    }

    private static List<Double> caretStops(int length, double charWidth) {
        List<Double> stops = new ArrayList<>(length + 1);
        for (int i = 0; i <= length; i++) {
            stops.add(i * charWidth);
        }
        return stops;
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
            return PageRenderContextTest.caretStops(text == null ? 0 : text.length(), 7.0);
        }
    }
}
