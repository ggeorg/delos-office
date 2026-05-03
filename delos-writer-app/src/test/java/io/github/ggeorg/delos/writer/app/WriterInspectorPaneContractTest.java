package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterInspectorPaneContractTest {
    @Test
    void writerInspectorUsesActiveSharedShellAndWriterOwnedPanels() throws IOException {
        String source = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/inspector/WriterInspectorPane.java"));

        assertTrue(source.contains("extends DelosInspector"));
        assertTrue(source.contains("addTab(\"style\", \"Style\""));
        assertTrue(source.contains("addTab(\"layout\", \"Layout\""));
        assertTrue(source.contains("addTab(\"document\", \"Document\""));
        assertTrue(source.contains("new WriterPageSetupInspector(session, editor)"));
        assertTrue(source.contains("new WriterTextFormatInspector(editor, commandRegistry)"));
        assertTrue(source.contains("new WriterImageInspector(editor, commandRegistry)"));
        assertTrue(source.contains("new WriterTableInspector(editor, commandRegistry)"));
        assertTrue(source.contains("setInspectorVisible"));
        assertFalse(source.contains("io.github.ggeorg.delos.javafx.chrome"));
    }

    @Test
    void documentInspectorMutatesDocumentModelThroughPageSetupNotPrinterOrSnapshots() throws IOException {
        String source = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/inspector/WriterPageSetupInspector.java"));

        assertTrue(source.contains("PageStyle"));
        assertTrue(source.contains("session.execute(new EditCommand"));
        assertTrue(source.contains("editor.reloadDocument()"));
        assertTrue(source.contains("DocumentEdit"));
        assertTrue(source.contains("Margins are shown in centimeters"));
        assertFalse(source.contains("PrinterJob"));
        assertFalse(source.contains("PDFPageable"));
        assertFalse(source.contains("createLayoutSnapshot"));
    }
}
