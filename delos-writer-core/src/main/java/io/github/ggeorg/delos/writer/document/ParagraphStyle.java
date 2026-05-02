package io.github.ggeorg.delos.writer.document;

/**
 * Block-level paragraph styling.
 */
public record ParagraphStyle(
        Alignment alignment,
        double firstLineIndent,
        double spacingBefore,
        double spacingAfter,
        double lineSpacingMultiplier,
        ParagraphListStyle listStyle
) {
    public ParagraphStyle(
            Alignment alignment,
            double firstLineIndent,
            double spacingBefore,
            double spacingAfter,
            double lineSpacingMultiplier
    ) {
        this(alignment, firstLineIndent, spacingBefore, spacingAfter, lineSpacingMultiplier, ParagraphListStyle.none());
    }

    public ParagraphStyle {
        alignment = alignment == null ? Alignment.LEFT : alignment;
        listStyle = listStyle == null ? ParagraphListStyle.none() : listStyle;
        if (lineSpacingMultiplier <= 0) {
            throw new IllegalArgumentException("lineSpacingMultiplier must be > 0");
        }
    }

    public static ParagraphStyle defaultBody() {
        return new ParagraphStyle(Alignment.LEFT, 0, 0, 14, 1.0);
    }

    public static ParagraphStyle defaultCentered() {
        return new ParagraphStyle(Alignment.CENTER, 0, 8, 18, 1.0);
    }

    public static ParagraphStyle defaultHeading() {
        return new ParagraphStyle(Alignment.LEFT, 0, 10, 18, 1.0);
    }

    public ParagraphStyle withAlignment(Alignment alignment) {
        return new ParagraphStyle(alignment, firstLineIndent, spacingBefore, spacingAfter, lineSpacingMultiplier, listStyle);
    }

    public ParagraphStyle withFirstLineIndent(double firstLineIndent) {
        return new ParagraphStyle(alignment, firstLineIndent, spacingBefore, spacingAfter, lineSpacingMultiplier, listStyle);
    }

    public ParagraphStyle withSpacingBefore(double spacingBefore) {
        return new ParagraphStyle(alignment, firstLineIndent, spacingBefore, spacingAfter, lineSpacingMultiplier, listStyle);
    }

    public ParagraphStyle withSpacingAfter(double spacingAfter) {
        return new ParagraphStyle(alignment, firstLineIndent, spacingBefore, spacingAfter, lineSpacingMultiplier, listStyle);
    }

    public ParagraphStyle withLineSpacingMultiplier(double lineSpacingMultiplier) {
        return new ParagraphStyle(alignment, firstLineIndent, spacingBefore, spacingAfter, lineSpacingMultiplier, listStyle);
    }

    public ParagraphStyle withListStyle(ParagraphListStyle listStyle) {
        return new ParagraphStyle(alignment, firstLineIndent, spacingBefore, spacingAfter, lineSpacingMultiplier, listStyle);
    }

    public ParagraphStyle asBulletListItem(int level) {
        return withListStyle(ParagraphListStyle.bullet(level));
    }

    public ParagraphStyle asNumberedListItem(int level, int start) {
        return withListStyle(ParagraphListStyle.numbered(level, start));
    }

    public ParagraphStyle withoutListStyle() {
        return withListStyle(ParagraphListStyle.none());
    }

    public boolean isListItem() {
        return listStyle.enabled();
    }
}
