package io.github.ggeorg.delos.writer.ui.control;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterStylesheetsContractTest {
    @Test
    void reusableWriterControlCssLivesWithWriterJavaFxControls() throws IOException {
        String css = Files.readString(Path.of("src/main/resources/io/github/ggeorg/delos/writer/ui/control/writer-controls.css"));

        assertNotNull(WriterStylesheets.class.getResource("/io/github/ggeorg/delos/writer/ui/control/writer-controls.css"));
        assertTrue(css.contains(".writer-document-view"));
        assertTrue(css.contains(".delos-editor"));
        assertTrue(css.contains(".editor-scroll"));
        assertTrue(css.contains(".horizontal-ruler"));
        assertTrue(css.contains(".vertical-ruler"));
        assertFalse(css.contains(".writer-toolbar-button"), "app toolbar styles belong in delos-writer-app");
        assertFalse(css.contains(".writer-inspector"), "Writer app inspector shell styles belong in delos-writer-app");
    }
}
