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
    void toolbarUsesSkinlessFilenameSaveAsTriggerAndPagesLikeGroupedChrome() throws IOException {
        String source = Files.readString(TOOLBAR);

        assertTrue(source.contains("documentCluster()"));
        assertTrue(source.contains("localIconButton(DelosIconId.LEFT_SIDEBAR"));
        assertTrue(source.contains("new FileTitleButton(commandRegistry, displayName, dirty, renameDocumentTitle)"));
        assertTrue(source.contains("writer-toolbar-file-title-button"));
        assertTrue(source.contains("setOnAction(event -> commandRegistry.byId(\"file.saveAs\").ifPresent(EditorCommand::execute))"));
        assertTrue(source.contains("stripWriterExtension"));
        assertTrue(source.contains("button(\"edit.undo\""));
        assertTrue(source.contains("button(\"edit.redo\""));
        assertTrue(source.contains("button(\"edit.cut\""));
        assertTrue(source.contains("button(\"edit.copy\""));
        assertTrue(source.contains("button(\"edit.paste\""));
        assertTrue(source.contains("button(\"insert.image\""));
        assertTrue(source.contains("button(\"insert.table\""));
        assertTrue(source.contains("formulaButton()"));
        assertTrue(source.contains("shareButton()"));
        assertTrue(source.contains("toggleButton(\"view.toggleInspector\""));
        assertTrue(source.contains("pageLayoutButton(selectInspectorTab)"));
        assertTrue(source.contains("ZoomPresetPicker"));

        assertFalse(source.contains("DelosBalloonPopover"));
        assertFalse(source.contains("TextField"));
        assertFalse(source.contains("GridPane"));
        assertFalse(source.contains("ContextMenu menu = new ContextMenu(DelosMenus.item(commandRegistry, \"file.save\")"));
        assertFalse(source.contains("MenuButton"));
        assertFalse(source.contains("SplitMenuButton"));
        assertFalse(source.contains("button(\"file.print\""));
        assertFalse(source.contains("button(\"format.bold\""));
        assertFalse(source.contains("DelosIconId.CHART"));
    }
}
