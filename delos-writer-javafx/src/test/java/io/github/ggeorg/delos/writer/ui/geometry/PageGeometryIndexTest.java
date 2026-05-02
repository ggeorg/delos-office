package io.github.ggeorg.delos.writer.ui.geometry;

import io.github.ggeorg.delos.writer.layout.CaretGeometry;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.ui.ViewTheme;
import javafx.geometry.Bounds;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageGeometryIndexTest {
    private static final ViewTheme THEME = ViewTheme.defaultTheme();

    @Test
    void computesContentDimensionsFromLayoutAndViewSpacing() {
        PageGeometryIndex index = PageGeometryIndex.from(sampleLayout(), THEME, 40.0);

        assertEquals(2, index.pageCount());
        assertEquals(
                500.0 + THEME.shadowExtentX() * 2.0,
                index.contentWidth(),
                0.001
        );
        assertEquals(
                THEME.outerPadding() * 2.0
                        + (700.0 + THEME.shadowExtentY() * 2.0)
                        + 40.0
                        + (700.0 + THEME.shadowExtentY() * 2.0),
                index.contentHeight(),
                0.001
        );
    }

    @Test
    void computesCaretBoundsInContentCoordinates() {
        PageGeometryIndex index = PageGeometryIndex.from(sampleLayout(), THEME, 40.0);
        CaretGeometry caret = new CaretGeometry(72.0, 96.0, 15.0);

        Bounds bounds = index.caretBoundsInContent(1, caret);

        assertEquals(THEME.shadowExtentX() + 72.0, bounds.getMinX(), 0.001);
        assertEquals(
                THEME.outerPadding()
                        + (700.0 + THEME.shadowExtentY() * 2.0)
                        + 40.0
                        + THEME.shadowExtentY()
                        + 96.0,
                bounds.getMinY(),
                0.001
        );
        assertEquals(2.0, bounds.getWidth(), 0.001);
        assertEquals(15.0, bounds.getHeight(), 0.001);
    }

    @Test
    void mapsContentYToNearestPage() {
        PageGeometryIndex index = PageGeometryIndex.from(sampleLayout(), THEME, 40.0);

        assertEquals(0, index.pageAtContentY(0.0));
        assertEquals(0, index.pageAtContentY(index.pageTopInContent(0) + 10.0));
        assertEquals(1, index.pageAtContentY(index.pageTopInContent(1) + 10.0));
        assertEquals(1, index.pageAtContentY(10_000.0));
    }

    private static LaidOutDocument sampleLayout() {
        PageStyle pageStyle = new PageStyle(500.0, 700.0, 50.0, 50.0, 50.0, 50.0);
        return new LaidOutDocument(
                pageStyle,
                List.of(
                        new LaidOutPage(0, pageStyle.width(), pageStyle.height(), List.of()),
                        new LaidOutPage(1, pageStyle.width(), pageStyle.height(), List.of())
                )
        );
    }
}
