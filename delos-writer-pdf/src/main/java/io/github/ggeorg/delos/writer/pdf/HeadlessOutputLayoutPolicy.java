package io.github.ggeorg.delos.writer.pdf;

import io.github.ggeorg.delos.render.RenderColor;
import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.RenderTheme;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;

import java.util.Objects;

/**
 * Explicit policy for JavaFX-free Writer output layout.
 *
 * <p>This policy belongs to the PDF/reporting side of the architecture. It owns
 * PDF-facing font canonicalization and render theme defaults for headless
 * export. The interactive desktop preview has its own policy in
 * {@code delos-writer-javafx}; the two are intentionally separate so preview
 * quality cannot accidentally depend on PDF renderer internals.</p>
 */
public final class HeadlessOutputLayoutPolicy {
    private final LayoutTheme layoutTheme;
    private final RenderTheme renderTheme;

    private HeadlessOutputLayoutPolicy(LayoutTheme layoutTheme, RenderTheme renderTheme) {
        LayoutTheme safeLayoutTheme = canonicalLayoutTheme(layoutTheme);
        this.layoutTheme = safeLayoutTheme;
        this.renderTheme = renderThemeWithFonts(
                Objects.requireNonNull(renderTheme, "renderTheme"),
                safeLayoutTheme.titleFont(),
                safeLayoutTheme.bodyFont()
        );
    }

    public static HeadlessOutputLayoutPolicy defaultPolicy() {
        LayoutTheme outputLayout = canonicalLayoutTheme(LayoutTheme.defaultTheme());
        return new HeadlessOutputLayoutPolicy(outputLayout, defaultRenderTheme(outputLayout));
    }

    public static HeadlessOutputLayoutPolicy of(LayoutTheme layoutTheme, RenderTheme renderTheme) {
        return new HeadlessOutputLayoutPolicy(layoutTheme, renderTheme);
    }

    public static HeadlessOutputLayoutPolicy fromExportOptions(PdfExportOptions options) {
        Objects.requireNonNull(options, "options");
        return new HeadlessOutputLayoutPolicy(options.layoutTheme(), options.renderTheme());
    }

    public LayoutTheme layoutTheme() {
        return layoutTheme;
    }

    public RenderTheme renderTheme() {
        return renderTheme;
    }

    public PdfExportOptions exportOptions() {
        return new PdfExportOptions(layoutTheme, renderTheme);
    }

    public LayoutTheme layoutThemeFor(LayoutTheme requestedTheme) {
        return canonicalLayoutTheme(requestedTheme);
    }

    public static LayoutTheme canonicalLayoutTheme(LayoutTheme theme) {
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

    private static RenderTheme defaultRenderTheme(LayoutTheme layoutTheme) {
        return new RenderTheme(
                RenderColor.rgb(255, 255, 255),
                RenderColor.rgba(0, 0, 0, 0.0),
                RenderColor.rgb(255, 255, 255),
                RenderColor.rgb(255, 255, 255),
                RenderColor.rgb(229, 233, 239),
                RenderColor.rgb(31, 37, 46),
                RenderColor.rgb(52, 58, 66),
                RenderColor.rgba(0, 0, 0, 0.0),
                layoutTheme.titleFont(),
                layoutTheme.bodyFont(),
                0.0,
                0.0,
                0.0
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
