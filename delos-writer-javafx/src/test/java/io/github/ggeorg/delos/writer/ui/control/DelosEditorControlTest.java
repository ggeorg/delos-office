package io.github.ggeorg.delos.writer.ui.control;

import io.github.ggeorg.delos.writer.contract.JavaFxTestSupport;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.FormulaBlock;
import io.github.ggeorg.delos.writer.document.FormulaSourceFormat;
import io.github.ggeorg.delos.writer.session.EditorSession;
import javafx.scene.control.Control;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DelosEditorControlTest extends JavaFxTestSupport {

    @Test
    void editorIsARealJavaFxControlBoundary() {
        DelosEditor editor = onFxThread(() -> new DelosEditor(new EditorSession(Document.sample())));

        assertInstanceOf(Control.class, editor);
        assertInstanceOf(DelosEditorSkin.class, onFxThread(editor::getSkin));
        assertTrue(onFxThread(() -> editor.getStyleClass().contains("delos-editor")));
        assertFalse(onFxThread(editor::isFocusTraversable));
        assertEquals(1, onFxThread(editor::currentPageNumber));
        assertTrue(onFxThread(() -> editor.totalPageCount() >= 1));
    }

    @Test
    void exposesControlLevelStateProperties() {
        EditorSession session = new EditorSession(Document.sample());
        DelosEditor editor = onFxThread(() -> new DelosEditor(session));

        assertSame(session.document(), onFxThread(editor::document));
        assertFalse(onFxThread(editor::isDirty));
        assertNotNull(onFxThread(() -> editor.documentProperty().get()));
        assertEquals(onFxThread(editor::currentPageNumber), onFxThread(() -> editor.currentPageNumberProperty().get()));
        assertEquals(onFxThread(editor::totalPageCount), onFxThread(() -> editor.totalPageCountProperty().get()));
        assertEquals(onFxThread(editor::hasSelection), onFxThread(() -> editor.hasSelectionProperty().get()));
    }

    @Test
    void exposesZoomAsControlOwnedState() {
        DelosEditor editor = onFxThread(() -> new DelosEditor(new EditorSession(Document.sample())));

        assertEquals(1.0, onFxThread(editor::zoom), 0.001);
        onFxThread(() -> editor.setZoom(1.25));

        assertEquals(1.25, onFxThread(editor::zoom), 0.001);
        assertEquals(1.25, onFxThread(() -> editor.zoomProperty().get()), 0.001);
    }

    @Test
    void exposesFormulaInsertionAsControlApi() {
        EditorSession session = new EditorSession(Document.sample());
        DelosEditor editor = onFxThread(() -> new DelosEditor(session));

        onFxThread(() -> editor.insertFormula(FormulaSourceFormat.LATEX, "E = mc^2", "mass energy equation"));

        assertTrue(session.document().blocks().stream().anyMatch(FormulaBlock.class::isInstance));
    }

    @Test
    void createsLayoutSnapshotWithoutExposingViewport() {
        EditorSession session = new EditorSession(Document.sample());
        DelosEditor editor = onFxThread(() -> new DelosEditor(session));

        WriterLayoutSnapshot snapshot = onFxThread(editor::createLayoutSnapshot);

        assertSame(session.document(), snapshot.document());
        assertNotNull(snapshot.layout());
        assertTrue(snapshot.totalPageCount() >= 1);
        assertEquals(onFxThread(editor::currentPageNumber), snapshot.currentPageNumber());
        assertEquals(onFxThread(editor::totalPageCount), snapshot.totalPageCount());
    }
}
