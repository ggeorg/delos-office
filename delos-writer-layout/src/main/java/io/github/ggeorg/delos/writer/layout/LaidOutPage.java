package io.github.ggeorg.delos.writer.layout;

import java.util.List;

/**
 * One fully laid-out page in page-local coordinates.
 */
public record LaidOutPage(
        int pageIndex,
        double width,
        double height,
        List<LaidOutBlock> blocks
) {
    public LaidOutPage {
        blocks = List.copyOf(blocks);
    }
}
