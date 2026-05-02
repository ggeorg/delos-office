package io.github.ggeorg.delos.writer.ui.control;

import io.github.ggeorg.delos.writer.session.EditorSession;
import io.github.ggeorg.delos.writer.ui.ViewTheme;

import java.util.Objects;

/**
 * Creates the high-quality JavaFX Writer preview used by the desktop editor.
 *
 * <p>This class is the exported boundary for desktop preview wiring. It keeps
 * JavaFX implementation details such as {@code JavaFxRenderTextMeasurer} inside
 * the {@code delos-writer-javafx} module, while application shells only depend
 * on stable UI controls and an explicit {@link DesktopPreviewLayoutPolicy}.</p>
 */
public final class WriterDesktopPreviewFactory {
    public WriterDocumentView createDocumentView(EditorSession session, DesktopPreviewLayoutPolicy policy) {
        Objects.requireNonNull(policy, "policy");
        return policy.createDocumentView(session, ViewTheme.defaultTheme());
    }

    public WriterDocumentView createDocumentView(EditorSession session, ViewTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return DesktopPreviewLayoutPolicy.fromLayoutTheme(theme.layoutTheme()).createDocumentView(session, theme);
    }
}
