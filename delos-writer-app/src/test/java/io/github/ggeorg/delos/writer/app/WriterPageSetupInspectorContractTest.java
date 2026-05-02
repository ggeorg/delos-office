package io.github.ggeorg.delos.writer.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WriterPageSetupInspectorContractTest {
    private static final Path INSPECTOR_PANE = Path.of(
            "src/main/java/io/github/ggeorg/delos/writer/app/inspector/WriterInspectorPane.java"
    );
    private static final Path PAGE_SETUP_INSPECTOR = Path.of(
            "src/main/java/io/github/ggeorg/delos/writer/app/inspector/WriterPageSetupInspector.java"
    );

    @Test
    void documentInspectorUsesEditableUnitAwarePageSetupControls() throws IOException {
        String pane = Files.readString(INSPECTOR_PANE);
        String source = Files.readString(PAGE_SETUP_INSPECTOR);

        assertTrue(pane.contains("new WriterPageSetupInspector(session, editor)"));
        assertTrue(pane.contains("pageSetupInspector.refresh()"));
        assertTrue(source.contains("ComboBox<PaperPreset> paper = new ComboBox<>()"));
        assertTrue(source.contains("paper.setItems(FXCollections.observableArrayList(PaperPreset.values()))"));
        assertTrue(source.contains("US_LETTER(\"US Letter\""));
        assertTrue(source.contains("US_LEGAL(\"US Legal\""));
        assertTrue(source.contains("ENVELOPE_DL(\"Envelope DL\""));
        assertTrue(source.contains("new ToggleButton(\"Portrait\")"));
        assertTrue(source.contains("new ToggleButton(\"Landscape\")"));
        assertTrue(source.contains("SpinnerValueFactory.DoubleSpinnerValueFactory(MIN_MARGIN_CM, MAX_MARGIN_CM"));
        assertTrue(source.contains("Margins are shown in centimeters"));
        assertTrue(source.contains("cmToPoints(spinnerValueCm"));
        assertTrue(source.contains("pointsToCm(pageStyle.marginTop())"));
        assertTrue(source.contains("parseLengthAsCm"));
        assertTrue(source.contains("session.execute(new EditCommand"));
        assertTrue(source.contains("editor.reloadDocument()"));

        assertFalse(source.contains("valueLabel()"), "Document inspector must not regress to read-only value labels");
    }
}
