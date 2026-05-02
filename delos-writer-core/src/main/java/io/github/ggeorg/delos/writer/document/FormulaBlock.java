package io.github.ggeorg.delos.writer.document;

import java.util.Objects;

/**
 * Block-level formula stored as editable source, not as a generated image.
 *
 * <p>v65 keeps formulas deliberately source-first. Rendering uses a conservative
 * placeholder, while the canonical document content remains the formula source
 * itself so future renderers can target SVG, PDF, MathML, or a native Delos math
 * layout engine without changing the document model.</p>
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
