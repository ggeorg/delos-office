package io.github.ggeorg.delos.writer.contract;

import io.github.ggeorg.delos.writer.document.CharacterStyle;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.ParagraphStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.layout.BlockRole;
import io.github.ggeorg.delos.writer.layout.CaretGeometry;
import io.github.ggeorg.delos.writer.layout.CaretLocator;
import io.github.ggeorg.delos.writer.layout.DocumentLayoutEngine;
import io.github.ggeorg.delos.writer.layout.HitTestResult;
import io.github.ggeorg.delos.writer.layout.LaidOutRun;
import io.github.ggeorg.delos.writer.layout.LaidOutBlock;
import io.github.ggeorg.delos.writer.layout.LaidOutDocument;
import io.github.ggeorg.delos.writer.layout.LaidOutLine;
import io.github.ggeorg.delos.writer.layout.LaidOutPage;
import io.github.ggeorg.delos.writer.layout.LaidOutTextBlock;
import io.github.ggeorg.delos.writer.layout.PageHitTester;
import io.github.ggeorg.delos.writer.layout.PaginatingDocumentLayoutEngine;
import io.github.ggeorg.delos.writer.layout.ParagraphLayouter;
import io.github.ggeorg.delos.writer.layout.ResolvedTextPosition;
import io.github.ggeorg.delos.writer.layout.GreedyParagraphLayouter;
import io.github.ggeorg.delos.writer.document.TextPosition;
import io.github.ggeorg.delos.writer.layout.LayoutTheme;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutContractTest {
    private static final LayoutTheme THEME = LayoutTheme.defaultTheme();

    @Test
    void allBodyParagraphsAppearInLaidOutDocument() {
        LaidOutDocument layout = layout(Document.sample());

        Set<Integer> laidOutParagraphs = layout.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .filter(LaidOutTextBlock.class::isInstance)
                .map(LaidOutTextBlock.class::cast)
                .filter(block -> block.role() == BlockRole.BODY)
                .map(LaidOutTextBlock::sourceParagraphIndex)
                .collect(Collectors.toSet());

        Set<Integer> expected = IntStream.range(0, Document.sample().paragraphs().size())
                .boxed()
                .collect(Collectors.toSet());

        assertEquals(expected, laidOutParagraphs);
    }

    @Test
    void documentTitleIsNotRenderedAsPageContent() {
        Document document = new Document(
                "My Document",
                PageStyle.a4Default(),
                List.of(Paragraph.of("Body text"))
        );
        LaidOutDocument layout = layout(document);
        LaidOutPage firstPage = layout.pages().getFirst();

        assertFalse(firstPage.blocks().isEmpty());
        assertTrue(firstPage.blocks().getFirst() instanceof LaidOutTextBlock);
        LaidOutTextBlock firstBlock = (LaidOutTextBlock) firstPage.blocks().getFirst();
        assertEquals(BlockRole.BODY, firstBlock.role());
        assertEquals(0, firstBlock.sourceParagraphIndex());
    }

    @Test
    void untitledPlaceholderIsNotRenderedAsPageContent() {
        LaidOutDocument layout = layout(Document.sample());
        LaidOutPage firstPage = layout.pages().getFirst();

        assertFalse(firstPage.blocks().isEmpty());
        assertTrue(firstPage.blocks().getFirst() instanceof LaidOutTextBlock);
        LaidOutTextBlock firstBlock = (LaidOutTextBlock) firstPage.blocks().getFirst();
        assertEquals(BlockRole.BODY, firstBlock.role());
        assertEquals(0, firstBlock.sourceParagraphIndex());
    }

    @Test
    void paragraphCanFragmentAcrossPagesWithoutLosingSourceIdentity() {
        Document document = new Document(
                "Fragmentation",
                new PageStyle(220.0, 240.0, 20.0, 20.0, 20.0, 20.0),
                List.of(Paragraph.of(longParagraph(350)))
        );

        LaidOutDocument layout = layout(document);

        List<LaidOutTextBlock> paragraphBlocks = layout.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .filter(LaidOutTextBlock.class::isInstance)
                .map(LaidOutTextBlock.class::cast)
                .filter(block -> block.role() == BlockRole.BODY)
                .toList();

        assertTrue(layout.pages().size() > 1);
        assertTrue(paragraphBlocks.size() > 1);
        assertTrue(paragraphBlocks.stream().allMatch(block -> block.sourceParagraphIndex() == 0));
        assertTrue(paragraphBlocks.stream().anyMatch(LaidOutTextBlock::isContinuationFragment));
    }


    @Test
    void pushesWholeParagraphToNextPageToAvoidSingleOrphanLine() {
        Document document = new Document(
                "TITLE",
                new PageStyle(220.0, 108.0, 20.0, 20.0, 20.0, 20.0),
                List.of(Paragraph.of("FILLER"), Paragraph.of("BODY-THREE"))
        );

        ParagraphLayouter layouter = fixedLineLayouter(Map.of(
                "FILLER", fixedLines("FILLER", 1, 20.0),
                "BODY-THREE", fixedLines("BODY-THREE", 3, 20.0)
        ));

        LaidOutDocument layout = engine(layouter).layout(document, THEME);

        List<LaidOutTextBlock> bodyBlocks = layout.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .filter(LaidOutTextBlock.class::isInstance)
                .map(LaidOutTextBlock.class::cast)
                .filter(block -> block.role() == BlockRole.BODY)
                .toList();

        assertTrue(layout.pages().size() >= 2);
        List<LaidOutTextBlock> targetBlocks = bodyBlocks.stream()
                .filter(block -> block.sourceParagraphIndex() == 1)
                .toList();

        assertEquals(1, targetBlocks.size());
        assertEquals(3, targetBlocks.getFirst().lines().size());
        assertTrue(targetBlocks.getFirst().firstFragment());
        assertTrue(targetBlocks.getFirst().lastFragment());
    }

    @Test
    void keepsShortParagraphTogetherWhenTheNextPageCanHoldIt() {
        Document document = new Document(
                "TITLE",
                new PageStyle(220.0, 148.0, 20.0, 20.0, 20.0, 20.0),
                List.of(Paragraph.of("FILLER"), Paragraph.of("BODY-FOUR"))
        );

        ParagraphLayouter layouter = fixedLineLayouter(Map.of(
                "FILLER", fixedLines("FILLER", 1, 20.0),
                "BODY-FOUR", fixedLines("BODY-FOUR", 4, 20.0)
        ));

        LaidOutDocument layout = engine(layouter).layout(document, THEME);

        List<LaidOutTextBlock> bodyBlocks = layout.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .filter(LaidOutTextBlock.class::isInstance)
                .map(LaidOutTextBlock.class::cast)
                .filter(block -> block.role() == BlockRole.BODY)
                .toList();

        assertTrue(layout.pages().size() >= 2);
        List<LaidOutTextBlock> targetBlocks = bodyBlocks.stream()
                .filter(block -> block.sourceParagraphIndex() == 1)
                .toList();

        assertEquals(1, targetBlocks.size());
        assertEquals(4, targetBlocks.getFirst().lines().size());
        assertTrue(targetBlocks.getFirst().firstFragment());
        assertTrue(targetBlocks.getFirst().lastFragment());
    }

    @Test
    void longerParagraphsStillSplitAndRebalanceToAvoidSingleWidowLines() {
        Document document = new Document(
                "TITLE",
                new PageStyle(220.0, 168.0, 20.0, 20.0, 20.0, 20.0),
                List.of(Paragraph.of("FILLER"), Paragraph.of("BODY-FIVE"))
        );

        ParagraphLayouter layouter = fixedLineLayouter(Map.of(
                "FILLER", fixedLines("FILLER", 1, 20.0),
                "BODY-FIVE", fixedLines("BODY-FIVE", 5, 20.0)
        ));

        LaidOutDocument layout = engine(layouter).layout(document, THEME);

        List<LaidOutTextBlock> bodyBlocks = layout.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .filter(LaidOutTextBlock.class::isInstance)
                .map(LaidOutTextBlock.class::cast)
                .filter(block -> block.role() == BlockRole.BODY)
                .toList();

        List<LaidOutTextBlock> targetBlocks = bodyBlocks.stream()
                .filter(block -> block.sourceParagraphIndex() == 1)
                .toList();

        assertTrue(layout.pages().size() >= 2);
        assertEquals(2, targetBlocks.size());
        assertEquals(3, targetBlocks.get(0).lines().size());
        assertEquals(2, targetBlocks.get(1).lines().size());
        assertTrue(targetBlocks.get(0).firstFragment());
        assertFalse(targetBlocks.get(0).lastFragment());
        assertFalse(targetBlocks.get(1).firstFragment());
        assertTrue(targetBlocks.get(1).lastFragment());
    }

    @Test
    void suppressesSpacingBeforeWhenParagraphStartsAtTopOfNewPage() {
        Paragraph filler = Paragraph.of("FILLER");
        Paragraph spaced = Paragraph.of(ParagraphStyle.defaultBody().withSpacingBefore(24.0), "SPACED");
        Document document = new Document(
                "TITLE",
                new PageStyle(220.0, 106.0, 20.0, 20.0, 20.0, 20.0),
                List.of(filler, spaced)
        );

        ParagraphLayouter layouter = fixedLineLayouter(Map.of(
                "FILLER", fixedLines("FILLER", 2, 20.0),
                "SPACED", fixedLines("SPACED", 1, 20.0)
        ));

        LaidOutDocument layout = engine(layouter).layout(document, THEME);

        List<LaidOutTextBlock> bodyBlocks = layout.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .filter(LaidOutTextBlock.class::isInstance)
                .map(LaidOutTextBlock.class::cast)
                .filter(block -> block.role() == BlockRole.BODY)
                .toList();

        assertEquals(2, bodyBlocks.size());
        LaidOutTextBlock spacedBlock = bodyBlocks.get(1);
        assertEquals(1, spacedBlock.sourceParagraphIndex());
        assertEquals(20.0, spacedBlock.y(), 0.0001, "spacing-before should be suppressed at the top of a new page");
    }

    @Test
    void shortParagraphThatCannotFitOnAFreshPageStillFragments() {
        Document document = new Document(
                "TITLE",
                new PageStyle(220.0, 88.0, 20.0, 20.0, 20.0, 20.0),
                List.of(Paragraph.of("FILLER"), Paragraph.of("BODY-FOUR"))
        );

        ParagraphLayouter layouter = fixedLineLayouter(Map.of(
                "FILLER", fixedLines("FILLER", 1, 20.0),
                "BODY-FOUR", fixedLines("BODY-FOUR", 4, 20.0)
        ));

        LaidOutDocument layout = engine(layouter).layout(document, THEME);

        List<LaidOutTextBlock> bodyBlocks = layout.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .filter(LaidOutTextBlock.class::isInstance)
                .map(LaidOutTextBlock.class::cast)
                .filter(block -> block.role() == BlockRole.BODY)
                .toList();

        List<LaidOutTextBlock> targetBlocks = bodyBlocks.stream()
                .filter(block -> block.sourceParagraphIndex() == 1)
                .toList();

        assertTrue(layout.pages().size() > 1);
        assertTrue(targetBlocks.size() > 1, "a short paragraph that cannot fit on a fresh page must still fragment");
        assertTrue(targetBlocks.get(0).firstFragment());
        assertFalse(targetBlocks.get(0).lastFragment());
    }

    @Test
    void tallParagraphRebalancesAcrossMultiplePagesWithoutSingleLineTailWhenPossible() {
        Document document = new Document(
                "TITLE",
                new PageStyle(220.0, 128.0, 20.0, 20.0, 20.0, 20.0),
                List.of(Paragraph.of("FILLER"), Paragraph.of("BODY-EIGHT"))
        );

        ParagraphLayouter layouter = fixedLineLayouter(Map.of(
                "FILLER", fixedLines("FILLER", 1, 20.0),
                "BODY-EIGHT", fixedLines("BODY-EIGHT", 8, 20.0)
        ));

        LaidOutDocument layout = engine(layouter).layout(document, THEME);

        List<LaidOutTextBlock> bodyBlocks = layout.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .filter(LaidOutTextBlock.class::isInstance)
                .map(LaidOutTextBlock.class::cast)
                .filter(block -> block.role() == BlockRole.BODY)
                .toList();

        List<LaidOutTextBlock> targetBlocks = bodyBlocks.stream()
                .filter(block -> block.sourceParagraphIndex() == 1)
                .toList();

        assertEquals(3, targetBlocks.size());
        assertEquals(2, targetBlocks.get(0).lines().size());
        assertEquals(4, targetBlocks.get(1).lines().size());
        assertEquals(2, targetBlocks.get(2).lines().size());
    }


    @Test
    void relaxedPaginationPolicyCanAllowNaiveSingleWidowSplits() {
        Document document = new Document(
                "TITLE",
                new PageStyle(220.0, 148.0, 20.0, 20.0, 20.0, 20.0),
                List.of(Paragraph.of("FILLER"), Paragraph.of("BODY-FOUR"))
        );

        ParagraphLayouter layouter = fixedLineLayouter(Map.of(
                "FILLER", fixedLines("FILLER", 1, 20.0),
                "BODY-FOUR", fixedLines("BODY-FOUR", 4, 20.0)
        ));

        PaginatingDocumentLayoutEngine.PaginationPolicy relaxed =
                new PaginatingDocumentLayoutEngine.PaginationPolicy(1, 1, 0);

        LaidOutDocument layout = engine(layouter, relaxed).layout(document, THEME);

        List<LaidOutTextBlock> bodyBlocks = layout.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .filter(LaidOutTextBlock.class::isInstance)
                .map(LaidOutTextBlock.class::cast)
                .filter(block -> block.role() == BlockRole.BODY)
                .toList();

        List<LaidOutTextBlock> targetBlocks = bodyBlocks.stream()
                .filter(block -> block.sourceParagraphIndex() == 1)
                .toList();

        assertEquals(2, targetBlocks.size());
        assertEquals(3, targetBlocks.get(0).lines().size());
        assertEquals(1, targetBlocks.get(1).lines().size());
    }

    @Test
    void stricterPaginationPolicyCanProtectThreeLineWidowsWithASatisfiablePolicy() {
        Document document = new Document(
                "TITLE",
                new PageStyle(220.0, 168.0, 20.0, 20.0, 20.0, 20.0),
                List.of(Paragraph.of("FILLER"), Paragraph.of("BODY-FIVE"))
        );

        ParagraphLayouter layouter = fixedLineLayouter(Map.of(
                "FILLER", fixedLines("FILLER", 1, 20.0),
                "BODY-FIVE", fixedLines("BODY-FIVE", 5, 20.0)
        ));

        PaginatingDocumentLayoutEngine.PaginationPolicy strict =
                new PaginatingDocumentLayoutEngine.PaginationPolicy(2, 3, 0);

        LaidOutDocument layout = engine(layouter, strict).layout(document, THEME);

        List<LaidOutTextBlock> bodyBlocks = layout.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .filter(LaidOutTextBlock.class::isInstance)
                .map(LaidOutTextBlock.class::cast)
                .filter(block -> block.role() == BlockRole.BODY)
                .toList();

        List<LaidOutTextBlock> targetBlocks = bodyBlocks.stream()
                .filter(block -> block.sourceParagraphIndex() == 1)
                .toList();

        assertEquals(2, targetBlocks.size());
        assertEquals(2, targetBlocks.get(0).lines().size());
        assertEquals(3, targetBlocks.get(1).lines().size());
    }


    @Test
    void verticalGlueCanStretchEligibleBodyGapsOnNonFinalPages() {
        ParagraphStyle bodyStyle = ParagraphStyle.defaultBody().withSpacingAfter(14.0);
        Document document = new Document(
                "TITLE",
                new PageStyle(220.0, 134.0, 20.0, 20.0, 20.0, 20.0),
                List.of(
                        Paragraph.of(bodyStyle, "BODY-ONE"),
                        Paragraph.of(bodyStyle, "BODY-TWO"),
                        Paragraph.of(bodyStyle, "BODY-THREE")
                )
        );

        ParagraphLayouter layouter = fixedLineLayouter(Map.of(
                "BODY-ONE", fixedLines("BODY-ONE", 1, 20.0),
                "BODY-TWO", fixedLines("BODY-TWO", 1, 20.0),
                "BODY-THREE", fixedLines("BODY-THREE", 3, 20.0)
        ));

        PaginatingDocumentLayoutEngine.PaginationPolicy policy =
                PaginatingDocumentLayoutEngine.PaginationPolicy.defaults()
                        .withVerticalGlue(new PaginatingDocumentLayoutEngine.VerticalGluePolicy(true, false, 10.0, 40.0));

        LaidOutDocument layout = engine(layouter, policy).layout(document, THEME);

        LaidOutPage firstPage = layout.pages().getFirst();
        List<LaidOutTextBlock> firstPageBody = firstPage.blocks().stream()
                .filter(LaidOutTextBlock.class::isInstance)
                .map(LaidOutTextBlock.class::cast)
                .filter(block -> block.role() == BlockRole.BODY)
                .toList();

        assertEquals(2, firstPageBody.size());
        LaidOutTextBlock secondBlock = firstPageBody.get(1);
        double contentBottom = document.pageStyle().height() - document.pageStyle().marginBottom();
        double blockBottom = secondBlock.y() + secondBlock.height();
        assertEquals(contentBottom, blockBottom, 0.0001);
    }

    @Test
    void verticalGlueRespectsPerGapStretchCapsAcrossMultipleGaps() {
        ParagraphStyle bodyStyle = ParagraphStyle.defaultBody().withSpacingAfter(14.0);
        Document document = new Document(
                "TITLE",
                new PageStyle(220.0, 188.0, 20.0, 20.0, 20.0, 20.0),
                List.of(
                        Paragraph.of(bodyStyle, "BODY-A"),
                        Paragraph.of(bodyStyle, "BODY-B"),
                        Paragraph.of(bodyStyle, "BODY-C"),
                        Paragraph.of(bodyStyle, "BODY-D")
                )
        );

        ParagraphLayouter layouter = fixedLineLayouter(Map.of(
                "BODY-A", fixedLines("BODY-A", 1, 20.0),
                "BODY-B", fixedLines("BODY-B", 1, 20.0),
                "BODY-C", fixedLines("BODY-C", 1, 20.0),
                "BODY-D", fixedLines("BODY-D", 4, 20.0)
        ));

        PaginatingDocumentLayoutEngine.PaginationPolicy policy =
                PaginatingDocumentLayoutEngine.PaginationPolicy.defaults()
                        .withVerticalGlue(new PaginatingDocumentLayoutEngine.VerticalGluePolicy(true, false, 10.0, 10.0));

        LaidOutDocument layout = engine(layouter, policy).layout(document, THEME);

        LaidOutPage firstPage = layout.pages().getFirst();
        List<LaidOutTextBlock> firstPageBody = firstPage.blocks().stream()
                .filter(LaidOutTextBlock.class::isInstance)
                .map(LaidOutTextBlock.class::cast)
                .filter(block -> block.role() == BlockRole.BODY)
                .toList();

        assertEquals(3, firstPageBody.size());
        assertEquals(20.0, firstPageBody.get(0).y(), 0.0001);
        assertEquals(64.0, firstPageBody.get(1).y(), 0.0001);
        assertEquals(108.0, firstPageBody.get(2).y(), 0.0001);
    }

    @Test
    void everyLaidOutLineHasCaretStopsForEachTextBoundary() {
        LaidOutDocument layout = layout(Document.sample());

        for (LaidOutPage page : layout.pages()) {
            for (LaidOutBlock block : page.blocks()) {
                if (block instanceof LaidOutTextBlock textBlock) {
                    for (LaidOutLine line : textBlock.lines()) {
                        assertEquals(line.text().length() + 1, line.caretStops().size(),
                                () -> "caret stops mismatch for line '" + line.text() + "'");
                    }
                }
            }
        }
    }

    @Test
    void hitTestRoundTripResolvesBackToTheSameLogicalPosition() {
        Document document = Document.sample();
        LaidOutDocument layout = layout(document);
        TextPosition original = new TextPosition(1, 5);

        CaretLocator locator = new CaretLocator();
        ResolvedTextPosition resolved = locator.resolve(layout, original);
        CaretGeometry caret = locator.locateCaret(layout, original);

        assertNotNull(resolved);
        assertNotNull(caret);

        HitTestResult hit = new PageHitTester().hitTest(
                resolved.page(),
                caret.x(),
                caret.y() + Math.max(1.0, caret.height() / 2.0)
        );

        assertNotNull(hit);
        assertEquals(original, hit.position());
    }

    private static LaidOutDocument layout(Document document) {
        return engine().layout(document, THEME);
    }

    private static DocumentLayoutEngine engine() {
        return engine(new GreedyParagraphLayouter());
    }

    private static DocumentLayoutEngine engine(ParagraphLayouter layouter) {
        return new PaginatingDocumentLayoutEngine(layouter);
    }

    private static DocumentLayoutEngine engine(
            ParagraphLayouter layouter,
            PaginatingDocumentLayoutEngine.PaginationPolicy paginationPolicy
    ) {
        return new PaginatingDocumentLayoutEngine(layouter, false, paginationPolicy);
    }

    private static ParagraphLayouter fixedLineLayouter(Map<String, List<LaidOutLine>> byText) {
        return (paragraph, baseFont, maxWidth, lineGap) -> {
            List<LaidOutLine> lines = byText.get(paragraph.plainText());
            if (lines == null) {
                throw new IllegalArgumentException("Unexpected paragraph text: " + paragraph.plainText());
            }
            return lines;
        };
    }

    private static List<LaidOutLine> fixedLines(String prefix, int lineCount, double lineHeight) {
        List<LaidOutLine> lines = new ArrayList<>(lineCount);
        int offset = 0;
        for (int i = 0; i < lineCount; i++) {
            String text = prefix + "-" + i;
            List<Double> caretStops = new ArrayList<>(text.length() + 1);
            for (int c = 0; c <= text.length(); c++) {
                caretStops.add((double) c * 8.0);
            }
            lines.add(new LaidOutLine(
                    text,
                    0.0,
                    i * lineHeight,
                    text.length() * 8.0,
                    lineHeight,
                    Math.max(1.0, lineHeight - 4.0),
                    offset,
                    offset + text.length(),
                    List.of(new LaidOutRun(text, 0, text.length(), 0.0, text.length() * 8.0, CharacterStyle.PLAIN)),
                    caretStops
            ));
            offset += text.length();
        }
        return List.copyOf(lines);
    }

    private static String longParagraph(int repetitions) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < repetitions; i++) {
            builder.append("Delos keeps paragraphs flowing across pages with stable logical mapping ");
        }
        return builder.toString();
    }
}
