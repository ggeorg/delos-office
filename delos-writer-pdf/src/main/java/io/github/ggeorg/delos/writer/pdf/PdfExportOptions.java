package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.render.RenderColor;
import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.RenderTheme;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;

import java.util.Objects;

/**
 * Options for Writer PDF export.
 *
 * <p>The layout theme and render theme intentionally travel together. Headless
 * PDF generation must measure text with the same PDF-facing font family that it
 * later renders with; otherwise line breaks and pagination can drift between
 * layout and final output.</p>
 */
public record PdfExportOptions(LayoutTheme layoutTheme, RenderTheme renderTheme) {
    public PdfExportOptions {
        layoutTheme = pdfLayoutTheme(layoutTheme);
        renderTheme = renderThemeWithFonts(Objects.requireNonNull(renderTheme, "renderTheme"),
                layoutTheme.titleFont(),
                layoutTheme.bodyFont());
    }

    /**
     * Backwards-compatible constructor for tests/callers that only override the
     * render theme. The PDF layout theme is derived from the same render fonts.
     */
    public PdfExportOptions(RenderTheme renderTheme) {
        this(layoutThemeFromRenderTheme(renderTheme), renderTheme);
    }

    public static PdfExportOptions defaultOptions() {
        LayoutTheme baseLayout = LayoutTheme.defaultTheme();
        RenderFont titleFont = new RenderFont("Helvetica", baseLayout.titleFont().size(), baseLayout.titleFont().bold(), baseLayout.titleFont().italic());
        RenderFont bodyFont = new RenderFont("Helvetica", baseLayout.bodyFont().size(), baseLayout.bodyFont().bold(), baseLayout.bodyFont().italic());
        LayoutTheme pdfLayout = new LayoutTheme(
                titleFont,
                bodyFont,
                baseLayout.titleGap(),
                baseLayout.titleLineGap(),
                baseLayout.separatorGap(),
                baseLayout.paragraphSpacing(),
                baseLayout.bodyLineGap()
        );
        return new PdfExportOptions(pdfLayout, new RenderTheme(
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
        ));
    }

    /**
     * Converts a Writer layout theme into its canonical PDF-facing equivalent.
     */
    public static LayoutTheme pdfLayoutTheme(LayoutTheme theme) {
        LayoutTheme safeTheme = Objects.requireNonNull(theme, "theme");
        return new LayoutTheme(
                PdfStandardFontMapper.resolve(safeTheme.titleFont()),
                PdfStandardFontMapper.resolve(safeTheme.bodyFont()),
                safeTheme.titleGap(),
                safeTheme.titleLineGap(),
                safeTheme.separatorGap(),
                safeTheme.paragraphSpacing(),
                safeTheme.bodyLineGap()
        );
    }

    private static LayoutTheme layoutThemeFromRenderTheme(RenderTheme theme) {
        RenderTheme safeTheme = Objects.requireNonNull(theme, "theme");
        LayoutTheme defaults = LayoutTheme.defaultTheme();
        return new LayoutTheme(
                safeTheme.titleFont(),
                safeTheme.bodyFont(),
                defaults.titleGap(),
                defaults.titleLineGap(),
                defaults.separatorGap(),
                defaults.paragraphSpacing(),
                defaults.bodyLineGap()
        );
    }

    private static RenderTheme renderThemeWithFonts(RenderTheme theme, RenderFont titleFont, RenderFont bodyFont) {
        return new RenderTheme(
                theme.workspaceBackground(),
                theme.pageShadow(),
                theme.pageBackground(),
                theme.pageBorder(),
                theme.separatorColor(),
                theme.titleText(),
                theme.bodyText(),
                theme.selectionFill(),
                Objects.requireNonNull(titleFont, "titleFont"),
                Objects.requireNonNull(bodyFont, "bodyFont"),
                theme.pageCornerRadius(),
                theme.pageShadowOffsetX(),
                theme.pageShadowOffsetY()
        );
    }
}
