package io.github.ggeorg.delos.writer.document;

import java.util.Objects;

/**
 * Inline text segment with immutable character styling.
 */
public record TextRun(String text, CharacterStyle style) {

    public TextRun {
        text = Objects.requireNonNull(text, "text");
        style = Objects.requireNonNullElse(style, CharacterStyle.PLAIN);
    }

    public TextRun(String text, boolean bold, boolean italic, boolean underline) {
        this(text, CharacterStyle.PLAIN
                .withBold(bold)
                .withItalic(italic)
                .withUnderline(underline));
    }

    public static TextRun plain(String text) {
        return new TextRun(text, CharacterStyle.PLAIN);
    }

    public boolean bold() {
        return style.bold();
    }

    public boolean italic() {
        return style.italic();
    }

    public boolean underline() {
        return style.underline();
    }

    public boolean strikethrough() {
        return style.strikethrough();
    }

    public String linkHref() {
        return style.linkHref();
    }

    public boolean linked() {
        return style.linked();
    }

    public boolean sameStyleAs(TextRun other) {
        return other != null && style.sameAs(other.style());
    }

    public TextRun withText(String text) {
        return new TextRun(text, style);
    }

    public TextRun withStyle(CharacterStyle style) {
        return new TextRun(text, style);
    }

    public TextRun withBold(boolean value) {
        return withStyle(style.withBold(value));
    }

    public TextRun withItalic(boolean value) {
        return withStyle(style.withItalic(value));
    }

    public TextRun withUnderline(boolean value) {
        return withStyle(style.withUnderline(value));
    }

    public TextRun withStrikethrough(boolean value) {
        return withStyle(style.withStrikethrough(value));
    }

    public TextRun withLinkHref(String value) {
        return withStyle(style.withLinkHref(value));
    }
}
