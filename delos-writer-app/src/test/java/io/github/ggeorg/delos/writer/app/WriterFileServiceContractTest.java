package io.github.ggeorg.delos.writer.app;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.io.WriterDocumentExtensions;
import io.github.ggeorg.delos.writer.io.WriterDocumentFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterFileServiceContractTest {
    private final WriterFileService service = new WriterFileService();
    private final WriterDocumentFormat format = new WriterDocumentFormat();

    @TempDir
    Path tempDir;

    @Test
    void savesAndReadsNativeDlwDocumentsWithoutFileChooser() throws IOException {
        Document document = format.createBlank("Native Round Trip");
        Path target = tempDir.resolve("native" + WriterDocumentExtensions.DOCUMENT);

        Path saved = service.save(null, target, document, false);
        WriterFileService.LoadedWriterDocument loaded = service.read(saved);

        assertEquals(target, saved);
        assertTrue(Files.exists(saved));
        assertEquals(target, loaded.path());
        assertEquals("Native Round Trip", loaded.document().title());
    }

    @Test
    void normalizesNativeDlwExtensionWhenSavingExistingWriterPath() throws IOException {
        Document document = format.createBlank("Normalized Native Save");
        Path targetWithoutExtension = tempDir.resolve("normalized-native");

        Path saved = service.save(null, targetWithoutExtension, document, false);

        assertEquals(tempDir.resolve("normalized-native" + WriterDocumentExtensions.DOCUMENT), saved);
        assertTrue(Files.exists(saved));
        assertEquals("Normalized Native Save", service.read(saved).document().title());
    }

    @Test
    void preservesLegacyDelosPathWhenSavingExistingLegacyDocument() throws IOException {
        Document document = format.createBlank("Legacy Round Trip");
        Path legacyPath = tempDir.resolve("legacy" + WriterDocumentExtensions.LEGACY_DELOS_XML);

        Path saved = service.save(null, legacyPath, document, false);
        WriterFileService.LoadedWriterDocument loaded = service.read(saved);

        assertEquals(legacyPath, saved);
        assertTrue(Files.exists(saved));
        assertEquals(legacyPath, loaded.path());
        assertEquals("Legacy Round Trip", loaded.document().title());
    }

    @Test
    void preservesLegacyDwritePathWhenSavingExistingLegacyDocument() throws IOException {
        Document document = format.createBlank("Legacy Dwrite Round Trip");
        Path legacyPath = tempDir.resolve("legacy" + WriterDocumentExtensions.LEGACY_DWRITE_XML);

        Path saved = service.save(null, legacyPath, document, false);

        assertEquals(legacyPath, saved);
        assertTrue(Files.exists(saved));
        assertEquals("Legacy Dwrite Round Trip", service.read(saved).document().title());
    }

    @Test
    void saveAsSuggestionAlwaysUsesNativeDlwExtension() {
        assertEquals("legacy" + WriterDocumentExtensions.DOCUMENT,
                WriterFileService.suggestedSaveFileName(Path.of("legacy.delos"), "Ignored"));
        assertEquals("report" + WriterDocumentExtensions.DOCUMENT,
                WriterFileService.suggestedSaveFileName(Path.of("report.dwrite"), "Ignored"));
        assertEquals("Untitled" + WriterDocumentExtensions.DOCUMENT,
                WriterFileService.suggestedSaveFileName(null, ""));
        assertEquals("Untitled" + WriterDocumentExtensions.DOCUMENT,
                WriterFileService.suggestedSaveFileName(null, "Untitled" + WriterDocumentExtensions.DOCUMENT));
        assertEquals("report" + WriterDocumentExtensions.DOCUMENT,
                WriterFileService.suggestedSaveFileName(null, "report" + WriterDocumentExtensions.DOCUMENT));
    }

    @Test
    void pathClassificationSeparatesNativeFromLegacy() {
        assertTrue(WriterFileService.isNativeWriterPath(Path.of("x.dlw")));
        assertFalse(WriterFileService.isNativeWriterPath(Path.of("x.delos")));
        assertTrue(WriterFileService.isLegacyPlainXmlPath(Path.of("x.delos")));
        assertTrue(WriterFileService.isLegacyPlainXmlPath(Path.of("x.dwrite")));
        assertFalse(WriterFileService.isLegacyPlainXmlPath(Path.of("x.dlw")));
    }
}
