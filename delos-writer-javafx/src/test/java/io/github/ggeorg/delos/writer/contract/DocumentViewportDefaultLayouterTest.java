package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.session.EditorSession;
import io.github.ggeorg.delos.writer.layout.KnuthPlassParagraphLayouter;
import io.github.ggeorg.delos.writer.layout.PaginatingDocumentLayoutEngine;
import io.github.ggeorg.delos.writer.ui.DocumentViewport;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DocumentViewportDefaultLayouterTest extends JavaFxTestSupport {
    @Test
    void defaultViewportUsesKnuthPlassParagraphLayouter() {
        DocumentViewport viewport = onFxThread(() -> new DocumentViewport(new EditorSession(Document.sample())));

        Object layoutEngine = readField(viewport, "layoutEngine");
        assertInstanceOf(PaginatingDocumentLayoutEngine.class, layoutEngine);

        Object paragraphLayouter = readField(layoutEngine, "paragraphLayouter");
        assertInstanceOf(KnuthPlassParagraphLayouter.class, paragraphLayouter);
    }

    private static Object readField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to inspect field '" + fieldName + "' on " + target.getClass().getName(), e);
        }
    }
}
