package io.github.ggeorg.delos.writer.layout;

import java.util.List;
import java.util.Objects;

/**
 * Laid-out fragment of a source paragraph on a page.
 *
 * <p>A single source paragraph may produce multiple {@code LaidOutTextBlock}s
 * when it spans pages. The {@code paragraphIndex} maps each fragment back to
 * the source story.</p>
 */
public record LaidOutTextBlock(
        BlockRole role,
        double x,
        double y,
        double width,
        double height,
        int sourceParagraphIndex,
        int startLineIndex,
        boolean firstFragment,
        boolean lastFragment,
        List<LaidOutLine> lines,
        LaidOutListMarker listMarker
) implements LaidOutBlock {
    public LaidOutTextBlock(
            BlockRole role,
            double x,
            double y,
            double width,
            double height,
            int sourceParagraphIndex,
            int startLineIndex,
            boolean firstFragment,
            boolean lastFragment,
            List<LaidOutLine> lines
    ) {
        this(role, x, y, width, height, sourceParagraphIndex, startLineIndex, firstFragment, lastFragment, lines, LaidOutListMarker.none());
    }

    public LaidOutTextBlock {
        role = Objects.requireNonNull(role, "role");
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        listMarker = listMarker == null ? LaidOutListMarker.none() : listMarker;
    }

    public int endLineIndexExclusive() {
        return startLineIndex + lines.size();
    }

    public boolean isContinuationFragment() {
        return !firstFragment || !lastFragment;
    }
}
