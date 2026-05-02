package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.PageStyle;

import java.util.List;

/**
 * Mutable pagination cursor used by small block paginators.
 *
 * <p>This intentionally stays package-private: it is an internal layout seam,
 * not a public Writer API. The owning document paginator still controls page
 * creation and final layout assembly.</p>
 */
interface PageFlowState {
    int pageIndex();

    double cursorY();

    void cursorY(double cursorY);

    List<LaidOutBlock> currentBlocks();

    void advanceToNextPage(PageStyle pageStyle);
}
