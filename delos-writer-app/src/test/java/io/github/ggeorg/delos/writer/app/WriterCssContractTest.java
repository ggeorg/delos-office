package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterCssContractTest {
    @Test
    void writerCssKeepsOnlyActiveWriterAppStyles() throws IOException {
        String css = Files.readString(Path.of("src/main/resources/io/github/ggeorg/delos/writer/app/delos-writer.css"));

        assertTrue(css.contains(".writer-toolbar-button"));
        assertTrue(css.contains(".writer-toolbar-separator"));
        assertTrue(css.contains(".writer-page-setup-inspector"));
        assertTrue(css.contains(".writer-table-inspector"));
        assertTrue(css.contains(".danger-button"));
        assertFalse(css.contains(".writer-document-view"), "Reusable Writer control CSS belongs in delos-writer-javafx.");
        assertFalse(css.contains(".horizontal-ruler"), "Reusable ruler CSS belongs in delos-writer-javafx.");
        assertFalse(css.contains(".editor-scroll"), "Reusable editor viewport CSS belongs in delos-writer-javafx.");
        assertFalse(css.contains(".delos-spreadsheet"), "Calc/spreadsheet styles do not belong in Writer CSS.");
        assertFalse(css.contains(".workspace-switcher"), "Old multi-workspace demo styles do not belong in Writer CSS.");
        assertFalse(css.contains(".title-bar"), "Deleted legacy title-bar styles should not live in Writer app CSS.");
        assertFalse(css.contains(".hamburger-menu"), "Deleted legacy title-bar styles should not live in Writer app CSS.");
    }
}
