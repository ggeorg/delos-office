package io.github.ggeorg.delos.document;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * One validated entry inside a native Delos ZIP package.
 */
public record DelosPackagePart(String path, String mediaType, byte[] bytes, boolean directory) {
    public DelosPackagePart {
        path = normalizePath(path, directory);
        mediaType = Objects.requireNonNullElse(mediaType, "").trim();
        bytes = directory ? new byte[0] : Objects.requireNonNull(bytes, "bytes").clone();
    }

    public static DelosPackagePart xml(String path, String xml) {
        return file(path, "application/xml", xml.getBytes(StandardCharsets.UTF_8));
    }

    public static DelosPackagePart file(String path, String mediaType, byte[] bytes) {
        return new DelosPackagePart(path, mediaType, bytes, false);
    }

    public static DelosPackagePart directory(String path) {
        return new DelosPackagePart(path, "", new byte[0], true);
    }

    public String text() {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }

    byte[] rawBytes() {
        return bytes;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DelosPackagePart part
                && path.equals(part.path)
                && mediaType.equals(part.mediaType)
                && directory == part.directory
                && Arrays.equals(bytes, part.bytes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(path, mediaType, directory);
        result = 31 * result + Arrays.hashCode(bytes);
        return result;
    }

    private static String normalizePath(String value, boolean directory) {
        String normalized = Objects.requireNonNullElse(value, "").trim().replace('\\', '/');
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("package part path must not be blank");
        }
        if (DelosPackageNames.MIMETYPE.equals(normalized)) {
            throw new IllegalArgumentException("mimetype is controlled by DelosPackageWriter");
        }
        if (normalized.startsWith("/") || normalized.contains(":")) {
            throw new IllegalArgumentException("package part path must be relative: " + value);
        }
        for (String segment : normalized.split("/")) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("unsafe package part path: " + value);
            }
        }
        if (directory && !normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        if (!directory && normalized.endsWith("/")) {
            throw new IllegalArgumentException("file package part must not end with '/': " + value);
        }
        return normalized;
    }
}
