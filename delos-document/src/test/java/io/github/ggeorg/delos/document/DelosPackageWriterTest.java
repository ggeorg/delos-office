package io.github.ggeorg.delos.document;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class DelosPackageWriterTest {
    private static final String WRITER_MEDIA_TYPE = "application/vnd.delos.writer.document";

    @Test
    void writesMimeTypeAsFirstEntry() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new DelosPackageWriter().write(packageWithContent("<doc/>"), output);

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(output.toByteArray()), StandardCharsets.UTF_8)) {
            ZipEntry first = zip.getNextEntry();
            assertEquals(DelosPackageNames.MIMETYPE, first.getName());
            assertEquals(ZipEntry.STORED, first.getMethod());
            assertEquals(WRITER_MEDIA_TYPE, new String(zip.readAllBytes(), StandardCharsets.US_ASCII));
        }
    }

    @Test
    void generatesManifestForPackageParts() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new DelosPackageWriter().write(packageWithContent("<doc/>"), output);

        DelosPackage read = new DelosPackageReader().read(new ByteArrayInputStream(output.toByteArray()));

        String manifest = read.requirePart(DelosPackageNames.MANIFEST_XML).text();
        assertEquals(WRITER_MEDIA_TYPE, read.rootMediaType());
        assertEquals("<doc/>", read.requirePart(DelosPackageNames.CONTENT_XML).text());
        org.junit.jupiter.api.Assertions.assertTrue(manifest.contains("full-path=\"/\""));
        org.junit.jupiter.api.Assertions.assertTrue(manifest.contains(WRITER_MEDIA_TYPE));
        org.junit.jupiter.api.Assertions.assertTrue(manifest.contains("content.xml"));
    }

    @Test
    void rejectsUnsafePackagePartPaths() {
        assertThrows(IllegalArgumentException.class,
                () -> DelosPackagePart.file("../evil.xml", "application/xml", new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> DelosPackagePart.file("/evil.xml", "application/xml", new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> DelosPackagePart.file(DelosPackageNames.MIMETYPE, "text/plain", new byte[0]));
    }

    private static DelosPackage packageWithContent(String contentXml) {
        return new DelosPackage(WRITER_MEDIA_TYPE, List.of(
                DelosPackagePart.xml(DelosPackageNames.CONTENT_XML, contentXml),
                DelosPackagePart.xml(DelosPackageNames.STYLES_XML, "<styles/>"),
                DelosPackagePart.xml(DelosPackageNames.SETTINGS_XML, "<settings/>"),
                DelosPackagePart.xml(DelosPackageNames.META_XML, "<meta/>"),
                DelosPackagePart.directory(DelosPackageNames.MEDIA_DIR)
        ));
    }
}
