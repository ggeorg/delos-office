package io.github.ggeorg.delos.javafx.inspector;

import io.github.ggeorg.delos.javafx.JavaFxTestSupport;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DelosInspectorContractTest extends JavaFxTestSupport {
    @Test
    void sharedJavaFxModuleProvidesReusableInspectorShell() {
        onFxThread(() -> {
            DelosInspector inspector = new DelosInspector();
            VBox document = new VBox(new Label("Document settings"));
            VBox format = new VBox(new Label("Text formatting"));

            inspector.addTab("document", "Document", document);
            inspector.addTab("format", "Format", format);

            assertEquals(DelosInspector.DEFAULT_WIDTH, inspector.getMinWidth());
            assertEquals(DelosInspector.DEFAULT_WIDTH, inspector.getPrefWidth());
            assertEquals(DelosInspector.DEFAULT_WIDTH, inspector.getMaxWidth());
            assertTrue(inspector.getStyleClass().contains("delos-inspector"));
            assertEquals(2, inspector.tabCount());
            assertEquals(List.of("document", "format"), inspector.tabIds());
            assertTrue(inspector.hasTab("document"));
            assertTrue(inspector.hasTab("format"));
            assertFalse(inspector.hasTab("arrange"));
            assertEquals("document", inspector.selectedTabId());
            assertSame(document, inspector.selectedContent());
            assertSame(document, inspector.contentForTab("document"));

            inspector.selectTab("format");

            assertEquals("format", inspector.selectedTabId());
            assertSame(format, inspector.selectedContent());
            assertSame(format, inspector.contentForTab("format"));
        });
    }

    @Test
    void rejectsUnknownOrDuplicateTabs() {
        onFxThread(() -> {
            DelosInspector inspector = new DelosInspector();
            inspector.addTab("document", "Document", new VBox());

            assertThrows(IllegalArgumentException.class, () -> inspector.addTab("document", "Again", new VBox()));
            assertThrows(IllegalArgumentException.class, () -> inspector.addTab(" ", "Blank", new VBox()));
            assertThrows(IllegalArgumentException.class, () -> inspector.selectTab("missing"));
            assertThrows(IllegalArgumentException.class, () -> inspector.contentForTab("missing"));
        });
    }
}
