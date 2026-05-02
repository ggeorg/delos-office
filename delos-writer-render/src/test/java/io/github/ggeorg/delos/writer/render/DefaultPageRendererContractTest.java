package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RecordingRenderTarget;
import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.render.RenderTheme;

import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.layout.BlockRole;
import io.github.ggeorg.delos.writer.layout.CaretGeometry;
import io.github.ggeorg.delos.writer.layout.LaidOutLine;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.layout.LaidOutRun;
import io.github.ggeorg.delos.writer.layout.LaidOutTextBlock;
import io.github.ggeorg.delos.writer.document.TextPosition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DefaultPageRendererContractTest {
    private static final RenderTheme THEME = TestRenderThemes.defaultTheme();
    private static final FixedRenderTextMeasurer MEASURER = new FixedRenderTextMeasurer();

    @Test
    void rendersBodyTextWithoutJavaFxCanvas() {
        RecordingRenderTarget target = new RecordingRenderTarget();

        new DefaultPageRenderer().renderPage(
                target,
                PageRenderContext.editor(samplePage(), THEME, MEASURER, PageRenderState.EMPTY)
        );

        assertTrue(target.containsText("Hello Delos"));
        assertTrue(target.count(RecordingRenderTarget.DrawKind.FILL_ROUND_RECT) >= 2);
        assertTrue(target.count(RecordingRenderTarget.DrawKind.STROKE_ROUND_RECT) >= 1);
    }

    @Test
    void rendersSelectionBeforeTextInEditorDestination() {
        RecordingRenderTarget target = new RecordingRenderTarget();
        SelectionRange selection = new SelectionRange(new TextPosition(0, 1), new TextPosition(0, 5));

        new DefaultPageRenderer().renderPage(
                target,
                PageRenderContext.editor(
                        samplePage(),
                        THEME,
                        MEASURER,
                        new PageRenderState(null, selection, false)
                )
        );

        assertTrue(target.callsOf(RecordingRenderTarget.DrawKind.FILL_RECT).stream()
                .anyMatch(call -> call.fill().equals(THEME.selectionFill()) && call.width() > 0.0));
        assertTrue(target.containsText("Hello Delos"));
    }

    @Test
    void rendersVisibleCaretInEditorDestination() {
        RecordingRenderTarget target = new RecordingRenderTarget();

        new DefaultPageRenderer().renderPage(
                target,
                PageRenderContext.editor(
                        samplePage(),
                        THEME,
                        MEASURER,
                        new PageRenderState(new CaretGeometry(72.0, 80.0, 16.0), null, true)
                )
        );

        assertTrue(target.callsOf(RecordingRenderTarget.DrawKind.SET_LINE_WIDTH).stream()
                .anyMatch(call -> Math.abs(call.lineWidth() - 1.25) < 0.001));
        assertTrue(target.count(RecordingRenderTarget.DrawKind.STROKE_LINE) >= 1);
    }

    @Test
    void recordsRenderStateSaveTranslateClipAndRestoreAroundPageContent() {
        RecordingRenderTarget target = new RecordingRenderTarget();

        new DefaultPageRenderer().renderPage(
                target,
                PageRenderContext.editor(samplePage(), THEME, MEASURER, PageRenderState.EMPTY)
        );

        assertEquals(RecordingRenderTarget.DrawKind.SAVE, target.calls().get(0).kind());
        assertEquals(RecordingRenderTarget.DrawKind.RESTORE, target.calls().get(target.calls().size() - 1).kind());
        assertEquals(1, target.count(RecordingRenderTarget.DrawKind.TRANSLATE));
        assertEquals(1, target.count(RecordingRenderTarget.DrawKind.CLIP));
        RecordingRenderTarget.DrawCall clip = target.callsOf(RecordingRenderTarget.DrawKind.CLIP).get(0);
        assertEquals(0.0, clip.x(), 0.001);
        assertEquals(0.0, clip.y(), 0.001);
        assertEquals(500.0, clip.width(), 0.001);
        assertEquals(700.0, clip.height(), 0.001);
    }

    @Test
    void pdfExportContextSkipsEditorChromeAndOverlays() {
        RecordingRenderTarget target = new RecordingRenderTarget();

        new DefaultPageRenderer().renderPage(
                target,
                new PageRenderContext(
                        samplePage(),
                        THEME,
                        MEASURER,
                        new PageRenderState(new CaretGeometry(72.0, 80.0, 16.0),
                                new SelectionRange(new TextPosition(0, 1), new TextPosition(0, 5)),
                                true),
                        PageRenderDestination.PDF_EXPORT,
                        1.0,
                        true,
                        true,
                        true
                )
        );

        assertEquals(0, target.count(RecordingRenderTarget.DrawKind.FILL_ROUND_RECT));
        assertEquals(0, target.callsOf(RecordingRenderTarget.DrawKind.FILL_RECT).stream()
                .filter(call -> call.fill().equals(THEME.selectionFill()))
                .count());
        assertTrue(target.callsOf(RecordingRenderTarget.DrawKind.SET_LINE_WIDTH).stream()
                .noneMatch(call -> Math.abs(call.lineWidth() - 1.25) < 0.001));
        assertTrue(target.containsText("Hello Delos"));
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
            return DefaultPageRendererContractTest.caretStops(text == null ? 0 : text.length(), 7.0);
        }
    }
}
