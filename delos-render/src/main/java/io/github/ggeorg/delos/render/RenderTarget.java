package io.github.ggeorg.delos.render;

/**
 * Platform-neutral drawing surface for page rendering.
 */
public interface RenderTarget {
    void save();

    void restore();

    void translate(double x, double y);

    void clip(double x, double y, double width, double height);

    void setGlobalAlpha(double alpha);

    void clearRect(double x, double y, double width, double height);

    void setFill(RenderColor color);

    void setStroke(RenderColor color);

    void setLineWidth(double lineWidth);

    void setFont(RenderFont font);

    void fillRect(double x, double y, double width, double height);

    void strokeRect(double x, double y, double width, double height);

    void fillRoundRect(double x, double y, double width, double height, double arcWidth, double arcHeight);

    void strokeRoundRect(double x, double y, double width, double height, double arcWidth, double arcHeight);

    void strokeLine(double startX, double startY, double endX, double endY);

    void fillText(String text, double x, double y);

    void fillPath(RenderPath path);

    void strokePath(RenderPath path);

    /**
     * Attempts to draw an image asset into the supplied rectangle.
     *
     * <p>Targets return {@code false} when the image is unresolved, unsupported,
     * or cannot be decoded. Callers should then draw a placeholder instead of
     * failing the whole document render.</p>
     */
    default boolean drawImage(RenderImage image, double x, double y, double width, double height) {
        return false;
    }
}
