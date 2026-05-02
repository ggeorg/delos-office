package io.github.ggeorg.delos.writer.io;

import io.github.ggeorg.delos.document.DelosPackageNames;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.DocumentMediaItem;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.document.PageStyle;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

final class WriterDocumentFormatTest {
    @Test
    void roundTripsNativeWriterPackage() throws Exception {
        WriterDocumentFormat format = new WriterDocumentFormat();
        Document original = format.createBlank("Notes");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        format.write(original, out);

        Document loaded = format.read(new ByteArrayInputStream(out.toByteArray()));

        assertEquals("Notes", loaded.title());
        assertEquals(WriterDocumentExtensions.DOCUMENT, format.type().extension());
        assertEquals(WriterDocumentMimeTypes.DOCUMENT, format.type().mediaType());
        assertPackageMimeType(out.toByteArray(), format.type().mediaType());
        assertPackageContains(out.toByteArray(), DelosPackageNames.CONTENT_XML);
        assertPackageContains(out.toByteArray(), DelosPackageNames.MANIFEST_XML);
    }

    @Test
    void roundTripsImageMediaInsideNativePackage() throws Exception {
        WriterDocumentFormat format = new WriterDocumentFormat();
        byte[] imageBytes = new byte[]{1, 2, 3, 4};
        Document original = Document.fromBlocks(
                "Images",
                PageStyle.a4Default(),
                List.of(new ImageBlock("media/image-1.png", 160, 90, "Image one")),
                List.of(DocumentMediaItem.image("media/image-1.png", "image/png", imageBytes))
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        format.write(original, out);
        Document loaded = format.read(new ByteArrayInputStream(out.toByteArray()));

        assertPackageContains(out.toByteArray(), "media/image-1.png");
        assertEquals(original.blocks(), loaded.blocks());
        assertEquals(1, loaded.mediaItems().size());
        assertEquals("media/image-1.png", loaded.mediaItems().getFirst().path());
        assertEquals("image/png", loaded.mediaItems().getFirst().mediaType());
        assertArrayEquals(imageBytes, loaded.mediaItems().getFirst().bytes());
    }

    @Test
    void readsLegacyPlainXmlWriterDocuments() throws Exception {
        WriterDocumentFormat format = new WriterDocumentFormat();
        Document original = format.createBlank("Legacy XML");
        String legacyXml = new DocumentSerializer().toXml(original);

        Document loaded = format.read(new ByteArrayInputStream(legacyXml.getBytes(StandardCharsets.UTF_8)));

        assertEquals("Legacy XML", loaded.title());
    }

    private static void assertPackageMimeType(byte[] bytes, String expectedMediaType) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            ZipEntry first = zip.getNextEntry();
            assertEquals(DelosPackageNames.MIMETYPE, first.getName());
            assertEquals(ZipEntry.STORED, first.getMethod());
            assertEquals(expectedMediaType, new String(zip.readAllBytes(), StandardCharsets.US_ASCII));
        }
    }

    private static void assertPackageContains(byte[] bytes, String expectedEntry) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (expectedEntry.equals(entry.getName())) {
                    return;
                }
            }
        }
        fail("Missing package entry: " + expectedEntry);
    }
}
