package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RecordingRenderTarget;
import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.render.RenderTheme;
import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.layout.BlockRole;
import io.github.ggeorg.delos.writer.layout.LaidOutLine;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.layout.LaidOutRun;
import io.github.ggeorg.delos.writer.layout.LaidOutTableBlock;
import io.github.ggeorg.delos.writer.layout.LaidOutTableCell;
import io.github.ggeorg.delos.writer.layout.LaidOutTableRow;
import io.github.ggeorg.delos.writer.layout.LaidOutTextBlock;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TableBlockRendererContractTest {
    private static final RenderTheme THEME = TestRenderThemes.defaultTheme();
    private static final FixedRenderTextMeasurer MEASURER = new FixedRenderTextMeasurer();

    @Test
    void rendersTableBordersAndCellText() {
        RecordingRenderTarget target = new RecordingRenderTarget();

        new DefaultPageRenderer().renderPage(
                target,
                PageRenderContext.pdfExport(tablePage(), THEME, MEASURER)
        );

        assertTrue(target.containsText("Cell"));
        assertTrue(target.count(RecordingRenderTarget.DrawKind.STROKE_RECT) >= 1);
    }

    private static LaidOutPage tablePage() {
        LaidOutTextBlock cellText = textBlock("Cell");
        LaidOutTableCell cell = new LaidOutTableCell(0.0, 0.0, 120.0, 32.0, List.of(cellText));
        LaidOutTableRow row = new LaidOutTableRow(0.0, 32.0, List.of(cell));
        LaidOutTableBlock table = new LaidOutTableBlock(72.0, 80.0, 120.0, 32.0, List.of(row));
        return new LaidOutPage(0, 500.0, 700.0, List.of(table));
    }

    private static LaidOutTextBlock textBlock(String text) {
        List<Double> stops = caretStops(text.length(), 7.0);
        LaidOutRun run = new LaidOutRun(text, 0, text.length(), 0.0, 28.0, CharacterStyle.PLAIN);
        LaidOutLine line = new LaidOutLine(
                text,
                0.0,
                0.0,
                28.0,
                16.0,
                12.0,
                0,
                text.length(),
                List.of(run),
                stops
        );
        return new LaidOutTextBlock(
                BlockRole.TABLE_CELL,
                5.0,
                5.0,
                110.0,
                16.0,
                -1,
                0,
                true,
                true,
                List.of(line)
        );
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
            return TableBlockRendererContractTest.caretStops(text == null ? 0 : text.length(), 7.0);
        }
    }
}
