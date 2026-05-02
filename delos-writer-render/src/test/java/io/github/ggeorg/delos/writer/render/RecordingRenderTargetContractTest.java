package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RecordingRenderTarget;
import io.github.ggeorg.delos.render.RenderColor;
import io.github.ggeorg.delos.render.RenderFont;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.ggeorg.delos.render.RecordingRenderTarget.DrawKind.CLIP;
import static io.github.ggeorg.delos.render.RecordingRenderTarget.DrawKind.FILL_RECT;
import static io.github.ggeorg.delos.render.RecordingRenderTarget.DrawKind.FILL_TEXT;
import static io.github.ggeorg.delos.render.RecordingRenderTarget.DrawKind.RESTORE;
import static io.github.ggeorg.delos.render.RecordingRenderTarget.DrawKind.SAVE;
import static io.github.ggeorg.delos.render.RecordingRenderTarget.DrawKind.SET_FILL;
import static io.github.ggeorg.delos.render.RecordingRenderTarget.DrawKind.SET_FONT;
import static io.github.ggeorg.delos.render.RecordingRenderTarget.DrawKind.SET_GLOBAL_ALPHA;
import static io.github.ggeorg.delos.render.RecordingRenderTarget.DrawKind.SET_LINE_WIDTH;
import static io.github.ggeorg.delos.render.RecordingRenderTarget.DrawKind.SET_STROKE;
import static io.github.ggeorg.delos.render.RecordingRenderTarget.DrawKind.STROKE_LINE;
import static io.github.ggeorg.delos.render.RecordingRenderTarget.DrawKind.TRANSLATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RecordingRenderTargetContractTest {

    @Test
    void recordsDrawingCallsInOrderWithCurrentState() {
        RecordingRenderTarget target = new RecordingRenderTarget();
        RenderColor fill = RenderColor.rgb(10, 20, 30);
        RenderColor stroke = RenderColor.rgb(40, 50, 60);
        RenderFont font = new RenderFont("Serif", 18.0, true, false);

        target.save();
        target.translate(5.0, 6.0);
        target.clip(1.0, 2.0, 3.0, 4.0);
        target.setFill(fill);
        target.setStroke(stroke);
        target.setLineWidth(2.5);
        target.setFont(font);
        target.setGlobalAlpha(0.75);
        target.fillRect(10.0, 20.0, 30.0, 40.0);
        target.strokeLine(1.0, 2.0, 3.0, 4.0);
        target.fillText("Hello", 7.0, 8.0);
        target.restore();

        List<RecordingRenderTarget.DrawCall> calls = target.calls();
        assertEquals(List.of(SAVE, TRANSLATE, CLIP, SET_FILL, SET_STROKE, SET_LINE_WIDTH, SET_FONT, SET_GLOBAL_ALPHA, FILL_RECT, STROKE_LINE, FILL_TEXT, RESTORE),
                calls.stream().map(RecordingRenderTarget.DrawCall::kind).toList());

        RecordingRenderTarget.DrawCall text = calls.get(10);
        assertEquals("Hello", text.text());
        assertEquals(7.0, text.x());
        assertEquals(8.0, text.y());
        assertEquals(fill, text.fill());
        assertEquals(stroke, text.stroke());
        assertEquals(font, text.font());
        assertEquals(2.5, text.lineWidth());
        assertEquals(0.75, text.globalAlpha());
        assertTrue(target.containsTextAt("Hello", 7.0, 8.0, 0.001));
    }

    @Test
    void restoreReinstatesSavedStateForFollowingCalls() {
        RecordingRenderTarget target = new RecordingRenderTarget();
        RenderColor original = RenderColor.rgb(1, 2, 3);
        RenderColor changed = RenderColor.rgb(200, 210, 220);

        target.setFill(original);
        target.save();
        target.setFill(changed);
        target.restore();
        target.fillText("After restore", 0.0, 0.0);

        RecordingRenderTarget.DrawCall restoredText = target.callsOf(FILL_TEXT).get(0);
        assertEquals(original, restoredText.fill());
    }
}
