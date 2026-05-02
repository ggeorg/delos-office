package io.github.ggeorg.delos.writer.ui;

import io.github.ggeorg.delos.writer.document.BlockSelection;
import io.github.ggeorg.delos.writer.document.TableCellSelection;
import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.layout.CaretGeometry;
import io.github.ggeorg.delos.writer.layout.CaretLocator;
import io.github.ggeorg.delos.writer.layout.HitTestResult;
import io.github.ggeorg.delos.writer.layout.LaidOutAtomicBlock;
import io.github.ggeorg.delos.writer.layout.LaidOutBlock;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.ResolvedTextPosition;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.render.CompositionTextState;
import io.github.ggeorg.delos.writer.render.DecorationKind;
import io.github.ggeorg.delos.writer.render.PageDecoration;
import io.github.ggeorg.delos.writer.render.PageRenderState;
import io.github.ggeorg.delos.writer.ui.geometry.PageGeometryIndex;
import io.github.ggeorg.delos.writer.ui.virtualization.PageVirtualizer;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps virtualized page views in sync with layout results and render state.
 */
public final class DocumentViewportViewSynchronizer {
    private final PageVirtualizer pageVirtualizer;
    private final CaretLocator caretLocator;

    public DocumentViewportViewSynchronizer(PageVirtualizer pageVirtualizer, CaretLocator caretLocator) {
        this.pageVirtualizer = pageVirtualizer;
        this.caretLocator = caretLocator;
    }

    public void syncPageViews(LaidOutDocument layout) {
        pageVirtualizer.setLayout(layout);
    }

    public void setVisibleViewport(Bounds visibleViewportInContent) {
        pageVirtualizer.setVisibleViewport(visibleViewportInContent);
    }

    public void refreshPageStates(
            LaidOutDocument laidOutDocument,
            SelectionRange selectionRange,
            BlockSelection blockSelection,
            TableCellSelection tableCellSelection,
            ResolvedTextPosition resolvedCaret,
            boolean hideCaretForTableCellSelection,
            CaretGeometry caret,
            boolean caretVisible,
            CompositionTextState composition
    ) {
        if (laidOutDocument == null) {
            return;
        }

        pageVirtualizer.forEachActivePageView(pageView -> {
            CaretGeometry pageCaret = null;
            boolean pageCaretVisible = false;
            if (caret != null && resolvedCaret != null && resolvedCaret.pageIndex() == pageView.page().pageIndex()) {
                pageCaret = caret;
                pageCaretVisible = caretVisible && blockSelection == null && (!hideCaretForTableCellSelection || tableCellSelection == null);
            }
            CompositionTextState pageComposition = compositionForPage(composition, pageView.page().pageIndex());
            List<PageDecoration> decorations = new ArrayList<>();
            decorations.addAll(blockSelectionDecorations(pageView.page().blocks(), blockSelection));
            decorations.addAll(tableCellSelectionDecorations(pageView.page().blocks(), tableCellSelection));
            pageView.setRenderState(new PageRenderState(pageCaret, selectionRange, pageCaretVisible, pageComposition, decorations));
        });
    }

    /**
     * Computes the caret bounds in page-column content coordinates using only
     * the immutable layout model.
     * <p>
     * This intentionally avoids PageView.localToParent(), localToScene(), and
     * any other scene-graph transform. After Enter/backspace, the JavaFX layout
     * pulse can lag behind the already-rebuilt document model; using the model
     * keeps scroll targeting stable and prevents stale page/row coordinates.
     */
    public Bounds caretBoundsInContent(LaidOutDocument laidOutDocument, TextPosition caretPosition) {
        if (laidOutDocument == null || caretPosition == null) {
            return null;
        }
        var resolved = caretLocator.resolve(laidOutDocument, caretPosition);
        CaretGeometry caret = caretLocator.locateCaret(resolved);
        if (resolved == null || caret == null || resolved.pageIndex() < 0 || resolved.pageIndex() >= laidOutDocument.pages().size()) {
            return null;
        }

        return pageGeometryIndex().caretBoundsInContent(resolved.pageIndex(), caret);
    }

    private CompositionTextState compositionForPage(CompositionTextState composition, int pageIndex) {
        if (composition == null || composition.isEmpty() || composition.pageIndex() != pageIndex) {
            return CompositionTextState.EMPTY;
        }
        return composition;
    }

    private List<PageDecoration> blockSelectionDecorations(List<LaidOutBlock> blocks, BlockSelection selection) {
        if (selection == null) {
            return List.of();
        }
        List<PageDecoration> decorations = new ArrayList<>();
        for (LaidOutBlock block : blocks) {
            if (block instanceof LaidOutAtomicBlock atomicBlock
                    && atomicBlock.sourceBlockIndex() == selection.blockIndex()) {
                decorations.add(PageDecoration.highlight(DecorationKind.COMMENT_HIGHLIGHT,
                        atomicBlock.x(), atomicBlock.y(), atomicBlock.width(), atomicBlock.height()));
            }
        }
        return decorations;
    }


    private List<PageDecoration> tableCellSelectionDecorations(List<LaidOutBlock> blocks, TableCellSelection selection) {
        if (selection == null) {
            return List.of();
        }
        List<PageDecoration> decorations = new ArrayList<>();
        for (LaidOutBlock block : blocks) {
            if (!(block instanceof io.github.ggeorg.delos.writer.layout.LaidOutTableBlock tableBlock)
                    || tableBlock.sourceBlockIndex() != selection.blockIndex()
                    || selection.rowIndex() >= tableBlock.rows().size()) {
                continue;
            }
            var row = tableBlock.rows().get(selection.rowIndex());
            if (selection.columnIndex() >= row.cells().size()) {
                continue;
            }
            var cell = row.cells().get(selection.columnIndex());
            decorations.add(PageDecoration.highlight(
                    DecorationKind.COMMENT_HIGHLIGHT,
                    tableBlock.x() + cell.x(),
                    tableBlock.y() + cell.y(),
                    cell.width(),
                    cell.height()
            ));
        }
        return decorations;
    }

    public double contentWidth() {
        return pageVirtualizer.contentWidth();
    }

    public double contentHeight() {
        return pageVirtualizer.contentHeight();
    }

    public PageGeometryIndex pageGeometryIndex() {
        return pageVirtualizer.geometryIndex();
    }

    public HitTestResult findNearestHit(double sceneX, double sceneY) {
        PageView bestView = null;
        Point2D bestLocal = null;
        double bestDistance = Double.MAX_VALUE;

        for (PageView pageView : pageVirtualizer.activePageViews()) {
            Point2D local = pageView.sceneToLocal(sceneX, sceneY);
            double distance = distanceToBounds(local.getX(), local.getY(), pageView.getWidth(), pageView.getHeight());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestView = pageView;
                bestLocal = local;
            }
        }

        return bestView == null || bestLocal == null ? null : bestView.hitTest(bestLocal.getX(), bestLocal.getY());
    }

    public TextPosition clampCaretToDocument(LaidOutDocument laidOutDocument, TextPosition position, io.github.ggeorg.delos.writer.layout.DocumentPositionNavigator navigator) {
        if (caretLocator.resolve(laidOutDocument, position) != null) {
            return position;
        }
        TextPosition first = navigator.firstPosition(laidOutDocument);
        return first == null ? new TextPosition(0, 0) : first;
    }

    public SelectionRange clampSelectionToDocument(LaidOutDocument laidOutDocument, SelectionRange selection, io.github.ggeorg.delos.writer.layout.DocumentPositionNavigator navigator) {
        if (selection == null) {
            return null;
        }

        TextPosition anchor = clampCaretToDocument(laidOutDocument, selection.anchor(), navigator);
        TextPosition focus = clampCaretToDocument(laidOutDocument, selection.focus(), navigator);
        SelectionRange resolved = new SelectionRange(anchor, focus);
        return resolved.isCollapsed() ? null : resolved;
    }

    private double distanceToBounds(double x, double y, double width, double height) {
        double dx = x < 0 ? -x : Math.max(0, x - width);
        double dy = y < 0 ? -y : Math.max(0, y - height);
        return Math.hypot(dx, dy);
    }
}
