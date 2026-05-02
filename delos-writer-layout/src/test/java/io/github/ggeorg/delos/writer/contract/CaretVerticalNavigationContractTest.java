package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.layout.BlockRole;
import io.github.ggeorg.delos.writer.layout.CaretGeometry;
import io.github.ggeorg.delos.writer.layout.CaretLocator;
import io.github.ggeorg.delos.writer.layout.DocumentPositionNavigator;
import io.github.ggeorg.delos.writer.layout.LaidOutBlock;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LaidOutLine;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.layout.LaidOutRun;
import io.github.ggeorg.delos.writer.layout.LaidOutTextBlock;
import io.github.ggeorg.delos.writer.layout.ResolvedTextPosition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class CaretVerticalNavigationContractTest {
    @Test
    void arrowDownCanLandAtTheStartOfASoftWrappedLine() {
        LaidOutDocument document = documentWithTwoSoftWrappedLines(20.0);
        DocumentPositionNavigator navigator = new DocumentPositionNavigator();
        CaretLocator locator = new CaretLocator();

        CaretGeometry caret = locator.locateCaret(document, new TextPosition(0, 0));
        assertNotNull(caret);

        TextPosition target = navigator.moveVertical(document, new TextPosition(0, 0), 1, caret.x());
        assertEquals(new TextPosition(0, 5), target);

        ResolvedTextPosition resolvedTarget = locator.resolve(document, target);
        assertNotNull(resolvedTarget);
        assertEquals(1, resolvedTarget.lineIndex());
        assertEquals(0, resolvedTarget.columnIndex());
    }

    private static LaidOutDocument documentWithTwoSoftWrappedLines(double lineX) {
        List<LaidOutLine> lines = List.of(
                line("Hello", lineX, 0.0, 0, 5),
                line("world", lineX, 22.0, 5, 10)
        );
        LaidOutTextBlock block = new LaidOutTextBlock(
                BlockRole.BODY,
                72.0,
                68.0,
                400.0,
                44.0,
                0,
                0,
                true,
                true,
                lines
        );
        LaidOutPage page = new LaidOutPage(0, 595.0, 842.0, List.<LaidOutBlock>of(block));
        return new LaidOutDocument(PageStyle.a4Default(), List.of(page));
    }

    private static LaidOutLine line(String text, double x, double y, int startOffset, int endOffset) {
        List<Double> caretStops = new ArrayList<>(text.length() + 1);
        for (int i = 0; i <= text.length(); i++) {
            caretStops.add(i * 10.0);
        }
        return new LaidOutLine(
                text,
                x,
                y,
                text.length() * 10.0,
                18.0,
                14.0,
                startOffset,
                endOffset,
                List.of(new LaidOutRun(text, 0, text.length(), 0.0, text.length() * 10.0, CharacterStyle.PLAIN)),
                caretStops
        );
    }
}
