package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterInspectorPaneContractTest {
    @Test
    void writerInspectorUsesSharedShellWithPagesStyleTabs() throws IOException {
        String source = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterInspectorPane.java"));

        assertTrue(source.contains("extends DelosInspector"));
        assertTrue(source.contains("new InspectorTab(\"document\", \"Document\""));
        assertTrue(source.contains("new InspectorTab(\"format\", \"Format\""));
        assertTrue(source.contains("new InspectorTab(\"arrange\", \"Arrange\""));
        assertTrue(source.contains("new InspectorSection(\"Page Setup\""));
        assertTrue(source.contains("new InspectorSection(\"Text\""));
        assertTrue(source.contains("new InspectorSection(\"Paragraph\""));
        assertTrue(source.contains("new InspectorSection(\"Lists\""));
        assertTrue(source.contains("new InspectorSection(\"Image\""));
        assertTrue(source.contains("new InspectorSection(\"Table\""));
        assertTrue(source.contains("SegmentedControl"));
        assertTrue(source.contains("new SegmentedOption(\"style\", \"Style\")"));
        assertTrue(source.contains("new SegmentedOption(\"layout\", \"Layout\")"));
        assertTrue(source.contains("new SegmentedOption(\"more\", \"More\")"));
        assertTrue(source.contains("new InspectorSection(\"Pagination\""));
    }

    @Test
    void documentInspectorMutatesDocumentModelThroughPageSetupNotPrinterOrSnapshots() throws IOException {
        String source = Files.readString(Path.of("src/main/java/io/github/ggeorg/delos/writer/app/WriterInspectorPane.java"));

        assertTrue(source.contains("PageSetup"));
        assertTrue(source.contains("PageSize"));
        assertTrue(source.contains("PageMargins"));
        assertTrue(source.contains("PageOrientation"));
        assertTrue(source.contains("session.execute(new PageStyleCommand"));
        assertTrue(source.contains("editor.reloadDocument()"));
        assertTrue(source.contains("oldDocument.withPageStyle"));
        assertFalse(source.contains("PrinterJob"));
        assertFalse(source.contains("PDFPageable"));
        assertFalse(source.contains("createLayoutSnapshot"));
    }
}
