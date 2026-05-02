package io.github.ggeorg.delos.writer.ui.ruler;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class RulerControlsContractTest {
    @Test
    void horizontalRulerTracksViewportScrollZoomAndPageMargins() throws IOException {
        String source = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/ui/ruler/HorizontalRuler.java"));

        assertTrue(source.contains("public final class HorizontalRuler"));
        assertTrue(source.contains("visibleContentXProperty()"));
        assertTrue(source.contains("viewportWidthProperty()"));
        assertTrue(source.contains("public void setPageStyle(PageStyle pageStyle)"));
        assertTrue(source.contains("pageWidth.set"));
        assertTrue(source.contains("marginLeft.set"));
        assertTrue(source.contains("marginRight.set"));
        assertTrue(source.contains("RulerMetrics.horizontalPageLeft"));
        assertTrue(source.contains("drawMargins"));
    }

    @Test
    void verticalRulerTracksViewportScrollZoomAndPageMargins() throws IOException {
        String source = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/ui/ruler/VerticalRuler.java"));

        assertTrue(source.contains("public final class VerticalRuler"));
        assertTrue(source.contains("visibleContentYProperty()"));
        assertTrue(source.contains("viewportHeightProperty()"));
        assertTrue(source.contains("public void setPageStyle(PageStyle pageStyle)"));
        assertTrue(source.contains("pageHeight.set"));
        assertTrue(source.contains("marginTop.set"));
        assertTrue(source.contains("marginBottom.set"));
        assertTrue(source.contains("RulerMetrics.visiblePageRange"));
        assertTrue(source.contains("RulerMetrics.pageTopInViewport"));
        assertTrue(source.contains("drawMargins"));
    }
}
