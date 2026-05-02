package io.github.ggeorg.delos.writer.ui.control;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;

import java.util.Objects;

/**
 * Immutable control-level snapshot of the editable Writer surface.
 *
 * <p>The snapshot is the seam between the live JavaFX editor and output
 * backends such as PDF export/printing. Consumers get the current document,
 * the latest frozen page layout, and the visible editor state without reaching
 * into {@code DocumentViewport}.</p>
 */
public record WriterLayoutSnapshot(
        Document document,
        LaidOutDocument layout,
        TextPosition caretPosition,
        SelectionRange selectionRange,
        int currentPageNumber,
        int totalPageCount
) {
    public WriterLayoutSnapshot {
        document = Objects.requireNonNull(document, "document");
        layout = Objects.requireNonNull(layout, "layout");
        if (currentPageNumber < 1) {
            throw new IllegalArgumentException("currentPageNumber must be >= 1");
        }
        if (totalPageCount < 1) {
            throw new IllegalArgumentException("totalPageCount must be >= 1");
        }
    }
}
