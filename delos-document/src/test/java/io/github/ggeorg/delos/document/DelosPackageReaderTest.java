package io.github.ggeorg.delos.document;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class DelosPackageReaderTest {
    private static final String WRITER_MEDIA_TYPE = "application/vnd.delos.writer.document";

    @Test
    void readsMimeTypeAndContentPart() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new DelosPackageWriter().write(new DelosPackage(WRITER_MEDIA_TYPE, List.of(
                DelosPackagePart.xml(DelosPackageNames.CONTENT_XML, "<doc/>"),
                DelosPackagePart.xml(DelosPackageNames.STYLES_XML, "<styles/>"),
                DelosPackagePart.xml(DelosPackageNames.SETTINGS_XML, "<settings/>"),
                DelosPackagePart.xml(DelosPackageNames.META_XML, "<meta/>"),
                DelosPackagePart.directory(DelosPackageNames.MEDIA_DIR)
        )), output);

        DelosPackage read = new DelosPackageReader().read(new ByteArrayInputStream(output.toByteArray()));

        assertEquals(WRITER_MEDIA_TYPE, read.rootMediaType());
        assertEquals("<doc/>", read.requirePart(DelosPackageNames.CONTENT_XML).text());
    }

    @Test
    void rejectsPackagesWithoutMimeTypeFirst() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry(DelosPackageNames.CONTENT_XML));
            zip.write("<doc/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        assertThrows(java.io.IOException.class,
                () -> new DelosPackageReader().read(new ByteArrayInputStream(output.toByteArray())));
    }
}
