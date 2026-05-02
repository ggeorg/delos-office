package io.github.ggeorg.delos.document;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class DocumentRegistryTest {
    @Test
    void findsFormatsByTypeAndExtension() {
        DocumentFormat<String> writer = new StringFormat(new DocumentType("writer", "Writer", ".dlw", "text/plain"));
        DocumentFormat<String> sheet = new StringFormat(new DocumentType("sheet", "Sheet", ".dls", "text/plain"));

        DocumentRegistry registry = DocumentRegistry.of(writer, sheet);

        assertEquals(writer, registry.findByTypeId("writer").orElseThrow());
        assertEquals(writer, registry.findByExtension(Path.of("notes.dlw")).orElseThrow());
        assertEquals(sheet, registry.findByExtension(Path.of("budget.dls")).orElseThrow());
        assertEquals(Path.of("notes.dlw"), writer.type().normalize(Path.of("notes")));
    }

    @Test
    void rejectsDuplicateExtensions() {
        DocumentFormat<String> first = new StringFormat(new DocumentType("writer", "Writer", ".dlw", "text/plain"));
        DocumentFormat<String> second = new StringFormat(new DocumentType("writer-template", "Writer Template", ".dlw", "text/plain"));

        assertThrows(IllegalArgumentException.class, () -> DocumentRegistry.of(first, second));
    }

    private record StringFormat(DocumentType type) implements DocumentFormat<String> {
        @Override
        public String createBlank(String title) {
            return title;
        }

        @Override
        public String read(InputStream inputStream) throws IOException {
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }

        @Override
        public void write(String document, OutputStream outputStream) throws IOException {
            outputStream.write(document.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
