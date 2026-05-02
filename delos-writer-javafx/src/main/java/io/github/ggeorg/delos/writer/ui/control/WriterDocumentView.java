package io.github.ggeorg.delos.writer.ui.control;

import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.session.EditorSession;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.writer.layout.DocumentLayoutEngine;
import io.github.ggeorg.delos.writer.render.DefaultPageRenderer;
import io.github.ggeorg.delos.writer.render.PageRenderer;
import io.github.ggeorg.delos.writer.ui.ViewTheme;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;

import java.util.Objects;

/**
 * Reusable word-processor document workspace control.
 *
 * <p>{@link DelosEditor} remains the focused editing surface. This higher-level
 * control composes the editor with document-workspace chrome such as rulers,
 * zoomed scrolling, and future page-view modes. Application shells should use
 * this control instead of wiring scroll panes, zoom hosts, and rulers directly.</p>
 */
public final class WriterDocumentView extends Control {
    private final EditorSession session;
    private final DelosEditor editor;
    private final ReadOnlyObjectWrapper<DelosEditor> editorRef = new ReadOnlyObjectWrapper<>(this, "editor");
    private final BooleanProperty horizontalRulerVisible = new SimpleBooleanProperty(this, "horizontalRulerVisible", true);
    private final BooleanProperty verticalRulerVisible = new SimpleBooleanProperty(this, "verticalRulerVisible", true);
    private final ObjectProperty<PageViewMode> pageViewMode = new SimpleObjectProperty<>(this, "pageViewMode", PageViewMode.CONTINUOUS);
    private final ReadOnlyDoubleWrapper visibleContentX = new ReadOnlyDoubleWrapper(this, "visibleContentX", 0.0);
    private final ReadOnlyDoubleWrapper visibleContentY = new ReadOnlyDoubleWrapper(this, "visibleContentY", 0.0);
    private final ReadOnlyDoubleWrapper viewportWidth = new ReadOnlyDoubleWrapper(this, "viewportWidth", 0.0);
    private final ReadOnlyDoubleWrapper viewportHeight = new ReadOnlyDoubleWrapper(this, "viewportHeight", 0.0);

    private WriterDocumentViewZoomController zoomController;

    public WriterDocumentView(EditorSession session) {
        this(session, null);
    }

    private WriterDocumentView(EditorSession session, DelosEditor editor) {
        this.session = Objects.requireNonNull(session, "session");
        this.editor = editor == null ? new DelosEditor(session) : editor;
        this.editorRef.set(this.editor);
        getStyleClass().add("writer-document-view");
        setFocusTraversable(false);
        setSkin(createDefaultSkin());
    }

    public WriterDocumentView(EditorSession session, ViewTheme viewTheme, DocumentLayoutEngine layoutEngine, PageRenderer pageRenderer) {
        this(session, new DelosEditor(session, viewTheme, layoutEngine, pageRenderer));
    }

    public WriterDocumentView(
            EditorSession session,
            ViewTheme viewTheme,
            DocumentLayoutEngine layoutEngine,
            RenderTextMeasurer renderTextMeasurer
    ) {
        this(session, viewTheme, layoutEngine, new DefaultPageRenderer(), renderTextMeasurer);
    }

    public WriterDocumentView(
            EditorSession session,
            ViewTheme viewTheme,
            DocumentLayoutEngine layoutEngine,
            PageRenderer pageRenderer,
            RenderTextMeasurer renderTextMeasurer
    ) {
        this(session, new DelosEditor(session, viewTheme, layoutEngine, pageRenderer, renderTextMeasurer));
    }

    public EditorSession session() {
        return session;
    }

    public ReadOnlyObjectProperty<DelosEditor> editorProperty() {
        return editorRef.getReadOnlyProperty();
    }

    public DelosEditor editor() {
        return editor;
    }

    public ReadOnlyObjectProperty<Document> documentProperty() {
        return editor.documentProperty();
    }

    public Document document() {
        return editor.document();
    }

    public DoubleProperty zoomFactorProperty() {
        return editor.zoomProperty();
    }

    public double zoomFactor() {
        return editor.zoom();
    }

    public void setZoomFactor(double factor) {
        if (zoomController == null) {
            editor.setZoom(factor);
            return;
        }
        zoomController.setZoomFactor(factor);
    }

    public void zoomToFitWidth() {
        if (zoomController != null) {
            zoomController.zoomToFitWidth();
        }
    }

    public boolean isFitWidthMode() {
        return zoomController != null && zoomController.isFitWidthMode();
    }

    public BooleanProperty horizontalRulerVisibleProperty() {
        return horizontalRulerVisible;
    }

    public boolean isHorizontalRulerVisible() {
        return horizontalRulerVisible.get();
    }

    public void setHorizontalRulerVisible(boolean visible) {
        horizontalRulerVisible.set(visible);
    }

    public BooleanProperty verticalRulerVisibleProperty() {
        return verticalRulerVisible;
    }

    public boolean isVerticalRulerVisible() {
        return verticalRulerVisible.get();
    }

    public void setVerticalRulerVisible(boolean visible) {
        verticalRulerVisible.set(visible);
    }


    public boolean areRulersVisible() {
        return isHorizontalRulerVisible() || isVerticalRulerVisible();
    }

    public void setRulersVisible(boolean visible) {
        setHorizontalRulerVisible(visible);
        setVerticalRulerVisible(visible);
    }

    public ObjectProperty<PageViewMode> pageViewModeProperty() {
        return pageViewMode;
    }

    public PageViewMode pageViewMode() {
        return pageViewMode.get();
    }

    public void setPageViewMode(PageViewMode mode) {
        pageViewMode.set(Objects.requireNonNull(mode, "mode"));
    }

    public ReadOnlyDoubleProperty visibleContentXProperty() {
        return visibleContentX.getReadOnlyProperty();
    }

    public ReadOnlyDoubleProperty visibleContentYProperty() {
        return visibleContentY.getReadOnlyProperty();
    }

    public ReadOnlyDoubleProperty viewportWidthProperty() {
        return viewportWidth.getReadOnlyProperty();
    }

    public ReadOnlyDoubleProperty viewportHeightProperty() {
        return viewportHeight.getReadOnlyProperty();
    }

    /**
     * Captures the current frozen Writer layout for output backends without
     * exposing the lower-level editor viewport implementation.
     */
    public WriterLayoutSnapshot createLayoutSnapshot() {
        return editor.createLayoutSnapshot();
    }

    public void focusEditor() {
        editor.focusEditor();
    }

    public void ensureVisible(Bounds bounds) {
        if (zoomController != null) {
            zoomController.ensureVisible(bounds);
        }
    }

    void attachZoomController(WriterDocumentViewZoomController controller) {
        this.zoomController = Objects.requireNonNull(controller, "controller");
    }

    void detachZoomController(WriterDocumentViewZoomController controller) {
        if (this.zoomController == controller) {
            this.zoomController = null;
        }
    }

    void updateVisibleViewport(double x, double y, double width, double height) {
        visibleContentX.set(x);
        visibleContentY.set(y);
        viewportWidth.set(width);
        viewportHeight.set(height);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new WriterDocumentViewSkin(this);
    }
}
