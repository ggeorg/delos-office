package io.github.ggeorg.delos.writer.editor;

/**
 * Supported inline text styles for the current Delos milestone.
 */
public enum TextStyle {
    BOLD("Toggle Bold"),
    ITALIC("Toggle Italic"),
    UNDERLINE("Toggle Underline"),
    STRIKETHROUGH("Toggle Strikethrough");

    private final String description;

    TextStyle(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
