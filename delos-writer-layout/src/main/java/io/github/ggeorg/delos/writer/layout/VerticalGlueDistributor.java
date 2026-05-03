package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.PageStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies optional vertical gap distribution after page flow pagination.
 */
final class VerticalGlueDistributor {
    List<LaidOutPage> apply(
        PageStyle pageStyle,
        List<LaidOutPage> pages,
        PaginatingDocumentLayoutEngine.VerticalGluePolicy policy
    ) {
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
            adjusted.add(applyToPage(pageStyle, pages.get(i), policy));
        }
        return List.copyOf(adjusted);
    }

    private LaidOutPage applyToPage(
        PageStyle pageStyle,
        LaidOutPage page,
        PaginatingDocumentLayoutEngine.VerticalGluePolicy policy
    ) {
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
                targets.add(new GapTarget(i));
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

    private record GapTarget(int beforeBlockIndex) { }
}
