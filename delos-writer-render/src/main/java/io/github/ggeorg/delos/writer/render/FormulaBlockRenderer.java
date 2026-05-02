package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.RenderTarget;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.render.RenderTheme;
import io.github.ggeorg.delos.writer.layout.LaidOutFormulaBlock;

/** Paints a conservative source-based formula placeholder. */
final class FormulaBlockRenderer {
    private static final double PADDING_X = 12.0;
    private static final double PADDING_Y = 9.0;
    private static final double BADGE_WIDTH = 30.0;
    private static final double TEXT_GAP = 4.0;

    void paint(RenderTarget target,
               LaidOutFormulaBlock formulaBlock,
               RenderTheme theme,
               RenderTextMeasurer measurer,
               double offsetX,
               double offsetY) {
        double x = offsetX + formulaBlock.x();
        double y = offsetY + formulaBlock.y();
        double width = formulaBlock.width();
        double height = formulaBlock.height();
        target.save();
        try {
            target.setFill(theme.pageBackground());
            target.fillRoundRect(x, y, width, height, 8.0, 8.0);
            target.setStroke(theme.separatorColor());
            target.setLineWidth(1.0);
            target.strokeRoundRect(x, y, width, height, 8.0, 8.0);

            target.setStroke(theme.titleText());
            target.setLineWidth(2.0);
            target.strokeLine(x + 6.0, y + 8.0, x + 6.0, y + Math.max(8.0, height - 8.0));

            RenderFont badgeFont = measurer.styledFont(theme.bodyFont(), true, false);
            RenderFont previewFont = measurer.styledFont(theme.bodyFont(), false, false);
            RenderFont sourceFont = new RenderFont("Monospaced", Math.max(9.0, theme.bodyFont().size() - 3.0), false, false);

            double badgeBaseline = y + PADDING_Y + measurer.baseline(badgeFont);
            double previewBaseline = y + PADDING_Y + measurer.baseline(previewFont);
            double sourceBaseline = previewBaseline + measurer.lineHeight(previewFont) + TEXT_GAP;
            double textX = x + PADDING_X + BADGE_WIDTH;
            int previewLength = maxTextLength(width - BADGE_WIDTH - PADDING_X * 2, previewFont, measurer);
            int sourceLength = maxTextLength(width - BADGE_WIDTH - PADDING_X * 2, sourceFont, measurer);

            target.setFill(theme.titleText());
            target.setFont(badgeFont);
            target.fillText("ƒx", x + PADDING_X, badgeBaseline);

            target.setFill(theme.bodyText());
            target.setFont(previewFont);
            target.fillText(
                    FormulaDisplayText.abbreviate(FormulaDisplayText.preview(formulaBlock.source()), previewLength),
                    textX,
                    previewBaseline
            );

            if (sourceBaseline <= y + height - PADDING_Y / 2.0) {
                target.setFill(theme.titleText());
                target.setFont(sourceFont);
                target.fillText(
                        FormulaDisplayText.abbreviate(FormulaDisplayText.compactSource(formulaBlock.source()), sourceLength),
                        textX,
                        sourceBaseline
                );
            }
        } finally {
            target.restore();
        }
    }

    private int maxTextLength(double availableWidth, RenderFont font, RenderTextMeasurer measurer) {
        double averageCharWidth = Math.max(4.0, measurer.charWidth('M', font));
        return Math.max(8, (int) Math.floor(Math.max(0.0, availableWidth) / averageCharWidth));
    }
}
