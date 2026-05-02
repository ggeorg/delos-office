package io.github.ggeorg.delos.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentIoAtomicWriteTest {
    private static final DocumentType TYPE = new DocumentType("writer", "Writer", ".dlw", "text/plain");

    @TempDir
    Path tempDir;

    @Test
    void normalizesExtensionAndWritesAtomically() throws IOException {
        DocumentIo io = new DocumentIo(DocumentRegistry.of(new StringFormat(TYPE)));

        Path saved = io.write(tempDir.resolve("notes"), new StringFormat(TYPE), "hello");

        assertEquals(tempDir.resolve("notes.dlw"), saved);
        assertEquals("hello", Files.readString(saved));
        assertNoTempFilesRemain();
    }

    @Test
    void readsRegisteredFormatByExtension() throws IOException {
        Path target = tempDir.resolve("notes.dlw");
        Files.writeString(target, "hello", StandardCharsets.UTF_8);
        DocumentIo io = new DocumentIo(DocumentRegistry.of(new StringFormat(TYPE)));

        DocumentPackage<?> loaded = io.read(target);

        assertEquals(target, loaded.path());
        assertEquals(TYPE, loaded.type());
        assertEquals("hello", loaded.content());
    }

    @Test
    void cleansTemporaryFileWhenWriteFails() throws IOException {
        Path target = tempDir.resolve("notes.dlw");
        DocumentIo io = new DocumentIo(DocumentRegistry.of(new FailingFormat(TYPE)));

        try {
            io.write(target, new FailingFormat(TYPE), "hello");
        } catch (IOException expected) {
            // expected
        }

        assertFalse(Files.exists(target));
        assertNoTempFilesRemain();
    }

    private void assertNoTempFilesRemain() throws IOException {
        try (var paths = Files.list(tempDir)) {
            assertTrue(paths.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }

    private record StringFormat(DocumentType type) implements DocumentFormat<String> {
        @Override
        public String createBlank(String title) {
            return title;
        }

        @Override
        public String read(InputStream inputStream) throws IOException {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        @Override
        public void write(String document, OutputStream outputStream) throws IOException {
            outputStream.write(document.getBytes(StandardCharsets.UTF_8));
        }
    }

    private record FailingFormat(DocumentType type) implements DocumentFormat<String> {
        @Override
        public String createBlank(String title) {
            return title;
        }

        @Override
        public String read(InputStream inputStream) {
            return "";
        }

        @Override
        public void write(String document, OutputStream outputStream) throws IOException {
            outputStream.write(document.getBytes(StandardCharsets.UTF_8));
            throw new IOException("boom");
        }
    }
}
