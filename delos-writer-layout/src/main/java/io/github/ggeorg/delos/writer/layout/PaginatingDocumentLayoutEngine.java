package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.render.RenderFont;
import io.github.ggeorg.delos.writer.document.Block;
import io.github.ggeorg.delos.writer.document.Document;
import io.github.ggeorg.delos.writer.document.FormulaBlock;
import io.github.ggeorg.delos.writer.document.HorizontalRuleBlock;
import io.github.ggeorg.delos.writer.document.ImageBlock;
import io.github.ggeorg.delos.writer.document.PageStyle;
import io.github.ggeorg.delos.writer.document.Paragraph;
import io.github.ggeorg.delos.writer.document.ParagraphBlock;
import io.github.ggeorg.delos.writer.document.TableBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Page-flow pagination engine for Delos Writer.
 *
 * <p>This class now acts as the orchestration layer: it plans incremental reuse,
 * delegates paragraph/table/atomic-block pagination, and assembles the final
 * document layout. The detailed mechanics live in small package-private helper
 * classes so they can be optimized and tested independently.</p>
 */
public final class PaginatingDocumentLayoutEngine implements DocumentLayoutEngine {
    private static final PaginationPolicy DEFAULT_PAGINATION_POLICY = PaginationPolicy.defaults();

    private final ParagraphLayouter paragraphLayouter;
    private final TablePaginator tablePaginator;
    private final AtomicBlockPaginator atomicBlockPaginator;
    private final ParagraphPaginator paragraphPaginator;
    private final IncrementalLayoutPlanner incrementalLayoutPlanner;
    private final VerticalGlueDistributor verticalGlueDistributor;
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
        this.validateCache = validateCache;
        this.paginationPolicy = Objects.requireNonNull(paginationPolicy, "paginationPolicy");
        this.tablePaginator = new TablePaginator(new TableBlockLayouter(this.paragraphLayouter));
        this.atomicBlockPaginator = new AtomicBlockPaginator();
        this.paragraphPaginator = new ParagraphPaginator(this.paginationPolicy);
        this.incrementalLayoutPlanner = new IncrementalLayoutPlanner();
        this.verticalGlueDistributor = new VerticalGlueDistributor();
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
        if (reusableCache != null
            && !incrementalLayoutPlanner.canReuseIncrementallyWithBlocks(reusableCache.blocks(), inputs.blocks())) {
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
        IncrementalLayoutPlan plan = incrementalLayoutPlanner.plan(reusableCache, inputs, document.blocks());

        Map<ParagraphLayoutKey, List<LaidOutLine>> nextParagraphCache = reusableCache == null
            ? new HashMap<>()
            : new HashMap<>(reusableCache.paragraphLayouts());

        List<LaidOutPage> pages = reusablePrefixPages(reusableCache, plan.reusablePrefixPageCount());
        DefaultPageFlowState state = plan.reusablePrefixPageCount() == 0
            ? DefaultPageFlowState.firstPage(pageStyle)
            : DefaultPageFlowState.subsequentPage(pageStyle, plan.reusablePrefixPageCount());

        appendDocumentBlocks(document, theme, pageStyle, nextParagraphCache, pages, state, plan);
        appendFallbackEmptyBlockIfNeeded(pageStyle, state);
        pages.add(new LaidOutPage(state.pageIndex(), pageStyle.width(), pageStyle.height(), state.currentBlocks()));

        List<LaidOutPage> balancedPages = verticalGlueDistributor.apply(pageStyle, pages, paginationPolicy.verticalGluePolicy());
        LaidOutDocument laidOutDocument = new LaidOutDocument(pageStyle, balancedPages);
        return new LayoutComputation(nextParagraphCache, laidOutDocument);
    }

    private List<LaidOutPage> reusablePrefixPages(LayoutCache reusableCache, int reusablePrefixPageCount) {
        List<LaidOutPage> pages = new ArrayList<>();
        if (reusableCache != null && reusablePrefixPageCount > 0) {
            pages.addAll(reusableCache.document().pages().subList(0, reusablePrefixPageCount));
        }
        return pages;
    }

    private void appendDocumentBlocks(
        Document document,
        LayoutTheme theme,
        PageStyle pageStyle,
        Map<ParagraphLayoutKey, List<LaidOutLine>> paragraphCache,
        List<LaidOutPage> pages,
        PageFlowState state,
        IncrementalLayoutPlan plan
    ) {
        ListLayoutState listLayoutState = new ListLayoutState();
        listLayoutState.replayBeforeParagraph(document.blocks(), plan.relayoutStartParagraph());

        List<Paragraph> paragraphs = document.paragraphs();
        int paragraphIndex = 0;
        List<Block> documentBlocks = document.blocks();
        for (int sourceBlockIndex = 0; sourceBlockIndex < documentBlocks.size(); sourceBlockIndex++) {
            Block block = documentBlocks.get(sourceBlockIndex);
            if (sourceBlockIndex < plan.relayoutStartBlockIndex()) {
                if (block instanceof ParagraphBlock) {
                    paragraphIndex += 1;
                }
                continue;
            }

            if (block instanceof ParagraphBlock paragraphBlock) {
                appendParagraphBlock(
                    pageStyle,
                    theme,
                    paragraphCache,
                    pages,
                    state,
                    paragraphs,
                    paragraphIndex,
                    paragraphBlock.paragraph(),
                    listLayoutState
                );
                paragraphIndex += 1;
            } else {
                listLayoutState.reset();
                appendNonParagraphBlock(pageStyle, theme, pages, state, sourceBlockIndex, block);
            }
        }
    }

    private void appendParagraphBlock(
        PageStyle pageStyle,
        LayoutTheme theme,
        Map<ParagraphLayoutKey, List<LaidOutLine>> paragraphCache,
        List<LaidOutPage> pages,
        PageFlowState state,
        List<Paragraph> paragraphs,
        int paragraphIndex,
        Paragraph paragraph,
        ListLayoutState listLayoutState
    ) {
        ListLayout listLayout = listLayoutState.layoutFor(pageStyle, paragraph);
        List<LaidOutLine> lines = layoutParagraph(
            paragraph,
            theme.bodyFont(),
            listLayout.contentWidth(),
            theme.bodyLineGap(),
            paragraphCache
        );
        paragraphPaginator.appendParagraph(pageStyle, paragraphs, pages, state, paragraphIndex, paragraph, lines, listLayout);
    }

    private void appendNonParagraphBlock(
        PageStyle pageStyle,
        LayoutTheme theme,
        List<LaidOutPage> pages,
        PageFlowState state,
        int sourceBlockIndex,
        Block block
    ) {
        if (block instanceof TableBlock tableBlock) {
            tablePaginator.appendTable(pageStyle, theme, pages, state, sourceBlockIndex, tableBlock);
        } else if (block instanceof ImageBlock imageBlock) {
            atomicBlockPaginator.appendImage(pageStyle, pages, state, sourceBlockIndex, imageBlock);
        } else if (block instanceof FormulaBlock formulaBlock) {
            atomicBlockPaginator.appendFormula(pageStyle, theme, pages, state, sourceBlockIndex, formulaBlock);
        } else if (block instanceof HorizontalRuleBlock horizontalRuleBlock) {
            atomicBlockPaginator.appendHorizontalRule(pageStyle, pages, state, sourceBlockIndex, horizontalRuleBlock);
        }
    }

    private void appendFallbackEmptyBlockIfNeeded(PageStyle pageStyle, PageFlowState state) {
        if (!state.currentBlocks().isEmpty()) {
            return;
        }
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
}
