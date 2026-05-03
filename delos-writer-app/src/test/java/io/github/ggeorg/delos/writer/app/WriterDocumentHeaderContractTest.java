package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterDocumentHeaderContractTest {
    @Test
    void documentHeaderOwnsOnlyDocumentChromeNotNativeWindowChrome() throws IOException {
        String source = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterDocumentHeader.java"));

        assertTrue(source.contains("Delos Writer"));
        assertTrue(source.contains("edit.undo"));
        assertTrue(source.contains("edit.redo"));
        assertTrue(source.contains("view.toggleInspector"));
        assertTrue(source.contains("setPrefHeight(32.0)"));
        assertTrue(source.contains("void refresh(String displayName, boolean dirty)"));
        assertFalse(source.contains("initStyle"));
        assertFalse(source.contains("setOnMouseDragged"));
        assertFalse(source.contains("setFullScreen"));
    }
}
