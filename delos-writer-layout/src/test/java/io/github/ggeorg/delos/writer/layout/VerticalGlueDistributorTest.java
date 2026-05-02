package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.PageStyle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VerticalGlueDistributorTest {
    @Test
    void leavesFinalPageUntouchedWhenFinalStretchIsDisabled() {
        PageStyle pageStyle = new PageStyle(100, 120, 10, 10, 10, 10);
        LaidOutPage page = new LaidOutPage(0, 100, 120, List.of(
            textBlock(0, 10, 10, 10),
            textBlock(1, 10, 30, 10)
        ));

        List<LaidOutPage> adjusted = new VerticalGlueDistributor().apply(
            pageStyle,
            List.of(page),
            new PaginatingDocumentLayoutEngine.VerticalGluePolicy(true, false, 10.0, 20.0)
        );

        LaidOutTextBlock second = (LaidOutTextBlock) adjusted.getFirst().blocks().get(1);
        assertEquals(30.0, second.y(), 0.001);
    }

    @Test
    void stretchesEligibleGapsOnNonFinalPagesUpToConfiguredCap() {
        PageStyle pageStyle = new PageStyle(100, 120, 10, 10, 10, 10);
        LaidOutPage first = new LaidOutPage(0, 100, 120, List.of(
            textBlock(0, 10, 10, 10),
            textBlock(1, 10, 30, 10)
        ));
        LaidOutPage finalPage = new LaidOutPage(1, 100, 120, List.of(textBlock(2, 10, 10, 10)));

        List<LaidOutPage> adjusted = new VerticalGlueDistributor().apply(
            pageStyle,
            List.of(first, finalPage),
            new PaginatingDocumentLayoutEngine.VerticalGluePolicy(true, false, 10.0, 7.0)
        );

        LaidOutTextBlock shiftedSecond = (LaidOutTextBlock) adjusted.getFirst().blocks().get(1);
        LaidOutTextBlock unshiftedFinal = (LaidOutTextBlock) adjusted.get(1).blocks().getFirst();
        assertEquals(37.0, shiftedSecond.y(), 0.001);
        assertEquals(10.0, unshiftedFinal.y(), 0.001);
    }

    private static LaidOutTextBlock textBlock(int sourceParagraphIndex, double x, double y, double height) {
        return new LaidOutTextBlock(
            BlockRole.BODY,
            x,
            y,
            80,
            height,
            sourceParagraphIndex,
            0,
            true,
            true,
            List.of(line(height))
        );
    }

    private static LaidOutLine line(double height) {
        return new LaidOutLine(
            "x",
            0,
            0,
            6,
            height,
            Math.max(0, height - 2),
            0,
            1,
            List.of(),
            List.of(0.0, 6.0)
        );
    }
}
