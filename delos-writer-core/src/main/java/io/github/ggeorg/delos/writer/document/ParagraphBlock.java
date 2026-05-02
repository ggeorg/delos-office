package io.github.ggeorg.delos.writer.document;

import java.util.Objects;

/**
 * Text paragraph as a block-level document element.
 */
public record ParagraphBlock(Paragraph paragraph) implements Block {
    public ParagraphBlock {
        paragraph = Objects.requireNonNull(paragraph, "paragraph");
    }

    public static ParagraphBlock of(Paragraph paragraph) {
        return new ParagraphBlock(paragraph);
    }

    @Override
    public BlockKind kind() {
        return BlockKind.PARAGRAPH;
    }
}
