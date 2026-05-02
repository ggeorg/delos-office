package io.github.ggeorg.delos.document;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Objects;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes native Delos ZIP packages using ODF-style package discipline.
 */
public final class DelosPackageWriter {
    public void write(DelosPackage delosPackage, OutputStream outputStream) throws IOException {
        Objects.requireNonNull(delosPackage, "delosPackage");
        Objects.requireNonNull(outputStream, "outputStream");

        DelosPackage packageWithManifest = delosPackage.withGeneratedManifest();
        try (ZipOutputStream zip = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            writeStoredMimeTypeFirst(zip, packageWithManifest.rootMediaType());
            for (DelosPackagePart part : packageWithManifest.parts().stream()
                    .sorted(Comparator.comparing(DelosPackagePart::path))
                    .toList()) {
                writePart(zip, part);
            }
        }
    }

    private static void writeStoredMimeTypeFirst(ZipOutputStream zip, String mediaType) throws IOException {
        byte[] bytes = mediaType.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(bytes);

        ZipEntry entry = new ZipEntry(DelosPackageNames.MIMETYPE);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(bytes.length);
        entry.setCompressedSize(bytes.length);
        entry.setCrc(crc.getValue());
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }

    private static void writePart(ZipOutputStream zip, DelosPackagePart part) throws IOException {
        ZipEntry entry = new ZipEntry(part.path());
        zip.putNextEntry(entry);
        if (!part.directory()) {
            zip.write(part.rawBytes());
        }
        zip.closeEntry();
    }
}
