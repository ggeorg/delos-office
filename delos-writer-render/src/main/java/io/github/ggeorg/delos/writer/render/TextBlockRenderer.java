package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RenderColor;
import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.render.RenderTarget;
import io.github.ggeorg.delos.render.TextDecorationMetrics;
import io.github.ggeorg.delos.render.TextLayoutResult;
import io.github.ggeorg.delos.render.RenderTextMeasurer;
import io.github.ggeorg.delos.render.RenderTheme;

import io.github.ggeorg.delos.writer.layout.BlockRole;
import io.github.ggeorg.delos.writer.layout.LaidOutLine;
import io.github.ggeorg.delos.writer.layout.LaidOutRun;
import io.github.ggeorg.delos.writer.layout.LaidOutTextBlock;

/** Paints laid-out text blocks and styled runs. */
final class TextBlockRenderer {
    private final ProtrusionRenderPlanner protrusionPlanner;

    TextBlockRenderer() {
        this(MarginProtrusionPolicy.DEFAULT);
    }

    TextBlockRenderer(MarginProtrusionPolicy protrusionPolicy) {
        this.protrusionPlanner = new ProtrusionRenderPlanner(protrusionPolicy);
    }

    void paint(RenderTarget target,
               LaidOutTextBlock block,
               RenderTheme theme,
               RenderTextMeasurer measurer,
               double pageX,
               double pageY) {
        RenderFont baseFont = block.role() == BlockRole.TITLE ? theme.titleFont() : theme.bodyFont();
        RenderColor textColor = block.role() == BlockRole.TITLE ? theme.titleText() : theme.bodyText();
        target.setFill(textColor);

        paintListMarkerIfNeeded(target, block, baseFont, pageX, pageY);

        for (LaidOutLine line : block.lines()) {
            paintLine(target, line, block, baseFont, textColor, measurer, pageX, pageY);
        }
    }

    private void paintListMarkerIfNeeded(RenderTarget target,
                                          LaidOutTextBlock block,
                                          RenderFont baseFont,
                                          double pageX,
                                          double pageY) {
        if (!block.listMarker().visible()) {
            return;
        }
        target.setFont(baseFont);
        target.fillText(
                block.listMarker().text(),
                pageX + block.x() + block.listMarker().x(),
                pageY + block.y() + block.listMarker().y()
        );
    }

    private void paintLine(RenderTarget target,
                           LaidOutLine line,
                           LaidOutTextBlock block,
                           RenderFont baseFont,
                           RenderColor textColor,
                           RenderTextMeasurer measurer,
                           double pageX,
                           double pageY) {
        double baselineY = pageY + block.y() + line.y() + line.baseline();
        LaidOutRun firstNonEmptyRun = firstNonEmptyRun(line);
        LaidOutRun lastNonEmptyRun = lastNonEmptyRun(line);

        for (LaidOutRun run : line.runs()) {
            RenderFont runFont = measurer.styledFont(baseFont, run.bold(), run.italic());
            TextLayoutResult layout = measurer.layoutText(run.text(), runFont);
            double runX = pageX + block.x() + line.x() + run.x();
            target.setFont(runFont);
            paintRun(target, run, runFont, measurer, runX, baselineY, run == firstNonEmptyRun, run == lastNonEmptyRun);
            paintDecorationsIfNeeded(target, run, layout, textColor, runX, baselineY);
        }
    }

    private void paintDecorationsIfNeeded(RenderTarget target,
                                          LaidOutRun run,
                                          TextLayoutResult layout,
                                          RenderColor textColor,
                                          double runX,
                                          double baselineY) {
        if ((!run.underline() && !run.strikethrough()) || decorationWidth(run, layout) <= 0.0) {
            return;
        }

        TextDecorationMetrics decorations = layout.decorations();
        double width = decorationWidth(run, layout);
        target.save();
        target.setStroke(textColor);
        target.setLineWidth(decorations.thickness());
        if (run.underline()) {
            double underlineY = baselineY + decorations.underlineOffset();
            target.strokeLine(runX, underlineY, runX + width, underlineY);
        }
        if (run.strikethrough()) {
            double strikeY = baselineY - decorations.strikethroughOffset();
            target.strokeLine(runX, strikeY, runX + width, strikeY);
        }
        target.restore();
    }

    private double decorationWidth(LaidOutRun run, TextLayoutResult layout) {
        if (layout.width() > 0.0) {
            return layout.width();
        }
        return Math.max(0.0, run.width());
    }

    private LaidOutRun firstNonEmptyRun(LaidOutLine line) {
        for (LaidOutRun run : line.runs()) {
            if (!run.text().isEmpty()) {
                return run;
            }
        }
        return null;
    }

    private LaidOutRun lastNonEmptyRun(LaidOutLine line) {
        for (int i = line.runs().size() - 1; i >= 0; i--) {
            LaidOutRun run = line.runs().get(i);
            if (!run.text().isEmpty()) {
                return run;
            }
        }
        return null;
    }

    private void paintRun(RenderTarget target,
                          LaidOutRun run,
                          RenderFont runFont,
                          RenderTextMeasurer measurer,
                          double runX,
                          double baselineY,
                          boolean isLeadingEdgeRun,
                          boolean isTrailingEdgeRun) {
        String text = run.text();
        if (text.isEmpty()) {
            return;
        }

        double cursorX = runX;
        for (ProtrusionRenderPlanner.Piece piece : protrusionPlanner.plan(text, isLeadingEdgeRun, isTrailingEdgeRun)) {
            if (piece.text().isEmpty()) {
                continue;
            }
            if (piece.protruded() && piece.text().length() == 1) {
                char ch = piece.text().charAt(0);
                double naturalWidth = measurer.charWidth(ch, runFont);
                target.fillText(piece.text(), cursorX + naturalWidth * piece.shiftFraction(), baselineY);
                cursorX += naturalWidth;
            } else {
                target.fillText(piece.text(), cursorX, baselineY);
                cursorX += measurer.textWidth(piece.text(), runFont);
            }
        }
    }
}
