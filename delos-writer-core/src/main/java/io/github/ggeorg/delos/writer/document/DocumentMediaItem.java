package io.github.ggeorg.delos.writer.document;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Binary media asset stored inside a native Delos Writer package.
 *
 * <p>The path is package-relative and should normally live below {@code media/},
 * for example {@code media/image-1.png}. The document model keeps media bytes
 * separate from blocks so an {@link ImageBlock} can reference the asset without
 * embedding binary data into {@code content.xml}.</p>
 */
public final class DocumentMediaItem {
    private final String path;
    private final String mediaType;
    private final byte[] bytes;

    public DocumentMediaItem(String path, String mediaType, byte[] bytes) {
        this.path = normalizePath(path);
        this.mediaType = normalizeMediaType(mediaType, this.path);
        this.bytes = Objects.requireNonNull(bytes, "bytes").clone();
    }

    public static DocumentMediaItem image(String path, String mediaType, byte[] bytes) {
        return new DocumentMediaItem(path, mediaType, bytes);
    }

    public String path() {
        return path;
    }

    public String mediaType() {
        return mediaType;
    }

    public byte[] bytes() {
        return bytes.clone();
    }

    byte[] rawBytes() {
        return bytes;
    }

    private static String normalizePath(String value) {
        String normalized = Objects.requireNonNullElse(value, "").trim().replace('\\', '/');
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("media path must not be blank");
        }
        if (normalized.startsWith("/") || normalized.contains(":")) {
            throw new IllegalArgumentException("media path must be package-relative: " + value);
        }
        for (String segment : normalized.split("/")) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("unsafe media path: " + value);
            }
        }
        if (!normalized.startsWith("media/")) {
            throw new IllegalArgumentException("media path must be below media/: " + value);
        }
        return normalized;
    }

    private static String normalizeMediaType(String mediaType, String path) {
        String normalized = Objects.requireNonNullElse(mediaType, "").trim().toLowerCase(Locale.ROOT);
        if (!normalized.isBlank()) {
            return normalized;
        }
        return guessMediaType(path);
    }

    public static String guessMediaType(String path) {
        String normalized = Objects.requireNonNullElse(path, "").toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".png")) {
            return "image/png";
        }
        if (normalized.endsWith(".jpg") || normalized.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (normalized.endsWith(".gif")) {
            return "image/gif";
        }
        if (normalized.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (normalized.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DocumentMediaItem item
                && path.equals(item.path)
                && mediaType.equals(item.mediaType)
                && Arrays.equals(bytes, item.bytes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(path, mediaType);
        result = 31 * result + Arrays.hashCode(bytes);
        return result;
    }

    @Override
    public String toString() {
        return "DocumentMediaItem[path=" + path + ", mediaType=" + mediaType + ", bytes=" + bytes.length + ']';
    }
}
