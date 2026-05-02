package io.github.ggeorg.delos.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Reads native Delos ZIP packages and enforces the package-level invariants.
 */
public final class DelosPackageReader {
    private static final int BUFFER_SIZE = 8192;

    public DelosPackage read(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream");
        try (ZipInputStream zip = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {
            ZipEntry first = zip.getNextEntry();
            if (first == null) {
                throw new IOException("empty Delos package");
            }
            if (!DelosPackageNames.MIMETYPE.equals(first.getName())) {
                throw new IOException("Delos package must start with the mimetype entry");
            }
            if (first.isDirectory()) {
                throw new IOException("Delos mimetype entry must be a file");
            }
            String rootMediaType = readEntryBytes(zip).toString(StandardCharsets.US_ASCII).trim();
            zip.closeEntry();
            if (rootMediaType.isEmpty()) {
                throw new IOException("Delos package mimetype is blank");
            }

            List<DelosPackagePart> parts = new ArrayList<>();
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (DelosPackageNames.MIMETYPE.equals(name)) {
                    throw new IOException("Delos package contains duplicate mimetype entry");
                }
                if (entry.isDirectory()) {
                    parts.add(DelosPackagePart.directory(name));
                } else {
                    parts.add(DelosPackagePart.file(name, "", readEntryBytes(zip).toByteArray()));
                }
                zip.closeEntry();
            }
            return new DelosPackage(rootMediaType, parts);
        } catch (IllegalArgumentException exception) {
            throw new IOException("invalid Delos package", exception);
        }
    }

    private static ByteArrayOutputStream readEntryBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output;
    }
}
