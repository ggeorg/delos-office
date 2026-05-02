package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterToolBarContractTest {
    private static final Path TOOLBAR = Path.of(
            "src/main/java/io/github/ggeorg/delos/writer/app/WriterToolBar.java"
    );

    @Test
    void toolbarStaysSimpleAndLeavesDetailedFormattingToInspector() throws IOException {
        String source = Files.readString(TOOLBAR);

        assertTrue(source.contains("button(\"file.print\""));
        assertTrue(source.contains("button(\"insert.image\""));
        assertTrue(source.contains("button(\"insert.table\""));
        assertTrue(source.contains("button(\"insert.formula\""));
        assertTrue(source.contains("toggleButton(\"view.toggleRuler\""));
        assertTrue(source.contains("toggleButton(\"view.toggleInspector\""));
        assertTrue(source.contains("ZoomPresetPicker"));
        assertTrue(source.contains("new ZoomPreset(\"view.zoomFitWidth\", \"Fit\")"));
        assertTrue(source.contains("new ZoomPreset(\"view.zoom90\", \"90%\")"));
        assertTrue(source.contains("commandRegistry.byId(newValue.commandId()).ifPresent(EditorCommand::execute)"));

        assertFalse(source.contains("stylePicker()"));
        assertFalse(source.contains("fontPicker()"));
        assertFalse(source.contains("fontSizePicker()"));
        assertFalse(source.contains("toggleButton(\"format.bold\""));
        assertFalse(source.contains("button(\"format.bulletedList\""));
    }
}
