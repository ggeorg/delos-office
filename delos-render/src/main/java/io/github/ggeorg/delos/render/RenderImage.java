package io.github.ggeorg.delos.render;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable binary image asset passed through the platform-neutral rendering API.
 *
 * <p>The render layer deliberately treats images as assets, not as JavaFX or
 * PDFBox objects. Concrete targets decide whether they can paint the media type;
 * unsupported images can fall back to the normal document placeholder.</p>
 */
public final class RenderImage {
    private final String source;
    private final String mediaType;
    private final byte[] bytes;

    public RenderImage(String source, String mediaType, byte[] bytes) {
        this.source = Objects.requireNonNullElse(source, "").trim().replace('\\', '/');
        this.mediaType = normalizeMediaType(mediaType, this.source);
        this.bytes = Objects.requireNonNull(bytes, "bytes").clone();
    }

    public String source() {
        return source;
    }

    public String mediaType() {
        return mediaType;
    }

    public byte[] bytes() {
        return bytes.clone();
    }

    public boolean isRasterImage() {
        return "image/png".equals(mediaType)
                || "image/jpeg".equals(mediaType)
                || "image/gif".equals(mediaType);
    }

    private static String normalizeMediaType(String mediaType, String source) {
        String normalized = Objects.requireNonNullElse(mediaType, "").trim().toLowerCase(Locale.ROOT);
        if (!normalized.isBlank()) {
            return normalized;
        }
        String lowerSource = source.toLowerCase(Locale.ROOT);
        if (lowerSource.endsWith(".png")) {
            return "image/png";
        }
        if (lowerSource.endsWith(".jpg") || lowerSource.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lowerSource.endsWith(".gif")) {
            return "image/gif";
        }
        if (lowerSource.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RenderImage image
                && source.equals(image.source)
                && mediaType.equals(image.mediaType)
                && Arrays.equals(bytes, image.bytes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(source, mediaType);
        result = 31 * result + Arrays.hashCode(bytes);
        return result;
    }

    @Override
    public String toString() {
        return "RenderImage[source=" + source + ", mediaType=" + mediaType + ", bytes=" + bytes.length + ']';
    }
}
