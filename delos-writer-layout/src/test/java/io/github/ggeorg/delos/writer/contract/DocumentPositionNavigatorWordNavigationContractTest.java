package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.SelectionRange;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.layout.BlockRole;
import io.github.ggeorg.delos.writer.layout.DocumentPositionNavigator;
import io.github.ggeorg.delos.writer.layout.LaidOutBlock;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LaidOutLine;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.layout.LaidOutRun;
import io.github.ggeorg.delos.writer.layout.LaidOutTextBlock;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DocumentPositionNavigatorWordNavigationContractTest {
    private final DocumentPositionNavigator navigator = new DocumentPositionNavigator();

    @Test
    void movesByWordAcrossGreekAndEnglishText() {
        LaidOutDocument document = documentWithParagraph("hello σπίτι world");

        assertEquals(new TextPosition(0, 6), navigator.moveWordRight(document, new TextPosition(0, 0)));
        assertEquals(new TextPosition(0, 12), navigator.moveWordRight(document, new TextPosition(0, 6)));
        assertEquals(new TextPosition(0, 6), navigator.moveWordLeft(document, new TextPosition(0, 11)));
        assertEquals(new TextPosition(0, 0), navigator.moveWordLeft(document, new TextPosition(0, 5)));
    }

    @Test
    void selectsWordAtPosition() {
        LaidOutDocument document = documentWithParagraph("hello σπίτι world");

        SelectionRange range = navigator.wordRangeAt(document, new TextPosition(0, 8));

        assertEquals(new TextPosition(0, 6), range.start());
        assertEquals(new TextPosition(0, 11), range.end());
    }

    @Test
    void selectsWholeParagraphAtPosition() {
        LaidOutDocument document = documentWithParagraph("hello σπίτι world");

        SelectionRange range = navigator.paragraphRangeAt(document, new TextPosition(0, 8));

        assertEquals(new TextPosition(0, 0), range.start());
        assertEquals(new TextPosition(0, 17), range.end());
    }

    private static LaidOutDocument documentWithParagraph(String text) {
        LaidOutLine line = line(text, 0, text.length());
        LaidOutTextBlock block = new LaidOutTextBlock(
                BlockRole.BODY,
                72.0,
                68.0,
                400.0,
                22.0,
                0,
                0,
                true,
                true,
                List.of(line)
        );
        LaidOutPage page = new LaidOutPage(0, 595.0, 842.0, List.<LaidOutBlock>of(block));
        return new LaidOutDocument(PageStyle.a4Default(), List.of(page));
    }

    private static LaidOutLine line(String text, int startOffset, int endOffset) {
        List<Double> caretStops = new ArrayList<>(text.length() + 1);
        for (int i = 0; i <= text.length(); i++) {
            caretStops.add(i * 10.0);
        }
        return new LaidOutLine(
                text,
                0.0,
                0.0,
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
