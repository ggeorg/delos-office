package io.github.ggeorg.delos.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class RecordingRenderTargetVectorTest {
    @Test
    void recordsStrokeRectAndPaths() {
        RecordingRenderTarget target = new RecordingRenderTarget();
        RenderPath path = RenderPath.builder()
                .moveTo(0.0, 0.0)
                .lineTo(10.0, 0.0)
                .lineTo(10.0, 10.0)
                .close()
                .build();

        target.strokeRect(1.0, 2.0, 3.0, 4.0);
        target.fillPath(path);
        target.strokePath(path);

        assertEquals(1, target.count(RecordingRenderTarget.DrawKind.STROKE_RECT));
        assertEquals(1, target.count(RecordingRenderTarget.DrawKind.FILL_PATH));
        assertEquals(1, target.count(RecordingRenderTarget.DrawKind.STROKE_PATH));
        assertSame(path, target.callsOf(RecordingRenderTarget.DrawKind.FILL_PATH).get(0).path());
    }
}
