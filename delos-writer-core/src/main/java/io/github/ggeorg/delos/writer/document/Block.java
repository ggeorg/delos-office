package io.github.ggeorg.delos.writer.document;

/**
 * Top-level block abstraction for Delos Writer content.
 *
 * <p>The live editor path is still paragraph-first, but the domain model now
 * has explicit homes for future rich blocks. Lists remain paragraph metadata;
 * images can be inline later or represented as image blocks; tables are block
 * content containing cell paragraphs.</p>
 */
public sealed interface Block permits ParagraphBlock, TableBlock, ImageBlock, HorizontalRuleBlock, FormulaBlock {
    BlockKind kind();
}
