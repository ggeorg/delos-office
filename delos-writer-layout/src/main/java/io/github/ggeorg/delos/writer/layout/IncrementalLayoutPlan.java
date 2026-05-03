package io.github.ggeorg.delos.writer.layout;

/**
 * Computed reuse boundary for an incremental pagination pass.
 */
record IncrementalLayoutPlan(
    int firstChangedParagraph,
    int relayoutAnchorParagraph,
    int reusablePrefixPageCount,
    int relayoutStartParagraph,
    int relayoutStartBlockIndex
) {
    static IncrementalLayoutPlan cold() {
        return new IncrementalLayoutPlan(0, 0, 0, 0, 0);
    }
}
