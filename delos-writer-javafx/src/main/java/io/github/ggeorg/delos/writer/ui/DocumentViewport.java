package io.github.ggeorg.delos.writer.ui;

import io.github.ggeorg.delos.writer.document.Alignment;
import io.github.ggeorg.delos.writer.document.DocumentMediaItem;
import io.github.ggeorg.delos.writer.document.FormulaBlock;
import io.github.ggeorg.delos.writer.document.FormulaSourceFormat;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.document.ListMarkerKind;
import io.github.ggeorg.delos.writer.session.EditorSession;
import io.github.ggeorg.delos.writer.editor.DocumentEditor;
import io.github.ggeorg.delos.writer.editor.DocumentFormatter;
import io.github.ggeorg.delos.writer.editor.EditorInteractionModel;
import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.document.TableCellSelection;
import io.github.ggeorg.delos.writer.document.TableBlock;
import io.github.ggeorg.delos.writer.editor.TextStyle;
import io.github.ggeorg.delos.writer.document.CaretPosition;
import io.github.ggeorg.delos.writer.layout.CaretGeometry;
import io.github.ggeorg.delos.writer.layout.CaretLocator;
import io.github.ggeorg.delos.writer.layout.DocumentLayoutEngine;
import io.github.ggeorg.delos.writer.layout.DocumentPositionNavigator;
import io.github.ggeorg.delos.writer.layout.KnuthPlassParagraphLayouter;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.PaginatingDocumentLayoutEngine;
import io.github.ggeorg.delos.writer.layout.ResolvedTextPosition;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.render.DefaultPageRenderer;
import io.github.ggeorg.delos.writer.render.fx.JavaFxTextMeasurer;
import io.github.ggeorg.delos.writer.render.PageRenderer;
import io.github.ggeorg.delos.writer.render.CompositionTextState;
import io.github.ggeorg.delos.writer.ui.virtualization.PageVirtualizer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Bounds;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.function.Consumer;

public final class DocumentViewport extends Region {
    private static final Duration CARET_BLINK_INTERVAL = Duration.millis(560);

    private final EditorSession session;
    private final ViewTheme theme;
    private final DocumentLayoutEngine layoutEngine;
    private final PageRenderer pageRenderer;
    private final PageVirtualizer pageVirtualizer;
    private final EditorInteractionModel interactionModel;
    private final CaretLocator caretLocator;
    private final DocumentPositionNavigator navigator;
    private final DocumentViewportEditController editController;
    private final DocumentViewportInputHandler inputHandler;
    private final InputMethodController inputMethodController;
    private final DocumentViewportViewSynchronizer viewSynchronizer;
    private final CaretFollowCoordinator caretFollowCoordinator;
    private final Timeline caretBlinkTimeline;
    private final ReadOnlyObjectWrapper<CaretGeometry> caretGeometry = new ReadOnlyObjectWrapper<>();
    private LaidOutDocument laidOutDocument;
    private Double preferredCaretPageX;
    private boolean caretVisible;

    public DocumentViewport(EditorSession session) {
        this(session, ViewTheme.defaultTheme(), new PaginatingDocumentLayoutEngine(new KnuthPlassParagraphLayouter(new JavaFxTextMeasurer())), new DefaultPageRenderer());
    }

    public DocumentViewport(EditorSession session, ViewTheme theme, DocumentLayoutEngine layoutEngine, PageRenderer pageRenderer) {
        this.session = session;
        this.theme = theme;
        this.layoutEngine = layoutEngine;
        this.pageRenderer = pageRenderer;
        this.pageVirtualizer = new PageVirtualizer(theme, pageRenderer);
        this.interactionModel = new EditorInteractionModel();
        this.caretLocator = new CaretLocator();
        this.navigator = new DocumentPositionNavigator();
        this.viewSynchronizer = new DocumentViewportViewSynchronizer(pageVirtualizer, caretLocator);
        this.caretFollowCoordinator = new CaretFollowCoordinator(
                interactionModel::caretPosition,
                position -> viewSynchronizer.caretBoundsInContent(laidOutDocument, position)
        );
        this.editController = new DocumentViewportEditController(
                session,
                interactionModel,
                navigator,
                new DocumentEditor(),
                new DocumentFormatter(),
                () -> laidOutDocument,
                this::rebuildLayout,
                caretFollowCoordinator::requestFollowCaret,
                () -> preferredCaretPageX = null
        );
        this.inputHandler = new DocumentViewportInputHandler(
                () -> laidOutDocument,
                interactionModel,
                navigator,
                caretLocator,
                editController,
                viewSynchronizer::findNearestHit,
                this::requestFocus,
                session::document,
                () -> preferredCaretPageX,
                value -> preferredCaretPageX = value
        );
        this.inputMethodController = new InputMethodController(
                interactionModel,
                editController,
                viewSynchronizer::findNearestHit,
                () -> viewSynchronizer.caretBoundsInContent(laidOutDocument, interactionModel.caretPosition()),
                this
        );
        this.caretBlinkTimeline = new Timeline(new KeyFrame(CARET_BLINK_INTERVAL, event -> handleCaretBlinkTick()));
        this.caretBlinkTimeline.setCycleCount(Timeline.INDEFINITE);

        getChildren().add(pageVirtualizer);

        setFocusTraversable(true);
        addEventHandler(KeyEvent.KEY_PRESSED, inputHandler::handleKeyPressed);
        addEventHandler(KeyEvent.KEY_TYPED, inputHandler::handleKeyTyped);
        installInputMethodHandling();
        addEventHandler(MouseEvent.MOUSE_PRESSED, inputHandler::handleMousePressed);
        addEventHandler(MouseEvent.MOUSE_DRAGGED, inputHandler::handleMouseDragged);
        addEventHandler(MouseEvent.MOUSE_RELEASED, inputHandler::handleMouseReleased);

        interactionModel.addChangeListener(this::handleInteractionChanged);
        focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                resetCaretBlink();
                requestCaretFollowAfterCurrentLayout();
            } else {
                caretBlinkTimeline.stop();
                setCaretVisible(false);
                caretFollowCoordinator.clear();
            }
        });

        rebuildLayout();
    }

    private void installInputMethodHandling() {
        if (!Platform.isSupported(ConditionalFeature.INPUT_METHOD)) {
            return;
        }

        // RichTextFX lesson: both the event handler and InputMethodRequests
        // must be installed for platform composition/dead-key input to work.
        setOnInputMethodTextChanged(inputMethodController::handleInputMethodTextChanged);
        setInputMethodRequests(inputMethodController.requests());
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isShortcutDown() || event.getCode().isNavigationKey()) {
                inputMethodController.clearCompositionRange();
            }
        });
    }

    public void undo() { editController.undo(); }
    public void redo() { editController.redo(); }
    public void copy() { editController.copy(); }
    public void cut() { editController.cut(); }
    public void paste() { editController.paste(); }
    public void selectAll() { editController.selectAll(); requestCaretFollowAfterCurrentLayout(); }
    public void reloadDocument() { preferredCaretPageX = null; caretFollowCoordinator.requestFollowCaret(); rebuildLayout(); }
    public void toggleStyle(TextStyle style) { editController.toggleStyle(style); }
    public void setParagraphAlignment(Alignment alignment) { editController.setParagraphAlignment(alignment); }
    public void toggleListKind(ListMarkerKind kind) { editController.toggleListKind(kind); }
    public void increaseListLevel() { editController.increaseListLevel(); }
    public void decreaseListLevel() { editController.decreaseListLevel(); }
    public void insertImage(DocumentMediaItem mediaItem, double width, double height, String altText) { editController.insertImage(mediaItem, width, height, altText); }
    public void insertTable(int rows, int columns) { editController.insertTable(rows, columns); }
    public TableCellSelection activeTableCellSelection() { return editController.activeTableCellSelection(); }
    public TableBlock selectedTableBlock() { return editController.selectedTableBlock(); }
    public boolean hasSelectedTable() { return editController.hasSelectedTable(); }
    public void insertTableRowAbove() { editController.insertTableRowAbove(); }
    public void insertTableRowBelow() { editController.insertTableRowBelow(); }
    public void deleteTableRow() { editController.deleteTableRow(); }
    public void insertTableColumnLeft() { editController.insertTableColumnLeft(); }
    public void insertTableColumnRight() { editController.insertTableColumnRight(); }
    public void deleteTableColumn() { editController.deleteTableColumn(); }
    public void setSelectedTableHeaderRow(boolean enabled) { editController.setSelectedTableHeaderRow(enabled); }
    public void updateSelectedTableProperties(double widthFraction, double cellPadding, boolean bordersEnabled) { editController.updateSelectedTableProperties(widthFraction, cellPadding, bordersEnabled); }
    public void insertFormula(FormulaSourceFormat sourceFormat, String source, String altText) { editController.insertFormula(sourceFormat, source, altText); }
    public FormulaBlock selectedFormulaBlock() { return editController.selectedFormulaBlock(); }
    public boolean hasSelectedFormulaBlock() { return editController.hasSelectedFormulaBlock(); }
    public void updateSelectedFormula(FormulaSourceFormat sourceFormat, String source, String altText) { editController.updateSelectedFormula(sourceFormat, source, altText); }
    public ImageBlock selectedImageBlock() { return editController.selectedImageBlock(); }
    public boolean hasSelectedImageBlock() { return editController.hasSelectedImageBlock(); }
    public void updateSelectedImageProperties(double width, double height, String altText) { editController.updateSelectedImageProperties(width, height, altText); }

    public boolean hasSelection() {
        SelectionRange selection = interactionModel.selectionRange();
        return interactionModel.blockSelection() != null
                || interactionModel.tableCellSelection() != null
                || (selection != null && !selection.isCollapsed());
    }

    public int currentPageNumber() {
        if (laidOutDocument == null || laidOutDocument.pages().isEmpty()) {
            return 1;
        }
        TextPosition caret = interactionModel.caretPosition();
        CaretPosition storyCaret = interactionModel.storyCaretPosition();
        if (caret == null && storyCaret == null) {
            return 1;
        }
        var resolved = storyCaret == null ? caretLocator.resolve(laidOutDocument, caret) : caretLocator.resolve(laidOutDocument, storyCaret);
        return resolved == null ? 1 : resolved.pageIndex() + 1;
    }

    public int totalPageCount() {
        return laidOutDocument == null || laidOutDocument.pages().isEmpty() ? 1 : laidOutDocument.pages().size();
    }

    public ReadOnlyObjectProperty<CaretGeometry> caretGeometryProperty() { return caretGeometry.getReadOnlyProperty(); }
    public CaretGeometry caretGeometry() { return caretGeometry.get(); }
    public TextPosition caretPosition() { return interactionModel.caretPosition(); }
    public SelectionRange selectionRange() { return interactionModel.selectionRange(); }
    public TableCellSelection tableCellSelection() { return interactionModel.tableCellSelection(); }
    public void setScrollIntoViewHandler(Consumer<Bounds> scrollIntoViewHandler) { caretFollowCoordinator.setScrollIntoViewHandler(scrollIntoViewHandler); }
    public double scrollableContentWidth() { return viewSynchronizer.contentWidth(); }
    public double scrollableContentHeight() { return viewSynchronizer.contentHeight(); }

    /**
     * Returns the latest immutable layout generated by the viewport.
     *
     * <p>This is intentionally exposed only to the {@code DelosEditor} control
     * boundary so export/print code can consume frozen layout snapshots without
     * depending on the viewport implementation.</p>
     */
    public LaidOutDocument laidOutDocument() {
        return laidOutDocument;
    }

    public void setVisibleViewport(Bounds visibleViewportInContent) {
        viewSynchronizer.setVisibleViewport(visibleViewportInContent);
    }

    private void handleInteractionChanged() {
        refreshPageStates();
        resetCaretBlink();
        if (!caretFollowCoordinator.isFollowCaretRequested()) {
            caretFollowCoordinator.requestFollowCaret();
        }
        caretFollowCoordinator.afterLayoutSync();
    }

    private void handleCaretBlinkTick() {
        if (!isCaretBlinkEligible()) {
            setCaretVisible(false);
            caretBlinkTimeline.stop();
            return;
        }
        setCaretVisible(!caretVisible);
    }

    private void rebuildLayout() {
        rebuildLayout(interactionModel.caretPosition(), interactionModel.selectionRange());
    }

    private void rebuildLayout(TextPosition targetCaret, SelectionRange targetSelection) {
        laidOutDocument = layoutEngine.layout(session.document(), theme.layoutTheme());
        viewSynchronizer.syncPageViews(laidOutDocument);

        restoreInteractionState(targetCaret, targetSelection);
        requestLayout();
        interactionModel.fireChanged();
    }

    private void restoreInteractionState(TextPosition targetCaret, SelectionRange targetSelection) {
        if (targetCaret == null) {
            TextPosition firstPosition = navigator.firstPosition(laidOutDocument);
            if (firstPosition != null) {
                interactionModel.restore(firstPosition, null);
            }
            return;
        }

        TextPosition resolvedCaret = viewSynchronizer.clampCaretToDocument(laidOutDocument, targetCaret, navigator);
        SelectionRange resolvedSelection = viewSynchronizer.clampSelectionToDocument(laidOutDocument, targetSelection, navigator);
        interactionModel.restore(resolvedCaret, resolvedSelection);
    }
    private void refreshPageStates() {
        TextPosition position = interactionModel.caretPosition();
        CaretPosition storyCaret = interactionModel.storyCaretPosition();
        ResolvedTextPosition resolvedCaret = storyCaret == null
                ? (position == null ? null : caretLocator.resolve(laidOutDocument, position))
                : caretLocator.resolve(laidOutDocument, storyCaret);
        CaretGeometry currentCaretGeometry = caretLocator.locateCaret(resolvedCaret);
        caretGeometry.set(currentCaretGeometry);

        TableCellSelection tableCellDecoration = interactionModel.tableCellSelection();
        boolean hideCaretForTableCellSelection = tableCellDecoration != null;
        if (tableCellDecoration == null) {
            tableCellDecoration = interactionModel.storyCaretTableCellSelection();
        }

        viewSynchronizer.refreshPageStates(
                laidOutDocument,
                interactionModel.selectionRange(),
                interactionModel.blockSelection(),
                tableCellDecoration,
                resolvedCaret,
                hideCaretForTableCellSelection,
                currentCaretGeometry,
                caretVisible,
                CompositionTextState.EMPTY
        );
    }

    private void resetCaretBlink() {
        setCaretVisible(isCaretBlinkEligible());
        caretBlinkTimeline.stop();
        if (isCaretBlinkEligible()) {
            caretBlinkTimeline.playFromStart();
        }
    }

    private boolean isCaretBlinkEligible() {
        return isFocused() && (interactionModel.caretPosition() != null || interactionModel.storyCaretPosition() != null);
    }

    private void setCaretVisible(boolean caretVisible) {
        if (this.caretVisible == caretVisible) {
            return;
        }
        this.caretVisible = caretVisible;
        refreshPageStates();
    }

    private void requestCaretFollowAfterCurrentLayout() {
        caretFollowCoordinator.requestFollowCaret();
        caretFollowCoordinator.afterLayoutSync();
    }

    @Override
    protected double computePrefWidth(double height) {
        return pageVirtualizer.prefWidth(height);
    }

    @Override
    protected double computePrefHeight(double width) {
        return pageVirtualizer.prefHeight(width);
    }

    @Override
    protected void layoutChildren() {
        pageVirtualizer.resizeRelocate(0.0, 0.0, getWidth(), getHeight());
    }
}
