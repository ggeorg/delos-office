package io.github.ggeorg.delos.writer.document;

import java.util.Locale;

/**
 * Block-level paragraph styling.
 */
public record ParagraphStyle(
        Alignment alignment,
        double firstLineIndent,
        double spacingBefore,
        double spacingAfter,
        double lineSpacingMultiplier,
        ParagraphListStyle listStyle,
        String languageTag
) {
    public static final String DEFAULT_LANGUAGE_TAG = "en-US";

    public ParagraphStyle(
            Alignment alignment,
            double firstLineIndent,
            double spacingBefore,
            double spacingAfter,
            double lineSpacingMultiplier
    ) {
        this(alignment, firstLineIndent, spacingBefore, spacingAfter, lineSpacingMultiplier, ParagraphListStyle.none());
    }

    public ParagraphStyle(
            Alignment alignment,
            double firstLineIndent,
            double spacingBefore,
            double spacingAfter,
            double lineSpacingMultiplier,
            ParagraphListStyle listStyle
    ) {
        this(alignment, firstLineIndent, spacingBefore, spacingAfter, lineSpacingMultiplier, listStyle, DEFAULT_LANGUAGE_TAG);
    }

    public ParagraphStyle {
        alignment = alignment == null ? Alignment.LEFT : alignment;
        listStyle = listStyle == null ? ParagraphListStyle.none() : listStyle;
        languageTag = normalizeLanguageTag(languageTag);
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
        return new ParagraphStyle(alignment, firstLineIndent, spacingBefore, spacingAfter, lineSpacingMultiplier, listStyle, languageTag);
    }

    public ParagraphStyle withFirstLineIndent(double firstLineIndent) {
        return new ParagraphStyle(alignment, firstLineIndent, spacingBefore, spacingAfter, lineSpacingMultiplier, listStyle, languageTag);
    }

    public ParagraphStyle withSpacingBefore(double spacingBefore) {
        return new ParagraphStyle(alignment, firstLineIndent, spacingBefore, spacingAfter, lineSpacingMultiplier, listStyle, languageTag);
    }

    public ParagraphStyle withSpacingAfter(double spacingAfter) {
        return new ParagraphStyle(alignment, firstLineIndent, spacingBefore, spacingAfter, lineSpacingMultiplier, listStyle, languageTag);
    }

    public ParagraphStyle withLineSpacingMultiplier(double lineSpacingMultiplier) {
        return new ParagraphStyle(alignment, firstLineIndent, spacingBefore, spacingAfter, lineSpacingMultiplier, listStyle, languageTag);
    }

    public ParagraphStyle withListStyle(ParagraphListStyle listStyle) {
        return new ParagraphStyle(alignment, firstLineIndent, spacingBefore, spacingAfter, lineSpacingMultiplier, listStyle, languageTag);
    }

    public ParagraphStyle withLanguageTag(String languageTag) {
        return new ParagraphStyle(alignment, firstLineIndent, spacingBefore, spacingAfter, lineSpacingMultiplier, listStyle, languageTag);
    }

    public ParagraphStyle withoutLanguageTag() {
        return withLanguageTag(null);
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

    private static String normalizeLanguageTag(String value) {
        if (value == null) {
            return null;
        }
        String candidate = value.trim().replace('_', '-');
        if (candidate.isEmpty()) {
            return null;
        }
        String normalized = Locale.forLanguageTag(candidate).toLanguageTag();
        return "und".equals(normalized) ? candidate : normalized;
    }
}
