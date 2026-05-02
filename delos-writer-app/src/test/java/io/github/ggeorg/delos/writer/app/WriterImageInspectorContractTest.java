package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterImageInspectorContractTest {
    private static final Path INSPECTOR_PANE = Path.of(
            "src/main/java/io/github/ggeorg/delos/writer/app/inspector/WriterInspectorPane.java"
    );
    private static final Path IMAGE_INSPECTOR = Path.of(
            "src/main/java/io/github/ggeorg/delos/writer/app/inspector/WriterImageInspector.java"
    );
    private static final Path COMMAND_PROVIDER = Path.of(
            "src/main/java/io/github/ggeorg/delos/writer/app/WriterCommandProvider.java"
    );

    @Test
    void formatInspectorContainsImageInspectorAndRefreshesItWithSelectionChanges() throws IOException {
        String pane = Files.readString(INSPECTOR_PANE);

        assertTrue(pane.contains("new WriterImageInspector(editor, commandRegistry)"));
        assertTrue(pane.contains("formatPanel()"));
        assertTrue(pane.contains("imageInspector.refresh()"));
        assertTrue(pane.contains("setInspectorVisible(imageInspector, hasImage)"));
    }

    @Test
    void imageInspectorEditsOnlyImageBlockPropertiesThroughEditorCommands() throws IOException {
        String source = Files.readString(IMAGE_INSPECTOR);

        assertTrue(source.contains("editor.selectedImageBlock()"));
        assertTrue(source.contains("editor.hasSelectedImageBlock()"));
        assertTrue(source.contains("editor.updateSelectedImageProperties("));
        assertTrue(source.contains("SpinnerValueFactory.DoubleSpinnerValueFactory"));
        assertTrue(source.contains("new CheckBox(\"Keep aspect ratio\")"));
        assertTrue(source.contains("new TextField()"));
        assertTrue(source.contains("image.replace"));
        assertTrue(source.contains("replaceImageCommand.execute()"));

        assertFalse(source.contains("new ImageView("), "inspector must not become the image rendering surface");
        assertFalse(source.contains("new ImageBlock("), "inspector should delegate model replacement to editor/session code");
    }

    @Test
    void replaceImageIsARealCommandEnabledOnlyForSelectedImageBlocks() throws IOException {
        String provider = Files.readString(COMMAND_PROVIDER);

        assertTrue(provider.contains("register(\"image.replace\", \"Replace Image…\", \"Format\""));
        assertTrue(provider.contains("insertController::insertImage, editor::hasSelectedImageBlock"));
        assertFalse(provider.contains("registerDisabled(\"image.replace\""));
    }
}
