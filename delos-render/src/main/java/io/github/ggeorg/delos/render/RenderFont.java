package io.github.ggeorg.delos.render;

/**
 * Platform-neutral font descriptor used by renderers and render measurers.
 */
public record RenderFont(String family, double size, boolean bold, boolean italic) {
    public RenderFont {
        family = family == null || family.isBlank() ? "System" : family;
        size = Double.isFinite(size) && size > 0.0 ? size : 12.0;
    }

    public RenderFont withStyle(boolean bold, boolean italic) {
        return new RenderFont(family, size, bold, italic);
    }
}
