package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterSharedUiContractTest {
    @Test
    void writerUsesSharedDelosUiHelpersButDoesNotDependOnIkonliDirectly() throws IOException {
        String toolbar = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterToolBar.java"));
        String menuBar = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterMenuBar.java"));
        String inspector = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/inspector/WriterInspectorPane.java"));
        String app = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/DelosWriterApp.java"));

        assertTrue(toolbar.contains("DelosToolBars"));
        assertTrue(toolbar.contains("DelosToolbarGroup"));
        assertTrue(toolbar.contains("DelosIconId"));
        assertTrue(menuBar.contains("DelosMenus"));
        assertTrue(inspector.contains("DelosInspector"));
        assertTrue(inspector.contains("InspectorSection"));
        assertTrue(app.contains("DelosStylesheets.addTo(scene)"));
        assertTrue(app.contains("WriterStylesheets.addTo(scene)"));
        assertFalse(toolbar.contains("org.kordamp.ikonli"));
        assertFalse(menuBar.contains("org.kordamp.ikonli"));
        assertFalse(inspector.contains("org.kordamp.ikonli"));
    }
}
