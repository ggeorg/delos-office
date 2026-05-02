package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.layout.BlockRole;
import io.github.ggeorg.delos.writer.layout.CaretLocator;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LaidOutLine;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.layout.LaidOutTextBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class CaretLocatorResolvedContractTest {
    @Test
    void locatesCaretFromAlreadyResolvedPositionWithoutResolvingAgain() {
        CaretLocator locator = new CaretLocator();
        LaidOutDocument document = document();

        var resolved = locator.resolve(document, new TextPosition(0, 2));
        var caret = locator.locateCaret(resolved);

        assertNotNull(resolved);
        assertNotNull(caret);
        assertEquals(142.0, caret.x());
        assertEquals(220.0, caret.y());
        assertEquals(18.0, caret.height());
    }

    private static LaidOutDocument document() {
        PageStyle pageStyle = new PageStyle(500.0, 700.0, 50.0, 50.0, 50.0, 50.0);
        LaidOutLine line = new LaidOutLine(
                "abcd",
                10.0,
                20.0,
                40.0,
                18.0,
                14.0,
                0,
                4,
                List.of(),
                List.of(0.0, 8.0, 12.0, 20.0, 40.0)
        );
        LaidOutTextBlock block = new LaidOutTextBlock(
                BlockRole.BODY,
                120.0,
                200.0,
                300.0,
                18.0,
                0,
                0,
                true,
                true,
                List.of(line)
        );
        LaidOutPage page = new LaidOutPage(0, pageStyle.width(), pageStyle.height(), List.of(block));
        return new LaidOutDocument(pageStyle, List.of(page));
    }
}
