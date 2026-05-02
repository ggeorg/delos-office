package io.github.ggeorg.delos.writer.ui;

import io.github.ggeorg.delos.writer.contract.JavaFxTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class StatusBarZoomTest extends JavaFxTestSupport {
    @Test
    void zoomFactorUpdatesLabelOnly() {
        StatusBar bar = onFxThread(StatusBar::new);

        onFxThread(() -> bar.setZoomFactor(1.25));

        assertEquals("Zoom: 125%", onFxThread(() -> bar.zoomLabel().getText()));
    }
}
