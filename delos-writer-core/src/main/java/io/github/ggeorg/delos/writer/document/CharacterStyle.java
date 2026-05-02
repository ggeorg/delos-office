package io.github.ggeorg.delos.writer.document;

/**
 * Immutable inline character styling for a text run.
 *
 * <p>Null font-related fields mean "inherit from the surrounding paragraph/theme".
 * Keeping style in one value object prevents TextRun from growing a new boolean or
 * scalar field every time Delos adds another inline formatting capability.</p>
 */
public record CharacterStyle(
        boolean bold,
        boolean italic,
        boolean underline,
        boolean strikethrough,
        String fontFamily,
        Double fontSize,
        String color,
        String linkHref
) {
    public static final CharacterStyle PLAIN = new CharacterStyle(
            false,
            false,
            false,
            false,
            null,
            null,
            null,
            null
    );

    public CharacterStyle(
            boolean bold,
            boolean italic,
            boolean underline,
            boolean strikethrough,
            String fontFamily,
            Double fontSize,
            String color
    ) {
        this(bold, italic, underline, strikethrough, fontFamily, fontSize, color, null);
    }

    public CharacterStyle {
        fontFamily = normalize(fontFamily);
        color = normalize(color);
        linkHref = normalize(linkHref);
        if (fontSize != null && fontSize <= 0) {
            throw new IllegalArgumentException("fontSize must be positive when specified");
        }
    }

    public CharacterStyle withBold(boolean value) {
        return new CharacterStyle(value, italic, underline, strikethrough, fontFamily, fontSize, color, linkHref);
    }

    public CharacterStyle withItalic(boolean value) {
        return new CharacterStyle(bold, value, underline, strikethrough, fontFamily, fontSize, color, linkHref);
    }

    public CharacterStyle withUnderline(boolean value) {
        return new CharacterStyle(bold, italic, value, strikethrough, fontFamily, fontSize, color, linkHref);
    }

    public CharacterStyle withStrikethrough(boolean value) {
        return new CharacterStyle(bold, italic, underline, value, fontFamily, fontSize, color, linkHref);
    }

    public CharacterStyle withFontFamily(String value) {
        return new CharacterStyle(bold, italic, underline, strikethrough, value, fontSize, color, linkHref);
    }

    public CharacterStyle withFontSize(Double value) {
        return new CharacterStyle(bold, italic, underline, strikethrough, fontFamily, value, color, linkHref);
    }

    public CharacterStyle withColor(String value) {
        return new CharacterStyle(bold, italic, underline, strikethrough, fontFamily, fontSize, value, linkHref);
    }

    public CharacterStyle withLinkHref(String value) {
        return new CharacterStyle(bold, italic, underline, strikethrough, fontFamily, fontSize, color, value);
    }

    public boolean linked() {
        return linkHref != null;
    }

    public boolean sameAs(CharacterStyle other) {
        return equals(other);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
