package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterTableInspectorContractTest {
    private static final Path INSPECTOR_PANE = Path.of(
            "src/main/java/io/github/ggeorg/delos/writer/app/inspector/WriterInspectorPane.java"
    );
    private static final Path TABLE_INSPECTOR = Path.of(
            "src/main/java/io/github/ggeorg/delos/writer/app/inspector/WriterTableInspector.java"
    );

    @Test
    void formatInspectorContainsTableInspectorAndRefreshesItWithSelectionChanges() throws IOException {
        String pane = Files.readString(INSPECTOR_PANE);

        assertTrue(pane.contains("new WriterTableInspector(editor, commandRegistry)"));
        assertTrue(pane.contains("tableInspector.refresh()"));
        assertTrue(pane.contains("setInspectorVisible(textFormatInspector, !hasImage && !hasTable)"));
        assertTrue(pane.contains("setInspectorVisible(tableInspector, hasTable)"));
    }

    @Test
    void tableInspectorExposesOnlyRealV1TableOperations() throws IOException {
        String source = Files.readString(TABLE_INSPECTOR);

        assertTrue(source.contains("editor.selectedTableBlock()"));
        assertTrue(source.contains("editor.hasSelectedTable()"));
        assertTrue(source.contains("table.insertRowAbove"));
        assertTrue(source.contains("table.insertRowBelow"));
        assertTrue(source.contains("table.deleteRow"));
        assertTrue(source.contains("table.insertColumnLeft"));
        assertTrue(source.contains("table.insertColumnRight"));
        assertTrue(source.contains("table.deleteColumn"));
        assertTrue(source.contains("editor.setSelectedTableHeaderRow"));
        assertTrue(source.contains("editor.updateSelectedTableProperties"));

        assertFalse(source.contains("table.merge"), "merged-cell commands do not belong in Table Inspector v1");
        assertFalse(source.contains("Merge cells"), "merged-cell UI does not belong in Table Inspector v1");
    }
}
