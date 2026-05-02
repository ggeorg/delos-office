package io.github.ggeorg.delos.writer.pdf;

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
        layoutTheme = HeadlessOutputLayoutPolicy.canonicalLayoutTheme(layoutTheme);
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
        return HeadlessOutputLayoutPolicy.defaultPolicy().exportOptions();
    }

    /**
     * Converts a Writer layout theme into its canonical PDF-facing equivalent.
     */
    public static LayoutTheme pdfLayoutTheme(LayoutTheme theme) {
        return HeadlessOutputLayoutPolicy.canonicalLayoutTheme(theme);
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
