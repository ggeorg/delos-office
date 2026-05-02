package io.github.ggeorg.delos.writer.ui;

import io.github.ggeorg.delos.writer.contract.JavaFxTestSupport;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.layout.CaretGeometry;
import io.github.ggeorg.delos.writer.session.EditorSession;
import javafx.geometry.Bounds;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DocumentViewportCaretScrollRegressionTest extends JavaFxTestSupport {
    @Test
    void typingSchedulesOneModelDerivedCaretScrollAfterTheEdit() {
        EditorSession session = new EditorSession(Document.sample());
        List<Bounds> scrollRequests = new ArrayList<>();

        DocumentViewport viewport = onFxThread(() -> {
            DocumentViewport created = new DocumentViewport(session);
            created.setScrollIntoViewHandler(scrollRequests::add);
            created.fireEvent(keyTyped("x"));
            return created;
        });
        drainFxEvents();

        assertEquals(new TextPosition(0, 1), viewport.caretPosition());
        assertFalse(scrollRequests.isEmpty(), "typing must request a caret scroll after layout rebuild");
        assertEquals(1, scrollRequests.size(), "typing should use the simple pending debounce");
    }

    @Test
    void repeatedEnterDebouncesToFinalModelDerivedCaretScroll() {
        EditorSession session = new EditorSession(Document.sample());
        int originalParagraphCount = session.document().paragraphs().size();
        List<Bounds> scrollRequests = new ArrayList<>();

        DocumentViewport viewport = onFxThread(() -> {
            DocumentViewport created = new DocumentViewport(session);
            created.setScrollIntoViewHandler(scrollRequests::add);
            created.fireEvent(keyPressed(KeyCode.ENTER));
            created.fireEvent(keyPressed(KeyCode.ENTER));
            created.fireEvent(keyPressed(KeyCode.ENTER));
            return created;
        });
        drainFxEvents();

        assertEquals(originalParagraphCount + 3, session.document().paragraphs().size());
        assertEquals(new TextPosition(3, 0), viewport.caretPosition());
        assertFalse(scrollRequests.isEmpty(), "repeated Enter must request a caret scroll after all layout rebuilds");
        assertEquals(1, scrollRequests.size(), "repeated Enter should collapse into one pending caret scroll");

        assertScrollTargetMatchesCaret(viewport, scrollRequests.getFirst());
    }

    @Test
    void typingUndoRedoDebouncesToFinalModelDerivedCaretScroll() {
        EditorSession session = new EditorSession(Document.sample());
        List<Bounds> scrollRequests = new ArrayList<>();

        DocumentViewport viewport = onFxThread(() -> {
            DocumentViewport created = new DocumentViewport(session);
            created.setScrollIntoViewHandler(scrollRequests::add);
            created.fireEvent(keyTyped("x"));
            created.undo();
            created.redo();
            return created;
        });
        drainFxEvents();

        assertEquals(new TextPosition(0, 1), viewport.caretPosition());
        assertFalse(scrollRequests.isEmpty(), "undo/redo burst must still request a caret scroll after layout rebuild");
        assertEquals(1, scrollRequests.size(), "undo/redo should share the same pending caret scroll debounce");
        assertScrollTargetMatchesCaret(viewport, scrollRequests.getFirst());
    }

    private static void assertScrollTargetMatchesCaret(DocumentViewport viewport, Bounds scrollTarget) {
        CaretGeometry finalCaret = viewport.caretGeometry();
        assertNotNull(finalCaret, "final caret geometry must be available after layout rebuild");

        ViewTheme theme = ViewTheme.defaultTheme();
        assertEquals(theme.shadowExtentX() + finalCaret.x(), scrollTarget.getMinX(), 0.0001);
        assertEquals(theme.outerPadding() + theme.shadowExtentY() + finalCaret.y(), scrollTarget.getMinY(), 0.0001);
        assertEquals(finalCaret.height(), scrollTarget.getHeight(), 0.0001);
    }

    private static void drainFxEvents() {
        onFxThread(() -> { });
    }

    private static KeyEvent keyTyped(String character) {
        return new KeyEvent(
                KeyEvent.KEY_TYPED,
                character,
                character,
                KeyCode.UNDEFINED,
                false,
                false,
                false,
                false
        );
    }

    private static KeyEvent keyPressed(KeyCode code) {
        return new KeyEvent(
                KeyEvent.KEY_PRESSED,
                "",
                "",
                code,
                false,
                false,
                false,
                false
        );
    }
}
