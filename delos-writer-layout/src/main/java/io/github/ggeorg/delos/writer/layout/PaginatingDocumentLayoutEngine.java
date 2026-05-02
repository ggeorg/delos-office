package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.Block;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.FormulaBlock;
import io.github.ggeorg.delos.writer.document.HorizontalRuleBlock;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.document.ListMarkerKind;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.TableBlock;

import io.github.ggeorg.delos.render.RenderFont;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Real page-flow pagination layer for Delos.
 * <p>
 * v9 adds two important optimizations:
 * <ul>
 *     <li>paragraph line-layout caching for unchanged paragraphs</li>
 *     <li>incremental relayout from the first page affected by a document change</li>
 * </ul>
 */
public final class PaginatingDocumentLayoutEngine implements DocumentLayoutEngine {
    private static final double LIST_LEVEL_INDENT = 24.0;
    private static final double LIST_TEXT_INDENT = 28.0;
    private static final double TABLE_BLOCK_SPACING_AFTER = 6.0;
    private static final double IMAGE_BLOCK_SPACING_AFTER = 6.0;
    private static final double FORMULA_BLOCK_SPACING_AFTER = 6.0;
    private static final double HORIZONTAL_RULE_BLOCK_SPACING_AFTER = 6.0;
    private static final PaginationPolicy DEFAULT_PAGINATION_POLICY = PaginationPolicy.defaults();

    private final ParagraphLayouter paragraphLayouter;
    private final TableBlockLayouter tableBlockLayouter;
    private final ImageBlockLayouter imageBlockLayouter;
    private final FormulaBlockLayouter formulaBlockLayouter;
    private final HorizontalRuleBlockLayouter horizontalRuleBlockLayouter;
    private final boolean validateCache;
    private final PaginationPolicy paginationPolicy;
    private LayoutCache cache;

    public PaginatingDocumentLayoutEngine(ParagraphLayouter paragraphLayouter) {
        this(paragraphLayouter, false, DEFAULT_PAGINATION_POLICY);
    }

    public PaginatingDocumentLayoutEngine(ParagraphLayouter paragraphLayouter, boolean validateCache) {
        this(paragraphLayouter, validateCache, DEFAULT_PAGINATION_POLICY);
    }

    public PaginatingDocumentLayoutEngine(
        ParagraphLayouter paragraphLayouter,
        boolean validateCache,
        PaginationPolicy paginationPolicy
    ) {
        this.paragraphLayouter = Objects.requireNonNull(paragraphLayouter, "paragraphLayouter");
        this.tableBlockLayouter = new TableBlockLayouter(this.paragraphLayouter);
        this.imageBlockLayouter = new ImageBlockLayouter();
        this.formulaBlockLayouter = new FormulaBlockLayouter();
        this.horizontalRuleBlockLayouter = new HorizontalRuleBlockLayouter();
        this.validateCache = validateCache;
        this.paginationPolicy = Objects.requireNonNull(paginationPolicy, "paginationPolicy");
    }

    @Override
    public LaidOutDocument layout(Document document, LayoutTheme theme) {
        LayoutInputs inputs = LayoutInputs.from(document, theme);

        LayoutComputation incremental = layoutIncremental(document, theme, inputs);
        if (validateCache) {
            LayoutComputation cold = layoutCold(document, theme, inputs);
            if (!incremental.document().equals(cold.document())) {
                throw new IllegalStateException("Incremental layout cache mismatch: "
                    + describeFirstDifference(incremental.document(), cold.document()));
            }
        }

        cache = new LayoutCache(inputs, incremental.paragraphLayouts(), incremental.document());
        return incremental.document();
    }

    private LayoutComputation layoutIncremental(Document document, LayoutTheme theme, LayoutInputs inputs) {
        LayoutCache reusableCache = canReuseCache(inputs) ? cache : null;
        if (reusableCache != null && reusableCache.blocks().equals(inputs.blocks())) {
            return new LayoutComputation(reusableCache.paragraphLayouts(), reusableCache.document());
        }
        // v61 keeps rich-block cache invalidation conservative: documents that contain
        // non-paragraph blocks relayout from scratch after any change. Paragraph-only documents
        // keep the existing incremental cache path.
        if (reusableCache != null && containsNonParagraphBlock(inputs.blocks())) {
            reusableCache = null;
        }
        return layoutInternal(document, theme, inputs, reusableCache);
    }

    private LayoutComputation layoutCold(Document document, LayoutTheme theme, LayoutInputs inputs) {
        return layoutInternal(document, theme, inputs, null);
    }

    private LayoutComputation layoutInternal(
        Document document,
        LayoutTheme theme,
        LayoutInputs inputs,
        LayoutCache reusableCache
    ) {
        PageStyle pageStyle = document.pageStyle();

        int firstChangedParagraph = reusableCache == null
            ? 0
            : firstChangedParagraphIndex(reusableCache.paragraphs(), inputs.paragraphs());
        int relayoutAnchorParagraph = reusableCache == null
            ? 0
            : relayoutAnchorParagraph(reusableCache.paragraphs(), inputs.paragraphs(), firstChangedParagraph);

        int reusablePrefixPageCount = reusableCache == null
            ? 0
            : reusablePrefixPageCount(reusableCache.document(), relayoutAnchorParagraph);

        int relayoutStartParagraph = reusableCache == null
            ? 0
            : relayoutStartParagraphIndex(reusableCache.document(), reusablePrefixPageCount, relayoutAnchorParagraph);

        Map<ParagraphLayoutKey, List<LaidOutLine>> nextParagraphCache = reusableCache == null
            ? new HashMap<>()
            : new HashMap<>(reusableCache.paragraphLayouts());

        List<LaidOutPage> pages = new ArrayList<>();

        if (reusableCache != null && reusablePrefixPageCount > 0) {
            pages.addAll(reusableCache.document().pages().subList(0, reusablePrefixPageCount));
        }

        PaginationState state = reusablePrefixPageCount == 0
            ? PaginationState.firstPage(pageStyle)
            : PaginationState.subsequentPage(pageStyle, reusablePrefixPageCount);

        int[] numberedListCounters = new int[9];
        List<Paragraph> paragraphs = document.paragraphs();
        int paragraphIndex = 0;
        List<Block> documentBlocks = document.blocks();
        for (int sourceBlockIndex = 0; sourceBlockIndex < documentBlocks.size(); sourceBlockIndex++) {
            Block block = documentBlocks.get(sourceBlockIndex);
            if (block instanceof ParagraphBlock paragraphBlock) {
                if (paragraphIndex < relayoutStartParagraph) {
                    paragraphIndex += 1;
                    continue;
                }
                Paragraph paragraph = paragraphBlock.paragraph();
                ListLayout listLayout = listLayoutFor(pageStyle, paragraph, numberedListCounters);
                List<LaidOutLine> lines = layoutParagraph(paragraph, theme.bodyFont(), listLayout.contentWidth(), theme.bodyLineGap(), nextParagraphCache);
                appendParagraph(pageStyle, paragraphs, pages, state, paragraphIndex, paragraph, lines, listLayout);
                paragraphIndex += 1;
            } else if (block instanceof TableBlock tableBlock) {
                resetNumberedListCounters(numberedListCounters);
                appendTable(pageStyle, theme, pages, state, sourceBlockIndex, tableBlock);
            } else if (block instanceof ImageBlock imageBlock) {
                resetNumberedListCounters(numberedListCounters);
                appendImage(pageStyle, pages, state, sourceBlockIndex, imageBlock);
            } else if (block instanceof FormulaBlock formulaBlock) {
                resetNumberedListCounters(numberedListCounters);
                appendFormula(pageStyle, theme, pages, state, sourceBlockIndex, formulaBlock);
            } else if (block instanceof HorizontalRuleBlock horizontalRuleBlock) {
                resetNumberedListCounters(numberedListCounters);
                appendHorizontalRule(pageStyle, pages, state, sourceBlockIndex, horizontalRuleBlock);
            } else {
                resetNumberedListCounters(numberedListCounters);
            }
        }

        if (state.currentBlocks().isEmpty()) {
            state.currentBlocks().add(new LaidOutTextBlock(
                BlockRole.BODY,
                pageStyle.marginLeft(),
                pageStyle.marginTop(),
                pageStyle.contentWidth(),
                0,
                -1,
                0,
                true,
                true,
                List.of()
            ));
        }

        pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
        List<LaidOutPage> balancedPages = applyVerticalGlueIfEnabled(pageStyle, pages);
        LaidOutDocument laidOutDocument = new LaidOutDocument(pageStyle, balancedPages);
        return new LayoutComputation(nextParagraphCache, laidOutDocument);
    }

    private List<LaidOutPage> applyVerticalGlueIfEnabled(PageStyle pageStyle, List<LaidOutPage> pages) {
        VerticalGluePolicy policy = paginationPolicy.verticalGluePolicy();
        if (!policy.enabled() || pages.isEmpty()) {
            return List.copyOf(pages);
        }

        List<LaidOutPage> adjusted = new ArrayList<>(pages.size());
        for (int i = 0; i < pages.size(); i++) {
            boolean finalPage = i == pages.size() - 1;
            if (finalPage && !policy.stretchFinalPage()) {
                adjusted.add(pages.get(i));
                continue;
            }
            adjusted.add(applyVerticalGlueToPage(pageStyle, pages.get(i), policy));
        }
        return List.copyOf(adjusted);
    }

    private LaidOutPage applyVerticalGlueToPage(PageStyle pageStyle, LaidOutPage page, VerticalGluePolicy policy) {
        double contentBottom = pageStyle.height() - pageStyle.marginBottom();
        double pageBottom = maxBlockBottom(page.blocks());
        double slack = contentBottom - pageBottom;
        if (slack < policy.minSlackThreshold()) {
            return page;
        }

        List<GapTarget> gapTargets = eligibleVerticalGlueTargets(page.blocks());
        if (gapTargets.isEmpty()) {
            return page;
        }

        double[] stretchByGap = distributeVerticalSlack(slack, gapTargets.size(), policy.maxStretchPerGap());
        double applied = 0.0;
        for (double value : stretchByGap) {
            applied += value;
        }
        if (applied <= 0.0) {
            return page;
        }

        List<LaidOutBlock> shiftedBlocks = new ArrayList<>(page.blocks().size());
        double cumulativeShift = 0.0;
        int gapCursor = 0;
        for (int blockIndex = 0; blockIndex < page.blocks().size(); blockIndex++) {
            while (gapCursor < gapTargets.size() && gapTargets.get(gapCursor).beforeBlockIndex() == blockIndex) {
                cumulativeShift += stretchByGap[gapCursor];
                gapCursor += 1;
            }
            shiftedBlocks.add(shiftBlock(page.blocks().get(blockIndex), cumulativeShift));
        }

        return new LaidOutPage(page.pageIndex(), page.width(), page.height(), shiftedBlocks);
    }

    private double maxBlockBottom(List<LaidOutBlock> blocks) {
        double max = 0.0;
        for (LaidOutBlock block : blocks) {
            if (block instanceof LaidOutTextBlock textBlock) {
                max = Math.max(max, textBlock.y() + textBlock.height());
            } else if (block instanceof LaidOutAtomicBlock atomicBlock) {
                max = Math.max(max, atomicBlock.y() + atomicBlock.height());
            }
        }
        return max;
    }

    private List<GapTarget> eligibleVerticalGlueTargets(List<LaidOutBlock> blocks) {
        List<GapTarget> targets = new ArrayList<>();
        for (int i = 1; i < blocks.size(); i++) {
            LaidOutBlock previous = blocks.get(i - 1);
            LaidOutBlock current = blocks.get(i);
            if (!(previous instanceof LaidOutTextBlock prevText) || !(current instanceof LaidOutTextBlock currText)) {
                continue;
            }
            if (prevText.role() != BlockRole.BODY || currText.role() != BlockRole.BODY) {
                continue;
            }
            if (prevText.sourceParagraphIndex() < 0 || currText.sourceParagraphIndex() < 0) {
                continue;
            }
            if (prevText.sourceParagraphIndex() == currText.sourceParagraphIndex()) {
                continue;
            }
            if (prevText.isContinuationFragment() || currText.isContinuationFragment()) {
                continue;
            }
            double gap = currText.y() - (prevText.y() + prevText.height());
            if (gap > 0.0) {
                targets.add(new GapTarget(i, gap));
            }
        }
        return List.copyOf(targets);
    }

    private double[] distributeVerticalSlack(double slack, int gapCount, double maxStretchPerGap) {
        double[] stretches = new double[gapCount];
        if (gapCount == 0 || slack <= 0.0 || maxStretchPerGap <= 0.0) {
            return stretches;
        }

        double remaining = slack;
        boolean progress;
        do {
            progress = false;
            int activeCount = 0;
            for (double stretch : stretches) {
                if (stretch + 1e-9 < maxStretchPerGap) {
                    activeCount++;
                }
            }
            if (activeCount == 0) {
                break;
            }

            double share = remaining / activeCount;
            if (share <= 0.0) {
                break;
            }

            for (int i = 0; i < stretches.length; i++) {
                double room = maxStretchPerGap - stretches[i];
                if (room <= 1e-9) {
                    continue;
                }
                double delta = Math.min(room, share);
                if (delta > 0.0) {
                    stretches[i] += delta;
                    remaining -= delta;
                    progress = true;
                }
            }
        } while (progress && remaining > 1e-9);

        return stretches;
    }

    private LaidOutBlock shiftBlock(LaidOutBlock block, double deltaY) {
        if (deltaY == 0.0) {
            return block;
        }
        if (block instanceof LaidOutTextBlock textBlock) {
            return new LaidOutTextBlock(
                textBlock.role(),
                textBlock.x(),
                textBlock.y() + deltaY,
                textBlock.width(),
                textBlock.height(),
                textBlock.sourceParagraphIndex(),
                textBlock.startLineIndex(),
                textBlock.firstFragment(),
                textBlock.lastFragment(),
                textBlock.lines(),
                textBlock.listMarker()
            );
        }
        if (block instanceof LaidOutAtomicBlock atomicBlock) {
            return atomicBlock.withY(atomicBlock.y() + deltaY);
        }
        return block;
    }

    private String describeFirstDifference(LaidOutDocument incremental, LaidOutDocument cold) {
        if (!Objects.equals(incremental.pageStyle(), cold.pageStyle())) {
            return "page style differs";
        }
        if (incremental.pages().size() != cold.pages().size()) {
            return "page count differs: " + incremental.pages().size() + " vs " + cold.pages().size();
        }

        for (int pageIndex = 0; pageIndex < incremental.pages().size(); pageIndex++) {
            LaidOutPage incPage = incremental.pages().get(pageIndex);
            LaidOutPage coldPage = cold.pages().get(pageIndex);
            if (incPage.blocks().size() != coldPage.blocks().size()) {
                return "page " + pageIndex + " block count differs: "
                    + incPage.blocks().size() + " vs " + coldPage.blocks().size();
            }

            for (int blockIndex = 0; blockIndex < incPage.blocks().size(); blockIndex++) {
                LaidOutBlock incBlock = incPage.blocks().get(blockIndex);
                LaidOutBlock coldBlock = coldPage.blocks().get(blockIndex);
                if (!Objects.equals(incBlock, coldBlock)) {
                    return "page " + pageIndex + ", block " + blockIndex + " differs";
                }
            }
        }

        return "unknown difference";
    }

    private boolean canReuseCache(LayoutInputs inputs) {
        if (cache == null) {
            return false;
        }
        return cache.inputs().compatibleWith(inputs);
    }

    private boolean containsNonParagraphBlock(List<Block> blocks) {
        for (Block block : blocks) {
            if (!(block instanceof ParagraphBlock)) {
                return true;
            }
        }
        return false;
    }

    private int firstChangedParagraphIndex(List<Paragraph> previousParagraphs, List<Paragraph> currentParagraphs) {
        int limit = Math.min(previousParagraphs.size(), currentParagraphs.size());
        for (int i = 0; i < limit; i++) {
            if (!Objects.equals(previousParagraphs.get(i), currentParagraphs.get(i))) {
                return i;
            }
        }
        return limit;
    }

    private int relayoutAnchorParagraph(List<Paragraph> previousParagraphs, List<Paragraph> currentParagraphs, int firstChangedParagraph) {
        if (previousParagraphs.equals(currentParagraphs)) {
            return firstChangedParagraph;
        }
        if (firstChangedParagraph <= 0) {
            return 0;
        }
        return firstChangedParagraph - 1;
    }

    private int reusablePrefixPageCount(LaidOutDocument previousDocument, int relayoutAnchorParagraph) {
        if (previousDocument == null || relayoutAnchorParagraph <= 0) {
            return 0;
        }

        int reusableCount = 0;
        for (LaidOutPage page : previousDocument.pages()) {
            int maxParagraphIndex = maxBodyParagraphIndex(page);
            if (maxParagraphIndex >= 0 && maxParagraphIndex < relayoutAnchorParagraph) {
                reusableCount++;
                continue;
            }
            if (maxParagraphIndex < 0 && reusableCount == 0) {
                reusableCount++;
                continue;
            }
            break;
        }
        return reusableCount;
    }

    private int relayoutStartParagraphIndex(LaidOutDocument previousDocument, int reusablePrefixPageCount, int relayoutAnchorParagraph) {
        if (previousDocument == null || reusablePrefixPageCount <= 0) {
            return 0;
        }
        if (reusablePrefixPageCount >= previousDocument.pages().size()) {
            return relayoutAnchorParagraph;
        }

        LaidOutPage firstDirtyPage = previousDocument.pages().get(reusablePrefixPageCount);
        int minParagraphIndex = minBodyParagraphIndex(firstDirtyPage);
        return minParagraphIndex >= 0 ? Math.min(minParagraphIndex, relayoutAnchorParagraph) : relayoutAnchorParagraph;
    }

    private int minBodyParagraphIndex(LaidOutPage page) {
        int min = Integer.MAX_VALUE;
        for (LaidOutBlock block : page.blocks()) {
            if (block instanceof LaidOutTextBlock textBlock && textBlock.role() == BlockRole.BODY && textBlock.sourceParagraphIndex() >= 0) {
                min = Math.min(min, textBlock.sourceParagraphIndex());
            }
        }
        return min == Integer.MAX_VALUE ? -1 : min;
    }

    private int maxBodyParagraphIndex(LaidOutPage page) {
        int max = -1;
        for (LaidOutBlock block : page.blocks()) {
            if (block instanceof LaidOutTextBlock textBlock && textBlock.role() == BlockRole.BODY) {
                max = Math.max(max, textBlock.sourceParagraphIndex());
            }
        }
        return max;
    }

    private ListLayout listLayoutFor(PageStyle pageStyle, Paragraph paragraph, int[] numberedListCounters) {
        if (!paragraph.style().isListItem()) {
            for (int i = 0; i < numberedListCounters.length; i++) {
                numberedListCounters[i] = 0;
            }
            return ListLayout.none(pageStyle);
        }

        int level = Math.min(paragraph.style().listStyle().level(), numberedListCounters.length - 1);
        double markerIndent = level * LIST_LEVEL_INDENT;
        double bodyIndent = markerIndent + LIST_TEXT_INDENT;
        String markerText;
        if (paragraph.style().listStyle().kind() == ListMarkerKind.NUMBERED) {
            if (numberedListCounters[level] <= 0) {
                numberedListCounters[level] = paragraph.style().listStyle().start();
            }
            markerText = numberedListCounters[level]++ + ".";
        } else {
            markerText = "•";
        }
        for (int i = level + 1; i < numberedListCounters.length; i++) {
            numberedListCounters[i] = 0;
        }
        double contentWidth = Math.max(1, pageStyle.contentWidth() - bodyIndent);
        return new ListLayout(true, markerText, pageStyle.marginLeft() + bodyIndent, contentWidth, -LIST_TEXT_INDENT);
    }

    private LaidOutListMarker listMarkerFor(ListLayout listLayout, List<LaidOutLine> fragmentLines, boolean firstFragment, int nextLineIndex) {
        if (!listLayout.enabled() || !firstFragment || nextLineIndex != 0 || fragmentLines.isEmpty()) {
            return LaidOutListMarker.none();
        }
        LaidOutLine firstLine = fragmentLines.get(0);
        double baselineY = firstLine.y() + firstLine.baseline();
        return new LaidOutListMarker(listLayout.markerText(), listLayout.markerX(), baselineY, LIST_TEXT_INDENT);
    }

    private record ListLayout(boolean enabled, String markerText, double blockX, double contentWidth, double markerX) {
        static ListLayout none(PageStyle pageStyle) {
            return new ListLayout(false, "", pageStyle.marginLeft(), pageStyle.contentWidth(), 0);
        }
    }

    private List<LaidOutLine> layoutParagraph(
        Paragraph paragraph,
        RenderFont font,
        double maxWidth,
        double lineGap,
        Map<ParagraphLayoutKey, List<LaidOutLine>> paragraphCache
    ) {
        ParagraphLayoutKey key = ParagraphLayoutKey.of(paragraph, font, maxWidth, lineGap);
        List<LaidOutLine> cached = paragraphCache.get(key);
        if (cached != null) {
            return cached;
        }

        List<LaidOutLine> computed = paragraphLayouter.layoutLines(paragraph, font, maxWidth, lineGap);
        paragraphCache.put(key, computed);
        return computed;
    }

    private void appendParagraph(
        PageStyle pageStyle,
        List<Paragraph> paragraphs,
        List<LaidOutPage> pages,
        PaginationState state,
        int paragraphIndex,
        Paragraph paragraph,
        List<LaidOutLine> lines,
        ListLayout listLayout
    ) {
        double contentBottom = pageStyle.height() - pageStyle.marginBottom();
        int nextLineIndex = 0;
        boolean appliedSpacingBefore = false;

        while (nextLineIndex < lines.size()) {
            if (!appliedSpacingBefore && paragraph.style().spacingBefore() > 0 && paragraphIndex > 0) {
                if (state.currentBlocks().isEmpty()) {
                    // Suppress paragraph spacing-before at the top of a page.
                    appliedSpacingBefore = true;
                } else if (state.cursorY() + paragraph.style().spacingBefore() <= contentBottom) {
                    state.cursorY(state.cursorY() + paragraph.style().spacingBefore());
                    appliedSpacingBefore = true;
                } else {
                    pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
                    state.advanceToNextPage(pageStyle);
                    continue;
                }
            }

            if (state.cursorY() >= contentBottom && !state.currentBlocks().isEmpty()) {
                pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
                state.advanceToNextPage(pageStyle);
                continue;
            }

            int fragmentEndExclusive = chooseFragmentEndExclusive(
                lines,
                nextLineIndex,
                state.cursorY(),
                contentBottom,
                pageStyle.contentHeight(),
                !state.currentBlocks().isEmpty()
            );
            if (fragmentEndExclusive <= nextLineIndex) {
                if (!state.currentBlocks().isEmpty()) {
                    pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
                    state.advanceToNextPage(pageStyle);
                    continue;
                }

                fragmentEndExclusive = Math.min(nextLineIndex + 1, lines.size());
            }

            List<LaidOutLine> fragmentLines = sliceAndNormalizeLines(lines, nextLineIndex, fragmentEndExclusive);
            double fragmentHeight = blockHeight(fragmentLines);
            boolean firstFragment = nextLineIndex == 0;
            boolean lastFragment = fragmentEndExclusive == lines.size();

            state.currentBlocks().add(new LaidOutTextBlock(
                BlockRole.BODY,
                listLayout.blockX(),
                state.cursorY(),
                listLayout.contentWidth(),
                fragmentHeight,
                paragraphIndex,
                nextLineIndex,
                firstFragment,
                lastFragment,
                fragmentLines,
                listMarkerFor(listLayout, fragmentLines, firstFragment, nextLineIndex)
            ));
            state.cursorY(state.cursorY() + fragmentHeight);
            nextLineIndex = fragmentEndExclusive;

            if (lastFragment) {
                double spacingAfter = paragraph.style().spacingAfter();
                if (paragraphIndex < paragraphs.size() - 1) {
                    if (state.cursorY() + spacingAfter <= contentBottom) {
                        state.cursorY(state.cursorY() + spacingAfter);
                    } else if (!state.currentBlocks().isEmpty()) {
                        pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
                        state.advanceToNextPage(pageStyle);
                    }
                }
            } else {
                pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
                state.advanceToNextPage(pageStyle);
            }
        }
    }

    private int chooseFragmentEndExclusive(
        List<LaidOutLine> lines,
        int startLineIndex,
        double cursorY,
        double contentBottom,
        double freshPageContentHeight,
        boolean canBreakBeforeFragment
    ) {
        int bestEndExclusive = startLineIndex;

        for (int endExclusive = startLineIndex + 1; endExclusive <= lines.size(); endExclusive++) {
            double fragmentHeight = fragmentHeight(lines, startLineIndex, endExclusive);
            if (cursorY + fragmentHeight <= contentBottom) {
                bestEndExclusive = endExclusive;
            } else {
                break;
            }
        }

        int keepTogetherCandidate = keepShortParagraphTogetherWhenPossible(
            lines,
            startLineIndex,
            bestEndExclusive,
            freshPageContentHeight,
            canBreakBeforeFragment
        );
        if (keepTogetherCandidate != bestEndExclusive) {
            return keepTogetherCandidate;
        }

        return enforceWidowOrphanControl(lines.size(), startLineIndex, bestEndExclusive, canBreakBeforeFragment);
    }

    private int keepShortParagraphTogetherWhenPossible(
        List<LaidOutLine> lines,
        int startLineIndex,
        int bestEndExclusive,
        double freshPageContentHeight,
        boolean canBreakBeforeFragment
    ) {
        if (!canBreakBeforeFragment || startLineIndex != 0) {
            return bestEndExclusive;
        }

        int totalLineCount = lines.size();
        if (totalLineCount <= 0 || totalLineCount > paginationPolicy.keepTogetherMaxLines()) {
            return bestEndExclusive;
        }

        if (bestEndExclusive >= totalLineCount) {
            return bestEndExclusive;
        }

        double fullParagraphHeight = fragmentHeight(lines, 0, totalLineCount);
        if (fullParagraphHeight <= freshPageContentHeight) {
            return startLineIndex;
        }

        return bestEndExclusive;
    }

    private int enforceWidowOrphanControl(
        int totalLineCount,
        int startLineIndex,
        int bestEndExclusive,
        boolean canBreakBeforeFragment
    ) {
        if (bestEndExclusive <= startLineIndex || bestEndExclusive >= totalLineCount) {
            return bestEndExclusive;
        }

        int adjustedEndExclusive = bestEndExclusive;
        int remainingLineCount = totalLineCount - adjustedEndExclusive;
        if (remainingLineCount > 0 && remainingLineCount < paginationPolicy.minWidowLines()) {
            adjustedEndExclusive = Math.max(startLineIndex, totalLineCount - paginationPolicy.minWidowLines());
        }

        int placedLineCount = adjustedEndExclusive - startLineIndex;
        int adjustedRemainingLineCount = totalLineCount - adjustedEndExclusive;

        if (startLineIndex == 0 && adjustedRemainingLineCount > 0 && placedLineCount < paginationPolicy.minOrphanLines()) {
            return canBreakBeforeFragment ? startLineIndex : bestEndExclusive;
        }

        if (adjustedEndExclusive <= startLineIndex) {
            return canBreakBeforeFragment ? startLineIndex : bestEndExclusive;
        }

        return adjustedEndExclusive;
    }

    private double fragmentHeight(List<LaidOutLine> lines, int startInclusive, int endExclusive) {
        if (startInclusive >= endExclusive) {
            return 0;
        }

        double baseY = lines.get(startInclusive).y();
        LaidOutLine last = lines.get(endExclusive - 1);
        return (last.y() - baseY) + last.height();
    }

    private List<LaidOutLine> sliceAndNormalizeLines(
        List<LaidOutLine> lines,
        int startInclusive,
        int endExclusive
    ) {
        List<LaidOutLine> fragment = new ArrayList<>(endExclusive - startInclusive);
        if (startInclusive >= endExclusive) {
            return fragment;
        }

        double baseY = lines.get(startInclusive).y();
        for (int i = startInclusive; i < endExclusive; i++) {
            LaidOutLine line = lines.get(i);
            fragment.add(new LaidOutLine(
                line.text(),
                line.x(),
                line.y() - baseY,
                line.width(),
                line.height(),
                line.baseline(),
                line.startOffset(),
                line.endOffset(),
                line.runs(),
                line.caretStops(),
                line.caretOffsets()
            ));
        }

        return fragment;
    }

    private double blockHeight(List<LaidOutLine> lines) {
        if (lines.isEmpty()) {
            return 0;
        }

        LaidOutLine last = lines.get(lines.size() - 1);
        return last.y() + last.height();
    }

    private static final class PaginationState {
        private int pageIndex;
        private double cursorY;
        private List<LaidOutBlock> currentBlocks;

        private PaginationState(int pageIndex, double cursorY, List<LaidOutBlock> currentBlocks) {
            this.pageIndex = pageIndex;
            this.cursorY = cursorY;
            this.currentBlocks = currentBlocks;
        }

        static PaginationState firstPage(PageStyle pageStyle) {
            return new PaginationState(0, pageStyle.marginTop(), new ArrayList<>());
        }

        static PaginationState subsequentPage(PageStyle pageStyle, int pageIndex) {
            return new PaginationState(pageIndex, pageStyle.marginTop(), new ArrayList<>());
        }

        int pageIndex() {
            return pageIndex;
        }

        double cursorY() {
            return cursorY;
        }

        void cursorY(double cursorY) {
            this.cursorY = cursorY;
        }

        List<LaidOutBlock> currentBlocks() {
            return currentBlocks;
        }

        void advanceToNextPage(PageStyle pageStyle) {
            pageIndex += 1;
            cursorY = pageStyle.marginTop();
            currentBlocks = new ArrayList<>();
        }
    }
    private void resetNumberedListCounters(int[] numberedListCounters) {
        for (int i = 0; i < numberedListCounters.length; i++) {
            numberedListCounters[i] = 0;
        }
    }

    private void appendImage(
        PageStyle pageStyle,
        List<LaidOutPage> pages,
        PaginationState state,
        int sourceBlockIndex,
        ImageBlock imageBlock
    ) {
        double contentBottom = pageStyle.height() - pageStyle.marginBottom();
        LaidOutImageBlock laidOutImage = layoutImage(sourceBlockIndex, imageBlock, pageStyle, state.cursorY());

        if (!state.currentBlocks().isEmpty() && state.cursorY() + laidOutImage.height() > contentBottom) {
            pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
            state.advanceToNextPage(pageStyle);
            laidOutImage = layoutImage(sourceBlockIndex, imageBlock, pageStyle, state.cursorY());
        }

        state.currentBlocks().add(laidOutImage);
        state.cursorY(state.cursorY() + laidOutImage.height());
        if (state.cursorY() + IMAGE_BLOCK_SPACING_AFTER <= contentBottom) {
            state.cursorY(state.cursorY() + IMAGE_BLOCK_SPACING_AFTER);
        }
    }
    private void appendFormula(
        PageStyle pageStyle,
        LayoutTheme theme,
        List<LaidOutPage> pages,
        PaginationState state,
        int sourceBlockIndex,
        FormulaBlock formulaBlock
    ) {
        double contentBottom = pageStyle.height() - pageStyle.marginBottom();
        LaidOutFormulaBlock laidOutFormula = layoutFormula(sourceBlockIndex, formulaBlock, pageStyle, state.cursorY(), theme);

        if (!state.currentBlocks().isEmpty() && state.cursorY() + laidOutFormula.height() > contentBottom) {
            pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
            state.advanceToNextPage(pageStyle);
            laidOutFormula = layoutFormula(sourceBlockIndex, formulaBlock, pageStyle, state.cursorY(), theme);
        }

        state.currentBlocks().add(laidOutFormula);
        state.cursorY(state.cursorY() + laidOutFormula.height());
        if (state.cursorY() + FORMULA_BLOCK_SPACING_AFTER <= contentBottom) {
            state.cursorY(state.cursorY() + FORMULA_BLOCK_SPACING_AFTER);
        }
    }
    private void appendHorizontalRule(
        PageStyle pageStyle,
        List<LaidOutPage> pages,
        PaginationState state,
        int sourceBlockIndex,
        HorizontalRuleBlock horizontalRuleBlock
    ) {
        double contentBottom = pageStyle.height() - pageStyle.marginBottom();
        LaidOutSeparator laidOutRule = layoutHorizontalRule(sourceBlockIndex, horizontalRuleBlock, pageStyle, state.cursorY());

        if (!state.currentBlocks().isEmpty() && state.cursorY() + laidOutRule.height() > contentBottom) {
            pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
            state.advanceToNextPage(pageStyle);
            laidOutRule = layoutHorizontalRule(sourceBlockIndex, horizontalRuleBlock, pageStyle, state.cursorY());
        }

        state.currentBlocks().add(laidOutRule);
        state.cursorY(state.cursorY() + laidOutRule.height());
        if (state.cursorY() + HORIZONTAL_RULE_BLOCK_SPACING_AFTER <= contentBottom) {
            state.cursorY(state.cursorY() + HORIZONTAL_RULE_BLOCK_SPACING_AFTER);
        }
    }
    private void appendTable(
        PageStyle pageStyle,
        LayoutTheme theme,
        List<LaidOutPage> pages,
        PaginationState state,
        int sourceBlockIndex,
        TableBlock tableBlock
    ) {
        double contentBottom = pageStyle.height() - pageStyle.marginBottom();
        LaidOutTableBlock fullTable = layoutTable(
                sourceBlockIndex,
                tableBlock,
                pageStyle.marginLeft(),
                0.0,
                pageStyle.contentWidth(),
                theme
        );

        int headerRowCount = Math.min(tableBlock.headerRowCount(), fullTable.rows().size());
        int nextRowIndex = 0;
        boolean firstFragment = true;

        while (nextRowIndex < fullTable.rows().size()) {
            double minimumFragmentHeight = minimumTableFragmentHeight(fullTable, headerRowCount, nextRowIndex, firstFragment);
            if (!state.currentBlocks().isEmpty() && state.cursorY() + minimumFragmentHeight > contentBottom) {
                pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
                state.advanceToNextPage(pageStyle);
                continue;
            }

            double availableHeight = Math.max(0.0, contentBottom - state.cursorY());
            TableFragment fragment = tableFragment(fullTable, headerRowCount, nextRowIndex, firstFragment, availableHeight);
            if (fragment.rows().isEmpty()) {
                if (!state.currentBlocks().isEmpty()) {
                    pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
                    state.advanceToNextPage(pageStyle);
                    continue;
                }
                fragment = forcedSingleRowTableFragment(fullTable, headerRowCount, nextRowIndex, firstFragment);
            }

            state.currentBlocks().add(new LaidOutTableBlock(
                    sourceBlockIndex,
                    fullTable.x(),
                    state.cursorY(),
                    fullTable.width(),
                    fragment.height(),
                    fragment.rows()
            ));
            state.cursorY(state.cursorY() + fragment.height());
            nextRowIndex = fragment.nextRowIndex();
            firstFragment = false;

            if (nextRowIndex < fullTable.rows().size()) {
                pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));
                state.advanceToNextPage(pageStyle);
            }
        }

        if (state.cursorY() + TABLE_BLOCK_SPACING_AFTER <= contentBottom) {
            state.cursorY(state.cursorY() + TABLE_BLOCK_SPACING_AFTER);
        }
    }

    private TableFragment tableFragment(
        LaidOutTableBlock fullTable,
        int headerRowCount,
        int nextRowIndex,
        boolean firstFragment,
        double availableHeight
    ) {
        List<LaidOutTableRow> rows = new ArrayList<>();
        double rowY = 0.0;

        if (!firstFragment && headerRowCount > 0) {
            for (int rowIndex = 0; rowIndex < headerRowCount; rowIndex++) {
                LaidOutTableRow headerRow = fullTable.rows().get(rowIndex);
                rows.add(tableRowAtY(headerRow, rowY));
                rowY += headerRow.height();
            }
        }

        int sourceRowIndex = firstFragment ? nextRowIndex : Math.max(nextRowIndex, headerRowCount);
        int placedSourceRows = 0;
        while (sourceRowIndex < fullTable.rows().size()) {
            LaidOutTableRow row = fullTable.rows().get(sourceRowIndex);
            if (placedSourceRows > 0 && rowY + row.height() > availableHeight) {
                break;
            }
            if (placedSourceRows == 0 && rowY + row.height() > availableHeight && !rows.isEmpty()) {
                break;
            }
            if (placedSourceRows == 0 && rows.isEmpty() && rowY + row.height() > availableHeight) {
                rows.add(tableRowAtY(row, rowY));
                rowY += row.height();
                sourceRowIndex += 1;
                placedSourceRows += 1;
                break;
            }

            rows.add(tableRowAtY(row, rowY));
            rowY += row.height();
            sourceRowIndex += 1;
            placedSourceRows += 1;
        }

        if (placedSourceRows == 0 && !firstFragment && nextRowIndex >= fullTable.rows().size()) {
            return new TableFragment(rows, nextRowIndex, rowY);
        }
        if (placedSourceRows == 0) {
            return new TableFragment(List.of(), nextRowIndex, 0.0);
        }
        return new TableFragment(rows, sourceRowIndex, rowY);
    }

    private TableFragment forcedSingleRowTableFragment(
        LaidOutTableBlock fullTable,
        int headerRowCount,
        int nextRowIndex,
        boolean firstFragment
    ) {
        List<LaidOutTableRow> rows = new ArrayList<>();
        double rowY = 0.0;
        if (!firstFragment && headerRowCount > 0) {
            for (int rowIndex = 0; rowIndex < headerRowCount; rowIndex++) {
                LaidOutTableRow headerRow = fullTable.rows().get(rowIndex);
                rows.add(tableRowAtY(headerRow, rowY));
                rowY += headerRow.height();
            }
        }

        int sourceRowIndex = firstFragment ? nextRowIndex : Math.max(nextRowIndex, headerRowCount);
        if (sourceRowIndex < fullTable.rows().size()) {
            LaidOutTableRow row = fullTable.rows().get(sourceRowIndex);
            rows.add(tableRowAtY(row, rowY));
            rowY += row.height();
            sourceRowIndex += 1;
        }
        return new TableFragment(rows, sourceRowIndex, rowY);
    }

    private double minimumTableFragmentHeight(
        LaidOutTableBlock fullTable,
        int headerRowCount,
        int nextRowIndex,
        boolean firstFragment
    ) {
        int sourceRowIndex = firstFragment ? nextRowIndex : Math.max(nextRowIndex, headerRowCount);
        double height = 0.0;
        if (firstFragment && sourceRowIndex < headerRowCount) {
            for (int rowIndex = sourceRowIndex; rowIndex < headerRowCount; rowIndex++) {
                height += fullTable.rows().get(rowIndex).height();
            }
            if (headerRowCount < fullTable.rows().size()) {
                height += fullTable.rows().get(headerRowCount).height();
            }
            return height;
        }
        if (!firstFragment) {
            for (int rowIndex = 0; rowIndex < headerRowCount; rowIndex++) {
                height += fullTable.rows().get(rowIndex).height();
            }
        }
        if (sourceRowIndex < fullTable.rows().size()) {
            height += fullTable.rows().get(sourceRowIndex).height();
        }
        return height;
    }

    private LaidOutTableRow tableRowAtY(LaidOutTableRow row, double y) {
        double deltaY = y - row.y();
        List<LaidOutTableCell> cells = new ArrayList<>(row.cells().size());
        for (LaidOutTableCell cell : row.cells()) {
            cells.add(new LaidOutTableCell(
                    cell.x(),
                    cell.y() + deltaY,
                    cell.width(),
                    cell.height(),
                    cell.textBlocks(),
                    cell.header(),
                    cell.backgroundColor()
            ));
        }
        return new LaidOutTableRow(y, row.height(), cells);
    }

    private record TableFragment(List<LaidOutTableRow> rows, int nextRowIndex, double height) {
        TableFragment {
            rows = List.copyOf(rows);
        }
    }

    private LaidOutTableBlock layoutTable(int sourceBlockIndex, TableBlock tableBlock, double x, double y, double width, LayoutTheme theme) {
        return tableBlockLayouter.layout(sourceBlockIndex, tableBlock, x, y, width, theme);
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

    public record PaginationPolicy(
        int minOrphanLines,
        int minWidowLines,
        int keepTogetherMaxLines,
        VerticalGluePolicy verticalGluePolicy
    ) {
        public PaginationPolicy(int minOrphanLines, int minWidowLines, int keepTogetherMaxLines) {
            this(minOrphanLines, minWidowLines, keepTogetherMaxLines, VerticalGluePolicy.disabled());
        }

        public PaginationPolicy {
            if (minOrphanLines < 1) {
                throw new IllegalArgumentException("minOrphanLines must be >= 1");
            }
            if (minWidowLines < 1) {
                throw new IllegalArgumentException("minWidowLines must be >= 1");
            }
            if (keepTogetherMaxLines < 0) {
                throw new IllegalArgumentException("keepTogetherMaxLines must be >= 0");
            }
            verticalGluePolicy = Objects.requireNonNullElse(verticalGluePolicy, VerticalGluePolicy.disabled());
        }

        public static PaginationPolicy defaults() {
            return new PaginationPolicy(2, 2, 4, VerticalGluePolicy.disabled());
        }

        public static PaginationPolicy relaxed() {
            return new PaginationPolicy(1, 1, 0, VerticalGluePolicy.disabled());
        }

        public PaginationPolicy withVerticalGlue(VerticalGluePolicy verticalGluePolicy) {
            return new PaginationPolicy(minOrphanLines, minWidowLines, keepTogetherMaxLines, verticalGluePolicy);
        }
    }

    public record VerticalGluePolicy(
        boolean enabled,
        boolean stretchFinalPage,
        double minSlackThreshold,
        double maxStretchPerGap
    ) {
        public VerticalGluePolicy {
            if (minSlackThreshold < 0.0) {
                throw new IllegalArgumentException("minSlackThreshold must be >= 0");
            }
            if (maxStretchPerGap < 0.0) {
                throw new IllegalArgumentException("maxStretchPerGap must be >= 0");
            }
        }

        public static VerticalGluePolicy disabled() {
            return new VerticalGluePolicy(false, false, 0.0, 0.0);
        }

        public static VerticalGluePolicy conservative() {
            return new VerticalGluePolicy(true, false, 10.0, 18.0);
        }
    }

    private record GapTarget(int beforeBlockIndex, double gapHeight) { }

}
