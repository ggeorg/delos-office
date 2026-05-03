package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterInspectorContractTest {
    @Test
    void writerUsesSharedInspectorShellAndOwnsOnlyWriterPanels() throws IOException {
        String mainWindow = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterMainWindow.java"));
        String writerInspector = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/inspector/WriterInspectorPane.java"));
        String sharedInspector = Files.readString(Path.of("../delos-javafx/src/main/java/io/github/ggeorg/delos/javafx/inspector/DelosInspector.java"));

        assertTrue(mainWindow.contains("WriterInspectorPane"));
        assertTrue(mainWindow.contains("private boolean inspectorVisible = true"));
        assertTrue(mainWindow.contains("inspector.setManaged(inspectorVisible)"));
        assertTrue(writerInspector.contains("extends DelosInspector"));
        assertTrue(writerInspector.contains("addTab(\"style\""));
        assertTrue(writerInspector.contains("addTab(\"layout\""));
        assertTrue(writerInspector.contains("addTab(\"document\""));
        assertTrue(sharedInspector.contains("DEFAULT_WIDTH = 300.0"));
        assertFalse(mainWindow.contains("inspectorSlot"), "fixed visible width replaced the rejected slot idea");
    }

    @Test
    void textFormatInspectorExecutesCommandsInsteadOfMutatingVisibleNodes() throws IOException {
        String source = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/inspector/WriterTextFormatInspector.java"));

        assertTrue(source.contains("command.execute()"));
        assertTrue(source.contains("format.bold"));
        assertTrue(source.contains("format.italic"));
        assertTrue(source.contains("format.underline"));
        assertTrue(source.contains("format.strikethrough"));
        assertTrue(source.contains("format.alignLeft"));
        assertTrue(source.contains("format.alignCenter"));
        assertTrue(source.contains("format.alignRight"));
        assertTrue(source.contains("format.justify"));
        assertTrue(source.contains("format.bulletedList"));
        assertTrue(source.contains("format.numberedList"));
        assertTrue(source.contains("format.decreaseListLevel"));
        assertTrue(source.contains("format.increaseListLevel"));
        assertFalse(source.contains("new Text("), "inspector must not create document-rendering text nodes");
    }

    @Test
    void listCommandsAreRealCommandsNotDisabledPlaceholders() throws IOException {
        String provider = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterCommandProvider.java"));
        String inspector = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/inspector/WriterTextFormatInspector.java"));

        assertTrue(provider.contains("editor.toggleListKind(ListMarkerKind.BULLET)"));
        assertTrue(provider.contains("editor.toggleListKind(ListMarkerKind.NUMBERED)"));
        assertTrue(provider.contains("editor::decreaseListLevel"));
        assertTrue(provider.contains("editor::increaseListLevel"));
        assertTrue(inspector.contains("private Node listsSection()"));
        assertFalse(provider.contains("registerDisabled(\"format.bulletedList"));
        assertFalse(provider.contains("registerDisabled(\"format.numberedList"));
    }
}
