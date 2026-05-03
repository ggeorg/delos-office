package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.render.RenderColor;
import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.RenderTheme;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PdfExportOptionsTest {
    @Test
    void defaultOptionsUseTheSamePdfFontsForLayoutAndRender() {
        PdfExportOptions options = PdfExportOptions.defaultOptions();

        assertEquals(options.layoutTheme().titleFont(), options.renderTheme().titleFont());
        assertEquals(options.layoutTheme().bodyFont(), options.renderTheme().bodyFont());
        assertEquals("Helvetica", options.layoutTheme().titleFont().family());
        assertEquals("Times", options.layoutTheme().bodyFont().family());
    }

    @Test
    void canonicalizesLayoutAndRenderFontsTogether() {
        LayoutTheme layout = new LayoutTheme(
                new RenderFont("System", 24.0, false, false),
                new RenderFont("Georgia", 13.5, false, true),
                12.0,
                5.0,
                10.0,
                8.0,
                5.5
        );
        RenderTheme mismatchedRender = renderTheme(
                new RenderFont("Courier New", 24.0, true, false),
                new RenderFont("Arial", 13.5, false, false)
        );

        PdfExportOptions options = new PdfExportOptions(layout, mismatchedRender);

        assertEquals("Helvetica", options.layoutTheme().titleFont().family());
        assertEquals("Times", options.layoutTheme().bodyFont().family());
        assertEquals(options.layoutTheme().titleFont(), options.renderTheme().titleFont());
        assertEquals(options.layoutTheme().bodyFont(), options.renderTheme().bodyFont());
    }

    @Test
    void renderOnlyConstructorDerivesPdfLayoutFontsFromRenderTheme() {
        RenderTheme theme = renderTheme(
                new RenderFont("Courier New", 20.0, true, false),
                new RenderFont("Times New Roman", 11.0, false, false)
        );

        PdfExportOptions options = new PdfExportOptions(theme);

        assertEquals(new RenderFont("Courier", 20.0, true, false), options.layoutTheme().titleFont());
        assertEquals(new RenderFont("Times", 11.0, false, false), options.layoutTheme().bodyFont());
        assertEquals(options.layoutTheme().titleFont(), options.renderTheme().titleFont());
        assertEquals(options.layoutTheme().bodyFont(), options.renderTheme().bodyFont());
    }

    private static RenderTheme renderTheme(RenderFont titleFont, RenderFont bodyFont) {
        return new RenderTheme(
                RenderColor.rgb(255, 255, 255),
                RenderColor.rgba(0, 0, 0, 0.0),
                RenderColor.rgb(255, 255, 255),
                RenderColor.rgb(255, 255, 255),
                RenderColor.rgb(229, 233, 239),
                RenderColor.rgb(31, 37, 46),
                RenderColor.rgb(52, 58, 66),
                RenderColor.rgba(0, 0, 0, 0.0),
                titleFont,
                bodyFont,
                0.0,
                0.0,
                0.0
        );
    }
}
