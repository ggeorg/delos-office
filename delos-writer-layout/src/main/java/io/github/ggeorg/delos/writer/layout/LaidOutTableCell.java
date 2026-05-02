package io.github.ggeorg.delos.writer.layout;

import java.util.List;
import java.util.Objects;

/**
 * One laid-out table cell. Coordinates are relative to the containing table.
 */
public record LaidOutTableCell(
        double x,
        double y,
        double width,
        double height,
        List<LaidOutTextBlock> textBlocks,
        boolean header,
        String backgroundColor
) {
    public LaidOutTableCell(double x, double y, double width, double height, List<LaidOutTextBlock> textBlocks) {
        this(x, y, width, height, textBlocks, false, null);
    }

    public LaidOutTableCell {
        width = Math.max(0.0, width);
        height = Math.max(0.0, height);
        textBlocks = List.copyOf(Objects.requireNonNull(textBlocks, "textBlocks"));
        backgroundColor = normalize(backgroundColor);
    }

    public boolean hasExplicitBackground() {
        return backgroundColor != null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
