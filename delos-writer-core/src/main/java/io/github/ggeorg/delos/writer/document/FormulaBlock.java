package io.github.ggeorg.delos.writer.document;

import java.util.Objects;

/**
 * Block-level formula/equation source.
 *
 * <p>Formulas are source-first. Rendering may use a conservative fallback until
 * a full math layout engine is attached, while the document model stores the
 * canonical formula text and source format.</p>
 */
public record FormulaBlock(FormulaSourceFormat sourceFormat, String source, String altText) implements Block {
    public FormulaBlock(String source) {
        this(FormulaSourceFormat.LATEX, source, "");
    }

    public FormulaBlock(String source, String altText) {
        this(FormulaSourceFormat.LATEX, source, altText);
    }

    public FormulaBlock {
        sourceFormat = Objects.requireNonNullElse(sourceFormat, FormulaSourceFormat.LATEX);
        source = Objects.requireNonNullElse(source, "").strip();
        altText = Objects.requireNonNullElse(altText, "").strip();
    }

    @Override
    public BlockKind kind() {
        return BlockKind.FORMULA;
    }
}
