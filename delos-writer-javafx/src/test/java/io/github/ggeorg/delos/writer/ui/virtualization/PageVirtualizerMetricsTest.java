package io.github.ggeorg.delos.writer.ui.virtualization;

import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.ui.ViewTheme;
import io.github.ggeorg.delos.writer.ui.geometry.PageGeometryIndex;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageVirtualizerMetricsTest {
    private static final ViewTheme THEME = ViewTheme.defaultTheme();

    @Test
    void computesVisiblePageWindowFromAbsoluteContentCoordinates() {
        PageGeometryIndex geometry = PageGeometryIndex.from(layoutWithPages(5), THEME, THEME.interPageGap());
        PageVirtualizerMetrics metrics = new PageVirtualizerMetrics(0.0);

        var window = metrics.visibleWindow(
                geometry,
                geometry.pageTopInContent(2) + 10.0,
                100.0
        );

        assertEquals(2, window.firstPageIndex());
        assertEquals(2, window.lastPageIndex());
    }

    @Test
    void appliesOverscanAroundVisiblePageWindow() {
        PageGeometryIndex geometry = PageGeometryIndex.from(layoutWithPages(5), THEME, THEME.interPageGap());
        PageVirtualizerMetrics metrics = new PageVirtualizerMetrics(1.0);

        var window = metrics.visibleWindow(
                geometry,
                geometry.pageTopInContent(2) + 10.0,
                100.0
        );

        assertEquals(1, window.firstPageIndex());
        assertEquals(3, window.lastPageIndex());
    }

    @Test
    void emptyWindowForEmptyLayout() {
        PageGeometryIndex geometry = PageGeometryIndex.from(null, THEME, THEME.interPageGap());

        var window = new PageVirtualizerMetrics().visibleWindow(geometry, 0.0, 100.0);

        assertTrue(window.isEmpty());
        assertFalse(window.contains(0));
    }

    private static LaidOutDocument layoutWithPages(int count) {
        PageStyle pageStyle = new PageStyle(500.0, 700.0, 50.0, 50.0, 50.0, 50.0);
        List<LaidOutPage> pages = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            pages.add(new LaidOutPage(index, pageStyle.width(), pageStyle.height(), List.of()));
        }
        return new LaidOutDocument(pageStyle, pages);
    }
}
