package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RenderTarget;
import io.github.ggeorg.delos.render.RenderTheme;

import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.layout.BlockRole;
import io.github.ggeorg.delos.writer.layout.LaidOutLine;
import io.github.ggeorg.delos.writer.layout.LaidOutTextBlock;
import io.github.ggeorg.delos.writer.document.TextPosition;

/** Paints selection highlights for laid-out body text. */
final class SelectionRenderer {
    void paint(RenderTarget target,
               LaidOutTextBlock block,
               RenderTheme theme,
               double pageX,
               double pageY,
               SelectionRange selection) {
        if (selection == null || selection.isCollapsed() || block.role() != BlockRole.BODY) {
            return;
        }

        TextPosition selectionStart = selection.start();
        TextPosition selectionEnd = selection.end();
        if (selectionEnd.paragraphIndex() < block.sourceParagraphIndex()
                || selectionStart.paragraphIndex() > block.sourceParagraphIndex()) {
            return;
        }

        target.setFill(theme.selectionFill());
        for (LaidOutLine line : block.lines()) {
            paintLineSelection(target, block, pageX, pageY, selectionStart, selectionEnd, line);
        }
    }

    private void paintLineSelection(RenderTarget target,
                                    LaidOutTextBlock block,
                                    double pageX,
                                    double pageY,
                                    TextPosition selectionStart,
                                    TextPosition selectionEnd,
                                    LaidOutLine line) {
        TextPosition lineStart = new TextPosition(block.sourceParagraphIndex(), line.startOffset());
        TextPosition lineEnd = new TextPosition(block.sourceParagraphIndex(), line.endOffset());
        if (selectionEnd.compareTo(lineStart) <= 0 || selectionStart.compareTo(lineEnd) >= 0) {
            return;
        }

        int startColumn = selectionStart.compareTo(lineStart) <= 0
                ? 0
                : line.columnForOffset(selectionStart.offset());
        int endColumn = selectionEnd.compareTo(lineEnd) >= 0
                ? line.columnForOffset(line.endOffset())
                : line.columnForOffset(selectionEnd.offset());
        if (endColumn <= startColumn) {
            return;
        }

        double x1 = pageX + block.x() + line.caretXForColumn(startColumn);
        double x2 = pageX + block.x() + line.caretXForColumn(endColumn);
        double y = pageY + block.y() + line.y();
        target.fillRect(x1, y, x2 - x1, line.height());
    }
}
