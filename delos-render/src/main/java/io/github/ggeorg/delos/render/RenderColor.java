package io.github.ggeorg.delos.render;

/**
 * Platform-neutral RGBA color used by the renderer.
 */
public record RenderColor(double red, double green, double blue, double alpha) {
    public RenderColor {
        red = clamp(red);
        green = clamp(green);
        blue = clamp(blue);
        alpha = clamp(alpha);
    }

    public static RenderColor rgb(int red, int green, int blue) {
        return rgba(red, green, blue, 1.0);
    }

    public static RenderColor rgba(int red, int green, int blue, double alpha) {
        return new RenderColor(red / 255.0, green / 255.0, blue / 255.0, alpha);
    }

    private static double clamp(double value) {
        if (Double.isNaN(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }
}
