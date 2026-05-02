package io.github.ggeorg.delos.writer.render;

import io.github.ggeorg.delos.render.RenderColor;

/** CSS-hex parser for renderer-neutral table colors stored in the model. */
final class TableColorParser {
    private TableColorParser() {
    }

    static RenderColor parseOrNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (normalized.length() == 3) {
            normalized = "" + normalized.charAt(0) + normalized.charAt(0)
                    + normalized.charAt(1) + normalized.charAt(1)
                    + normalized.charAt(2) + normalized.charAt(2);
        }
        if (normalized.length() != 6 || !normalized.matches("[0-9a-fA-F]{6}")) {
            return null;
        }
        int red = Integer.parseInt(normalized.substring(0, 2), 16);
        int green = Integer.parseInt(normalized.substring(2, 4), 16);
        int blue = Integer.parseInt(normalized.substring(4, 6), 16);
        return RenderColor.rgb(red, green, blue);
    }
}
