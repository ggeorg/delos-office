package io.github.ggeorg.delos.writer.session;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.Paragraph;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorSessionStateTest {

    @Test
    void dirtyStateTracksEditsUndoAndMarkClean() {
        Document clean = Document.blank();
        Document edited = new Document(clean.title(), clean.pageStyle(), List.of(Paragraph.of("Edited")));

        EditorSession session = new EditorSession(clean);
        AtomicInteger notifications = new AtomicInteger();
        session.addStateListener(notifications::incrementAndGet);

        session.execute(new UndoableCommand() {
            @Override
            public String description() {
                return "Edit";
            }

            @Override
            public void execute() {
                session.setDocument(edited);
            }

            @Override
            public void undo() {
                session.setDocument(clean);
            }
        });

        assertTrue(session.isDirty());
        assertTrue(session.canUndo());

        session.undo();
        assertFalse(session.isDirty());
        assertTrue(session.canRedo());

        session.redo();
        assertTrue(session.isDirty());

        session.markClean();
        assertFalse(session.isDirty());
        assertEquals(4, notifications.get());
    }

    @Test
    void loadDocumentResetsHistoryAndDirtyState() {
        EditorSession session = new EditorSession(Document.blank());
        session.execute(new UndoableCommand() {
            @Override
            public String description() {
                return "Edit";
            }

            @Override
            public void execute() {
                session.setDocument(new Document("Edited", session.document().pageStyle(), List.of(Paragraph.of("Edited"))));
            }

            @Override
            public void undo() {
                session.setDocument(Document.blank());
            }
        });

        assertTrue(session.isDirty());
        assertTrue(session.canUndo());

        Document loaded = new Document("Loaded", session.document().pageStyle(), List.of(Paragraph.of("Loaded")));
        session.loadDocument(loaded);

        assertEquals(loaded, session.document());
        assertFalse(session.isDirty());
        assertFalse(session.canUndo());
        assertFalse(session.canRedo());
    }
}
