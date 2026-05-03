package io.github.ggeorg.delos.writer.ui.control;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.writer.layout.KnuthPlassParagraphLayouter;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;
import io.github.ggeorg.delos.writer.layout.PaginatingDocumentLayoutEngine;
import io.github.ggeorg.delos.writer.render.fx.JavaFxRenderTextMeasurer;
import io.github.ggeorg.delos.writer.session.EditorSession;
import io.github.ggeorg.delos.writer.ui.ViewTheme;

import java.util.Locale;
import java.util.Objects;

/**
 * Explicit policy for the interactive desktop Writer preview.
 *
 * <p>This policy is intentionally JavaFX-facing. It chooses fonts and metrics
 * for the live editing surface, where native glyph quality and styled-run
 * spacing matter more than imitating PDFBox measurement exactly. It still uses
 * the shared Knuth-Plass paragraph breaker so hyphenation works in every
 * alignment mode. Desktop export
 * and print still get WYSIWYG behavior by exporting the immutable layout
 * snapshot produced by this policy.</p>
 */
public final class DesktopPreviewLayoutPolicy {
    private final LayoutTheme layoutTheme;

    private DesktopPreviewLayoutPolicy(LayoutTheme layoutTheme) {
        this.layoutTheme = desktopPreviewLayoutTheme(layoutTheme);
    }

    public static DesktopPreviewLayoutPolicy defaultPolicy() {
        return new DesktopPreviewLayoutPolicy(LayoutTheme.defaultTheme());
    }

    public static DesktopPreviewLayoutPolicy fromLayoutTheme(LayoutTheme layoutTheme) {
        return new DesktopPreviewLayoutPolicy(layoutTheme);
    }

    public LayoutTheme layoutTheme() {
        return layoutTheme;
    }

    public ViewTheme applyTo(ViewTheme theme) {
        return Objects.requireNonNull(theme, "theme").withLayoutTheme(layoutTheme);
    }

    WriterDocumentView createDocumentView(EditorSession session, ViewTheme baseTheme) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(baseTheme, "baseTheme");

        JavaFxRenderTextMeasurer textMeasurer = new JavaFxRenderTextMeasurer();
        return new WriterDocumentView(
                session,
                applyTo(baseTheme),
                new PaginatingDocumentLayoutEngine(new KnuthPlassParagraphLayouter(textMeasurer)),
                textMeasurer
        );
    }

    private static LayoutTheme desktopPreviewLayoutTheme(LayoutTheme theme) {
        LayoutTheme safeTheme = Objects.requireNonNull(theme, "theme");
        return new LayoutTheme(
                desktopPreviewFont(safeTheme.titleFont()),
                desktopPreviewFont(safeTheme.bodyFont()),
                safeTheme.titleGap(),
                safeTheme.titleLineGap(),
                safeTheme.separatorGap(),
                safeTheme.paragraphSpacing(),
                safeTheme.bodyLineGap()
        );
    }

    /**
     * Converts only PDF Standard-14 internal family names back to JavaFX-safe
     * logical families for the live editor.
     *
     * <p>Do not map the shared document default {@code Serif} to {@code Times}
     * here. On JavaFX, {@code Times} is not a portable logical family and may
     * fall back to a sans font on some platforms. The live editor should keep
     * JavaFX logical families unless it receives a PDF-canonical family name
     * from an output policy.</p>
     */
    private static RenderFont desktopPreviewFont(RenderFont font) {
        Objects.requireNonNull(font, "font");
        String family = font.family();
        String normalized = family.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("times")) {
            family = "Serif";
        } else if (normalized.equals("courier")) {
            family = "Monospaced";
        }
        return new RenderFont(family, font.size(), font.bold(), font.italic());
    }
}
