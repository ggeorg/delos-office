package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.FormulaBlock;
import io.github.ggeorg.delos.writer.document.HorizontalRuleBlock;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.document.PageStyle;

import java.util.List;
import java.util.Objects;

/**
 * Paginates one-piece Writer blocks such as images, formulas, and horizontal rules.
 *
 * <p>The blocks handled here are atomic from the pagination perspective: they can
 * move to a fresh page when they do not fit, but they are not split across pages.</p>
 */
final class AtomicBlockPaginator {
    static final double IMAGE_BLOCK_SPACING_AFTER = 6.0;
    static final double FORMULA_BLOCK_SPACING_AFTER = 6.0;
    static final double HORIZONTAL_RULE_BLOCK_SPACING_AFTER = 6.0;

    private final ImageBlockLayouter imageBlockLayouter;
    private final FormulaBlockLayouter formulaBlockLayouter;
    private final HorizontalRuleBlockLayouter horizontalRuleBlockLayouter;

    AtomicBlockPaginator() {
        this(new ImageBlockLayouter(), new FormulaBlockLayouter(), new HorizontalRuleBlockLayouter());
    }

    AtomicBlockPaginator(
        ImageBlockLayouter imageBlockLayouter,
        FormulaBlockLayouter formulaBlockLayouter,
        HorizontalRuleBlockLayouter horizontalRuleBlockLayouter
    ) {
        this.imageBlockLayouter = Objects.requireNonNull(imageBlockLayouter, "imageBlockLayouter");
        this.formulaBlockLayouter = Objects.requireNonNull(formulaBlockLayouter, "formulaBlockLayouter");
        this.horizontalRuleBlockLayouter = Objects.requireNonNull(horizontalRuleBlockLayouter, "horizontalRuleBlockLayouter");
    }

    void appendImage(
        PageStyle pageStyle,
        List<LaidOutPage> pages,
        PageFlowState state,
        int sourceBlockIndex,
        ImageBlock imageBlock
    ) {
        appendAtomicBlock(
            pageStyle,
            pages,
            state,
            sourceBlockIndex,
            IMAGE_BLOCK_SPACING_AFTER,
            cursorY -> layoutImage(sourceBlockIndex, imageBlock, pageStyle, cursorY)
        );
    }

    void appendFormula(
        PageStyle pageStyle,
        LayoutTheme theme,
        List<LaidOutPage> pages,
        PageFlowState state,
        int sourceBlockIndex,
        FormulaBlock formulaBlock
    ) {
        appendAtomicBlock(
            pageStyle,
            pages,
            state,
            sourceBlockIndex,
            FORMULA_BLOCK_SPACING_AFTER,
            cursorY -> layoutFormula(sourceBlockIndex, formulaBlock, pageStyle, cursorY, theme)
        );
    }

    void appendHorizontalRule(
        PageStyle pageStyle,
        List<LaidOutPage> pages,
        PageFlowState state,
        int sourceBlockIndex,
        HorizontalRuleBlock horizontalRuleBlock
    ) {
        appendAtomicBlock(
            pageStyle,
            pages,
            state,
            sourceBlockIndex,
            HORIZONTAL_RULE_BLOCK_SPACING_AFTER,
            cursorY -> layoutHorizontalRule(sourceBlockIndex, horizontalRuleBlock, pageStyle, cursorY)
        );
    }

    private void appendAtomicBlock(
        PageStyle pageStyle,
        List<LaidOutPage> pages,
        PageFlowState state,
        int sourceBlockIndex,
        double spacingAfter,
        AtomicBlockFactory blockFactory
    ) {
        double contentBottom = pageStyle.height() - pageStyle.marginBottom();
        LaidOutAtomicBlock block = blockFactory.layoutAt(state.cursorY());

        if (!state.currentBlocks().isEmpty() && state.cursorY() + block.height() > contentBottom) {
            pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
            state.advanceToNextPage(pageStyle);
            block = blockFactory.layoutAt(state.cursorY());
        }

        state.currentBlocks().add(block);
        state.cursorY(state.cursorY() + block.height());
        if (state.cursorY() + spacingAfter <= contentBottom) {
            state.cursorY(state.cursorY() + spacingAfter);
        }
    }

    private LaidOutImageBlock layoutImage(int sourceBlockIndex, ImageBlock imageBlock, PageStyle pageStyle, double y) {
        return imageBlockLayouter.layout(sourceBlockIndex, imageBlock, pageStyle.marginLeft(), y, pageStyle.contentWidth());
    }

    private LaidOutFormulaBlock layoutFormula(
        int sourceBlockIndex,
        FormulaBlock formulaBlock,
        PageStyle pageStyle,
        double y,
        LayoutTheme theme
    ) {
        return formulaBlockLayouter.layout(sourceBlockIndex, formulaBlock, pageStyle.marginLeft(), y, pageStyle.contentWidth(), theme);
    }

    private LaidOutSeparator layoutHorizontalRule(
        int sourceBlockIndex,
        HorizontalRuleBlock horizontalRuleBlock,
        PageStyle pageStyle,
        double y
    ) {
        return horizontalRuleBlockLayouter.layout(sourceBlockIndex, horizontalRuleBlock, pageStyle.marginLeft(), y, pageStyle.contentWidth());
    }

    @FunctionalInterface
    private interface AtomicBlockFactory {
        LaidOutAtomicBlock layoutAt(double cursorY);
    }
}
