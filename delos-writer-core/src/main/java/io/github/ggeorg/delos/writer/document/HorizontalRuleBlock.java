package io.github.ggeorg.delos.writer.document;

/**
 * Horizontal rule placeholder block.
 */
public record HorizontalRuleBlock() implements Block {
    @Override
    public BlockKind kind() {
        return BlockKind.HORIZONTAL_RULE;
    }
}
