package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RenderImageResolver;
import io.github.ggeorg.delos.render.RenderTarget;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.render.RenderTheme;

import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.layout.LaidOutBlock;
import io.github.ggeorg.delos.writer.layout.LaidOutFormulaBlock;
import io.github.ggeorg.delos.writer.layout.LaidOutImageBlock;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.layout.LaidOutSeparator;
import io.github.ggeorg.delos.writer.layout.LaidOutTableBlock;
import io.github.ggeorg.delos.writer.layout.LaidOutTextBlock;

import java.util.Objects;

/**
 * Default platform-neutral painter for a single laid-out page.
 */
public final class DefaultPageRenderer implements PageRenderer {
    private final PageChromeRenderer chromeRenderer;
    private final SelectionRenderer selectionRenderer;
    private final TextBlockRenderer textBlockRenderer;
    private final SeparatorRenderer separatorRenderer;
    private final TableBlockRenderer tableBlockRenderer;
    private final ImageBlockRenderer imageBlockRenderer;
    private final FormulaBlockRenderer formulaBlockRenderer;
    private final CaretRenderer caretRenderer;
    private final CompositionOverlayRenderer compositionOverlayRenderer;
    private final DecorationOverlayRenderer decorationOverlayRenderer;

    public DefaultPageRenderer() {
        TextBlockRenderer textBlockRenderer = new TextBlockRenderer();
        this.chromeRenderer = new PageChromeRenderer();
        this.selectionRenderer = new SelectionRenderer();
        this.textBlockRenderer = textBlockRenderer;
        this.separatorRenderer = new SeparatorRenderer();
        this.tableBlockRenderer = new TableBlockRenderer(textBlockRenderer);
        this.imageBlockRenderer = new ImageBlockRenderer();
        this.formulaBlockRenderer = new FormulaBlockRenderer();
        this.caretRenderer = new CaretRenderer();
        this.compositionOverlayRenderer = new CompositionOverlayRenderer();
        this.decorationOverlayRenderer = new DecorationOverlayRenderer();
    }

    DefaultPageRenderer(PageChromeRenderer chromeRenderer,
                        SelectionRenderer selectionRenderer,
                        TextBlockRenderer textBlockRenderer,
                        SeparatorRenderer separatorRenderer,
                        TableBlockRenderer tableBlockRenderer,
                        ImageBlockRenderer imageBlockRenderer,
                        FormulaBlockRenderer formulaBlockRenderer,
                        CaretRenderer caretRenderer,
                        CompositionOverlayRenderer compositionOverlayRenderer,
                        DecorationOverlayRenderer decorationOverlayRenderer) {
        this.chromeRenderer = chromeRenderer;
        this.selectionRenderer = selectionRenderer;
        this.textBlockRenderer = textBlockRenderer;
        this.separatorRenderer = separatorRenderer;
        this.tableBlockRenderer = tableBlockRenderer;
        this.imageBlockRenderer = imageBlockRenderer;
        this.formulaBlockRenderer = formulaBlockRenderer;
        this.caretRenderer = caretRenderer;
        this.compositionOverlayRenderer = compositionOverlayRenderer;
        this.decorationOverlayRenderer = decorationOverlayRenderer;
    }

    @Override
    public void renderPage(RenderTarget target, PageRenderContext context) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(context, "context");

        LaidOutPage page = context.page();
        RenderTheme theme = context.theme();
        RenderTextMeasurer measurer = context.measurer();
        PageRenderState state = context.state();

        double shadowX = context.drawPageChrome() ? theme.shadowExtentX() : 0.0;
        double shadowY = context.drawPageChrome() ? theme.shadowExtentY() : 0.0;
        double pageX = shadowX;
        double pageY = shadowY;

        target.save();
        try {
            if (context.drawPageChrome()) {
                chromeRenderer.paintWorkspace(target, theme, page.width() + shadowX * 2, page.height() + shadowY * 2);
                chromeRenderer.paintPageShell(target, theme, pageX, pageY, page.width(), page.height());
            }

            target.save();
            try {
                target.translate(pageX, pageY);
                target.clip(0.0, 0.0, page.width(), page.height());

                if (context.drawsEditorOverlays()) {
                    decorationOverlayRenderer.paint(target, theme, state.decorations(), DecorationLayer.BEHIND_TEXT);
                }

                for (LaidOutBlock block : page.blocks()) {
                    paintBlock(
                            target,
                            block,
                            theme,
                            measurer,
                            context.imageResolver(),
                            context.drawSelection() ? state.selection() : null
                    );
                }

                if (context.drawsEditorOverlays()) {
                    decorationOverlayRenderer.paint(target, theme, state.decorations(), DecorationLayer.ABOVE_TEXT);
                }

                if (context.drawCaret() && state.composition() != null && !state.composition().isEmpty()) {
                    compositionOverlayRenderer.paint(target, theme, measurer, 0.0, 0.0, state.composition());
                }

                if (context.drawCaret() && state.caret() != null && state.caretVisible()) {
                    caretRenderer.paint(target, theme, 0.0, 0.0, state.caret());
                }
            } finally {
                target.restore();
            }
        } finally {
            target.restore();
        }
    }

    private void paintBlock(RenderTarget target,
                            LaidOutBlock block,
                            RenderTheme theme,
                            RenderTextMeasurer measurer,
                            RenderImageResolver imageResolver,
                            SelectionRange selection) {
        if (block instanceof LaidOutTextBlock textBlock) {
            selectionRenderer.paint(target, textBlock, theme, 0.0, 0.0, selection);
            textBlockRenderer.paint(target, textBlock, theme, measurer, 0.0, 0.0);
        } else if (block instanceof LaidOutImageBlock imageBlock) {
            imageBlockRenderer.paint(target, imageBlock, theme, measurer, imageResolver, 0.0, 0.0);
        } else if (block instanceof LaidOutFormulaBlock formulaBlock) {
            formulaBlockRenderer.paint(target, formulaBlock, theme, measurer, 0.0, 0.0);
        } else if (block instanceof LaidOutTableBlock tableBlock) {
            tableBlockRenderer.paint(target, tableBlock, theme, measurer, 0.0, 0.0);
        } else if (block instanceof LaidOutSeparator separator) {
            separatorRenderer.paint(target, separator, theme, 0.0, 0.0);
        }
    }
}
