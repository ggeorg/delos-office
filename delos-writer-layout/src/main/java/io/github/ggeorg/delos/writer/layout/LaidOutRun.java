package io.github.ggeorg.delos.writer.layout;

import io.github.ggeorg.delos.writer.document.CharacterStyle;

import java.util.Objects;

/**
 * One visual run inside a line.
 */
public record LaidOutRun(
        String text,
        int startColumn,
        int endColumn,
        double x,
        double width,
        CharacterStyle style
) {
    public LaidOutRun {
        text = Objects.requireNonNullElse(text, "");
        style = Objects.requireNonNullElse(style, CharacterStyle.PLAIN);
    }

    public boolean bold() {
        return style.bold();
    }

    public boolean italic() {
        return style.italic();
    }

    public boolean underline() {
        return style.underline() || style.linked();
    }

    public boolean strikethrough() {
        return style.strikethrough();
    }
}
