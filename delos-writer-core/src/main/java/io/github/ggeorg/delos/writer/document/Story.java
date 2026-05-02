package io.github.ggeorg.delos.writer.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reusable editable content flow.
 *
 * <p>A story is the common content container for the document body, table cells,
 * and future containers such as headers, footers, footnotes, and text boxes.</p>
 *
 * <p>The model deliberately stays passive: editing operations belong in editor
 * services, not on this value object.</p>
 */
public record Story(List<Block> blocks) {
    public Story {
        blocks = List.copyOf(Objects.requireNonNull(blocks, "blocks").stream()
                .map(block -> Objects.requireNonNull(block, "block"))
                .toList());
        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("story must contain at least one block");
        }
    }

    public static Story ofBlocks(List<Block> blocks) {
        return new Story(blocks);
    }

    public static Story ofParagraphs(List<Paragraph> paragraphs) {
        Objects.requireNonNull(paragraphs, "paragraphs");
        if (paragraphs.isEmpty()) {
            throw new IllegalArgumentException("story must contain at least one paragraph");
        }
        return new Story(paragraphs.stream()
                .map(paragraph -> new ParagraphBlock(Objects.requireNonNull(paragraph, "paragraph")))
                .map(Block.class::cast)
                .toList());
    }

    public static Story blank() {
        return ofParagraphs(List.of(Paragraph.of("")));
    }

    /**
     * Paragraph projection for the current paragraph-first editor/layout stack.
     *
     * <p>Non-paragraph blocks are skipped. Full-fidelity consumers should use
     * {@link #blocks()}.</p>
     */
    public List<Paragraph> paragraphs() {
        List<Paragraph> paragraphs = new ArrayList<>();
        for (Block block : blocks) {
            if (block instanceof ParagraphBlock paragraphBlock) {
                paragraphs.add(paragraphBlock.paragraph());
            }
        }
        if (paragraphs.isEmpty()) {
            return List.of(Paragraph.of(""));
        }
        return List.copyOf(paragraphs);
    }

    public Story withBlocks(List<Block> blocks) {
        return ofBlocks(blocks);
    }
}
