package io.github.ggeorg.delos.writer.ui.control;

import io.github.ggeorg.delos.writer.document.Alignment;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.DocumentMediaItem;
import io.github.ggeorg.delos.writer.document.FormulaBlock;
import io.github.ggeorg.delos.writer.document.FormulaSourceFormat;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.document.ListMarkerKind;
import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.document.TableCellSelection;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.editor.TextStyle;
import io.github.ggeorg.delos.writer.layout.CaretGeometry;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.session.EditorSession;
import io.github.ggeorg.delos.writer.ui.DocumentViewport;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Public JavaFX control boundary for the Delos word-processing surface.
 *
 * <p>The control owns the public API and state properties. The skin owns the
 * current viewport implementation, so application code can depend on
 * {@code DelosEditor} instead of constructing or coordinating the viewport
 * directly.</p>
 */
public final class DelosEditor extends Control {
    private final EditorSession session;
    private final ReadOnlyObjectWrapper<Document> document = new ReadOnlyObjectWrapper<>(this, "document");
    private final ReadOnlyBooleanWrapper dirty = new ReadOnlyBooleanWrapper(this, "dirty");
    private final ReadOnlyObjectWrapper<CaretGeometry> caretGeometry = new ReadOnlyObjectWrapper<>(this, "caretGeometry");
    private final ReadOnlyObjectWrapper<TextPosition> caretPosition = new ReadOnlyObjectWrapper<>(this, "caretPosition");
    private final ReadOnlyObjectWrapper<SelectionRange> selectionRange = new ReadOnlyObjectWrapper<>(this, "selectionRange");
    private final ReadOnlyBooleanWrapper hasSelection = new ReadOnlyBooleanWrapper(this, "hasSelection");
    private final ReadOnlyIntegerWrapper currentPageNumber = new ReadOnlyIntegerWrapper(this, "currentPageNumber", 1);
    private final ReadOnlyIntegerWrapper totalPageCount = new ReadOnlyIntegerWrapper(this, "totalPageCount", 1);
    private final DoubleProperty zoom = new SimpleDoubleProperty(this, "zoom", 1.0);
    private final Runnable sessionStateListener = this::refreshSessionState;
    private final ChangeListener<CaretGeometry> caretGeometryListener = (obs, oldValue, newValue) -> refreshViewportState();

    private DocumentViewport viewport;
    private Consumer<Bounds> scrollIntoViewHandler;

    public DelosEditor(EditorSession session) {
        this.session = Objects.requireNonNull(session, "session");
        getStyleClass().add("delos-editor");
        setFocusTraversable(false);
        session.addStateListener(sessionStateListener);
        refreshSessionState();
        setSkin(createDefaultSkin());
    }

    public EditorSession session() {
        return session;
    }

    public ReadOnlyObjectProperty<Document> documentProperty() {
        return document.getReadOnlyProperty();
    }

    public Document document() {
        return document.get();
    }

    public ReadOnlyBooleanProperty dirtyProperty() {
        return dirty.getReadOnlyProperty();
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public ReadOnlyObjectProperty<CaretGeometry> caretGeometryProperty() {
        return caretGeometry.getReadOnlyProperty();
    }

    public CaretGeometry caretGeometry() {
        return caretGeometry.get();
    }

    public ReadOnlyObjectProperty<TextPosition> caretPositionProperty() {
        return caretPosition.getReadOnlyProperty();
    }

    public TextPosition caretPosition() {
        return caretPosition.get();
    }

    public ReadOnlyObjectProperty<SelectionRange> selectionRangeProperty() {
        return selectionRange.getReadOnlyProperty();
    }

    public SelectionRange selectionRange() {
        return selectionRange.get();
    }

    public ReadOnlyBooleanProperty hasSelectionProperty() {
        return hasSelection.getReadOnlyProperty();
    }

    public boolean hasSelection() {
        return hasSelection.get();
    }

    public ReadOnlyIntegerProperty currentPageNumberProperty() {
        return currentPageNumber.getReadOnlyProperty();
    }

    public int currentPageNumber() {
        return currentPageNumber.get();
    }

    public ReadOnlyIntegerProperty totalPageCountProperty() {
        return totalPageCount.getReadOnlyProperty();
    }

    public int totalPageCount() {
        return totalPageCount.get();
    }

    /**
     * Editor zoom factor. The application chrome may bind to this property, but
     * the control owns it so callers no longer need to know about the current
     * zoom-controller implementation.
     */
    public DoubleProperty zoomProperty() {
        return zoom;
    }

    public double zoom() {
        return zoom.get();
    }

    public void setZoom(double zoom) {
        this.zoom.set(zoom);
    }

    /**
     * Captures the current document and frozen page layout for export/print
     * consumers without exposing {@code DocumentViewport} as public API.
     */
    public WriterLayoutSnapshot createLayoutSnapshot() {
        LaidOutDocument layout = viewport().laidOutDocument();
        if (layout == null) {
            throw new IllegalStateException("DelosEditor has no layout snapshot yet");
        }
        return new WriterLayoutSnapshot(
                document(),
                layout,
                caretPosition(),
                selectionRange(),
                currentPageNumber(),
                totalPageCount()
        );
    }

    public void undo() { viewport().undo(); }
    public void redo() { viewport().redo(); }
    public void copy() { viewport().copy(); }
    public void cut() { viewport().cut(); }
    public void paste() { viewport().paste(); }
    public void selectAll() { viewport().selectAll(); }
    public void reloadDocument() { viewport().reloadDocument(); refreshSessionState(); refreshViewportState(); }
    public void toggleStyle(TextStyle style) { viewport().toggleStyle(style); }
    public void setParagraphAlignment(Alignment alignment) { viewport().setParagraphAlignment(alignment); }
    public void toggleListKind(ListMarkerKind kind) { viewport().toggleListKind(kind); }
    public void increaseListLevel() { viewport().increaseListLevel(); }
    public void decreaseListLevel() { viewport().decreaseListLevel(); }
    public void insertImage(DocumentMediaItem mediaItem, double width, double height, String altText) { viewport().insertImage(mediaItem, width, height, altText); }
    public void insertTable(int rows, int columns) { viewport().insertTable(rows, columns); }
    public TableCellSelection activeTableCellSelection() { return viewport().activeTableCellSelection(); }
    public TableBlock selectedTableBlock() { return viewport().selectedTableBlock(); }
    public boolean hasSelectedTable() { return viewport().hasSelectedTable(); }
    public void insertTableRowAbove() { viewport().insertTableRowAbove(); }
    public void insertTableRowBelow() { viewport().insertTableRowBelow(); }
    public void deleteTableRow() { viewport().deleteTableRow(); }
    public void insertTableColumnLeft() { viewport().insertTableColumnLeft(); }
    public void insertTableColumnRight() { viewport().insertTableColumnRight(); }
    public void deleteTableColumn() { viewport().deleteTableColumn(); }
    public void setSelectedTableHeaderRow(boolean enabled) { viewport().setSelectedTableHeaderRow(enabled); }
    public void updateSelectedTableProperties(double widthFraction, double cellPadding, boolean bordersEnabled) { viewport().updateSelectedTableProperties(widthFraction, cellPadding, bordersEnabled); }
    public void insertFormula(FormulaSourceFormat sourceFormat, String source, String altText) { viewport().insertFormula(sourceFormat, source, altText); }
    public FormulaBlock selectedFormulaBlock() { return viewport().selectedFormulaBlock(); }
    public boolean hasSelectedFormulaBlock() { return viewport().hasSelectedFormulaBlock(); }
    public void updateSelectedFormula(FormulaSourceFormat sourceFormat, String source, String altText) { viewport().updateSelectedFormula(sourceFormat, source, altText); }
    public ImageBlock selectedImageBlock() { return viewport().selectedImageBlock(); }
    public boolean hasSelectedImageBlock() { return viewport().hasSelectedImageBlock(); }
    public void updateSelectedImageProperties(double width, double height, String altText) { viewport().updateSelectedImageProperties(width, height, altText); }

    public boolean isTextStyleActive(TextStyle style) {
        Objects.requireNonNull(style, "style");
        Document current = document();
        TextPosition position = caretPosition();
        if (current == null || position == null || current.paragraphs().isEmpty()) {
            return false;
        }
        int paragraphIndex = Math.max(0, Math.min(position.paragraphIndex(), current.paragraphs().size() - 1));
        var paragraph = current.paragraphs().get(paragraphIndex);
        int targetOffset = Math.max(0, Math.min(position.offset(), paragraph.length()));
        if (targetOffset > 0) {
            targetOffset--;
        }
        int runOffset = 0;
        for (var run : paragraph.runs()) {
            int runEnd = runOffset + run.text().length();
            if (targetOffset >= runOffset && targetOffset < runEnd) {
                return switch (style) {
                    case BOLD -> run.bold();
                    case ITALIC -> run.italic();
                    case UNDERLINE -> run.underline();
                    case STRIKETHROUGH -> run.strikethrough();
                };
            }
            runOffset = runEnd;
        }
        return false;
    }

    public boolean isParagraphAlignmentActive(Alignment alignment) {
        Objects.requireNonNull(alignment, "alignment");
        Document current = document();
        TextPosition position = caretPosition();
        if (current == null || position == null || current.paragraphs().isEmpty()) {
            return false;
        }
        int paragraphIndex = Math.max(0, Math.min(position.paragraphIndex(), current.paragraphs().size() - 1));
        return current.paragraphs().get(paragraphIndex).style().alignment() == alignment;
    }

    public boolean isListKindActive(ListMarkerKind kind) {
        Objects.requireNonNull(kind, "kind");
        return currentListKind() == kind;
    }

    public boolean canIncreaseListLevel() {
        var listStyle = currentListStyle();
        return listStyle != null && listStyle.enabled() && listStyle.level() < 8;
    }

    public boolean canDecreaseListLevel() {
        var listStyle = currentListStyle();
        return listStyle != null && listStyle.enabled();
    }

    private ListMarkerKind currentListKind() {
        var listStyle = currentListStyle();
        return listStyle == null ? ListMarkerKind.NONE : listStyle.kind();
    }

    private io.github.ggeorg.delos.writer.document.ParagraphListStyle currentListStyle() {
        Document current = document();
        TextPosition position = caretPosition();
        if (current == null || position == null || current.paragraphs().isEmpty()) {
            return null;
        }
        int paragraphIndex = Math.max(0, Math.min(position.paragraphIndex(), current.paragraphs().size() - 1));
        return current.paragraphs().get(paragraphIndex).style().listStyle();
    }

    public void setScrollIntoViewHandler(Consumer<Bounds> scrollIntoViewHandler) {
        this.scrollIntoViewHandler = scrollIntoViewHandler;
        if (viewport != null) {
            viewport.setScrollIntoViewHandler(scrollIntoViewHandler);
        }
    }

    public double scrollableContentWidth() {
        return viewport == null ? 0.0 : viewport.scrollableContentWidth();
    }

    public double scrollableContentHeight() {
        return viewport == null ? 0.0 : viewport.scrollableContentHeight();
    }

    public void setVisibleViewport(Bounds visibleViewportInContent) {
        if (viewport != null) {
            viewport.setVisibleViewport(visibleViewportInContent);
        }
    }

    /** Gives focus to the real editing surface while the viewport remains the internal implementation. */
    public void focusEditor() {
        viewport().requestFocus();
    }

    void attachViewport(DocumentViewport viewport) {
        if (this.viewport == viewport) {
            return;
        }
        if (this.viewport != null) {
            detachViewport(this.viewport);
        }
        this.viewport = Objects.requireNonNull(viewport, "viewport");
        this.viewport.setScrollIntoViewHandler(scrollIntoViewHandler);
        this.viewport.caretGeometryProperty().addListener(caretGeometryListener);
        refreshViewportState();
    }

    void detachViewport(DocumentViewport viewport) {
        if (this.viewport != viewport) {
            return;
        }
        this.viewport.caretGeometryProperty().removeListener(caretGeometryListener);
        this.viewport.setScrollIntoViewHandler(null);
        this.viewport = null;
        refreshViewportState();
    }

    private DocumentViewport viewport() {
        if (viewport == null) {
            throw new IllegalStateException("DelosEditor skin has not installed the document viewport");
        }
        return viewport;
    }

    private void refreshSessionState() {
        document.set(session.document());
        dirty.set(session.isDirty());
    }

    private void refreshViewportState() {
        if (viewport == null) {
            caretGeometry.set(null);
            caretPosition.set(null);
            selectionRange.set(null);
            hasSelection.set(false);
            currentPageNumber.set(1);
            totalPageCount.set(1);
            return;
        }
        caretGeometry.set(viewport.caretGeometry());
        caretPosition.set(viewport.caretPosition());
        selectionRange.set(viewport.selectionRange());
        hasSelection.set(viewport.hasSelection());
        currentPageNumber.set(viewport.currentPageNumber());
        totalPageCount.set(viewport.totalPageCount());
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new DelosEditorSkin(this);
    }
}
