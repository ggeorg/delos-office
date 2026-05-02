package io.github.ggeorg.delos.writer.ui.control;

import io.github.ggeorg.delos.javafx.ZoomViewportHost;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.ui.ViewTheme;
import io.github.ggeorg.delos.writer.ui.ruler.CornerRulerCell;
import io.github.ggeorg.delos.writer.ui.ruler.HorizontalRuler;
import io.github.ggeorg.delos.writer.ui.ruler.VerticalRuler;
import javafx.beans.value.ChangeListener;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SkinBase;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Skin that composes the editor viewport with optional rulers. */
public final class WriterDocumentViewSkin extends SkinBase<WriterDocumentView> {
    private final ViewTheme theme;
    private final DelosEditor editor;
    private final HorizontalRuler horizontalRuler = new HorizontalRuler();
    private final VerticalRuler verticalRuler = new VerticalRuler();
    private final CornerRulerCell corner = new CornerRulerCell();
    private final Group zoomGroup;
    private final ZoomViewportHost zoomHost;
    private final ScrollPane scrollPane;
    private final WriterDocumentViewZoomController zoomController;
    private final HBox rulerRow;
    private final HBox editorRow;
    private final VBox root;
    private final ChangeListener<Document> documentListener = (obs, oldValue, newValue) -> updateRulerPageStyle(newValue);

    WriterDocumentViewSkin(WriterDocumentView control) {
        super(control);
        this.editor = control.editor();
        this.theme = editor.viewTheme();
        this.zoomGroup = new Group(editor);
        this.zoomHost = new ZoomViewportHost(zoomGroup);
        this.scrollPane = new ScrollPane(zoomHost);
        this.zoomController = new WriterDocumentViewZoomController(control, scrollPane, zoomHost);

        control.attachZoomController(zoomController);
        editor.setScrollIntoViewHandler(zoomController::ensureVisible);
        zoomGroup.getTransforms().add(zoomController.zoomScale());

        configureScrollPane();
        configureRulers(control);

        rulerRow = new HBox(corner, horizontalRuler);
        rulerRow.getStyleClass().add("writer-document-ruler-row");
        HBox.setHgrow(horizontalRuler, Priority.ALWAYS);

        editorRow = new HBox(verticalRuler, scrollPane);
        editorRow.getStyleClass().add("writer-document-editor-row");
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        VBox.setVgrow(editorRow, Priority.ALWAYS);

        root = new VBox(rulerRow, editorRow);
        root.getStyleClass().add("writer-document-view-skin");
        getChildren().add(root);

        control.documentProperty().addListener(documentListener);
        updateRulerPageStyle(control.document());
        updateRulerVisibility(control);
    }

    @Override
    public void dispose() {
        WriterDocumentView control = getSkinnable();
        control.documentProperty().removeListener(documentListener);
        control.detachZoomController(zoomController);
        editor.setScrollIntoViewHandler(null);
        super.dispose();
    }

    @Override
    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return leftInset + root.prefWidth(height) + rightInset;
    }

    @Override
    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return topInset + root.prefHeight(width) + bottomInset;
    }

    @Override
    protected void layoutChildren(double contentX, double contentY, double contentWidth, double contentHeight) {
        root.resizeRelocate(contentX, contentY, contentWidth, contentHeight);
    }

    private void configureScrollPane() {
        scrollPane.setFitToWidth(false);
        // Delos Writer is an editing surface, not a map/canvas. Drag-to-pan makes
        // the ScrollPane install extra mouse handling around the viewport and can
        // make the edge scrollbar feel unreliable. Keep normal scrollbar/wheel
        // behavior explicit and predictable.
        scrollPane.setPannable(false);
        scrollPane.getStyleClass().add("editor-scroll");
        scrollPane.addEventFilter(ScrollEvent.SCROLL, zoomController::handleZoomScroll);
    }

    private void configureRulers(WriterDocumentView control) {
        horizontalRuler.zoomFactorProperty().bind(control.zoomFactorProperty());
        horizontalRuler.visibleContentXProperty().bind(control.visibleContentXProperty());
        horizontalRuler.viewportWidthProperty().bind(control.viewportWidthProperty());

        verticalRuler.zoomFactorProperty().bind(control.zoomFactorProperty());
        verticalRuler.visibleContentYProperty().bind(control.visibleContentYProperty());
        verticalRuler.viewportHeightProperty().bind(control.viewportHeightProperty());
        verticalRuler.setViewTheme(theme);

        control.horizontalRulerVisibleProperty().addListener((obs, oldValue, newValue) -> updateRulerVisibility(control));
        control.verticalRulerVisibleProperty().addListener((obs, oldValue, newValue) -> updateRulerVisibility(control));
    }

    private void updateRulerVisibility(WriterDocumentView control) {
        boolean showHorizontal = control.isHorizontalRulerVisible();
        boolean showVertical = control.isVerticalRulerVisible();
        setNodeShown(rulerRow, showHorizontal);
        setNodeShown(horizontalRuler, showHorizontal);
        setNodeShown(verticalRuler, showVertical);
        setNodeShown(corner, showHorizontal && showVertical);
    }

    private void updateRulerPageStyle(Document document) {
        if (document == null) {
            return;
        }
        PageStyle pageStyle = document.pageStyle();
        horizontalRuler.setPageStyle(pageStyle);
        verticalRuler.setPageStyle(pageStyle);
    }

    private static void setNodeShown(javafx.scene.Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }
}
