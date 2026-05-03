package io.github.ggeorg.delos.writer.app;

import io.github.ggeorg.delos.writer.session.EditorSession;
import io.github.ggeorg.delos.writer.ui.control.DesktopPreviewLayoutPolicy;
import io.github.ggeorg.delos.writer.ui.control.WriterDesktopPreviewFactory;
import io.github.ggeorg.delos.writer.ui.control.WriterDocumentView;

import java.io.IOException;
import java.util.Objects;

/**
 * Owns the desktop Writer output-preview wiring.
 *
 * <p>The app shell should compose chrome and commands. It should not import
 * JavaFX renderer internals or PDF renderer internals. Desktop editing uses an
 * explicit {@link DesktopPreviewLayoutPolicy} for native on-screen text quality.
 * Export and print get WYSIWYG behavior by consuming the frozen layout snapshot
 * from that visible preview.</p>
 */
final class WriterOutputPreview implements AutoCloseable {
    private final DesktopPreviewLayoutPolicy previewPolicy;
    private final WriterDesktopPreviewFactory previewFactory;

    static WriterOutputPreview createDefault() {
        return new WriterOutputPreview(
                DesktopPreviewLayoutPolicy.defaultPolicy(),
                new WriterDesktopPreviewFactory()
        );
    }

    WriterOutputPreview(
            DesktopPreviewLayoutPolicy previewPolicy,
            WriterDesktopPreviewFactory previewFactory
    ) {
        this.previewPolicy = Objects.requireNonNull(previewPolicy, "previewPolicy");
        this.previewFactory = Objects.requireNonNull(previewFactory, "previewFactory");
    }

    WriterDocumentView createDocumentView(EditorSession session) {
        Objects.requireNonNull(session, "session");
        return previewFactory.createDocumentView(session, previewPolicy);
    }

    @Override
    public void close() throws IOException {
        // JavaFX-backed preview measurement has no external resources to close.
    }
}
