package io.github.ggeorg.delos.writer.layout;

import java.util.List;
import java.util.Objects;

/**
 * One laid-out table row. Coordinates are relative to the containing table.
 */
public record LaidOutTableRow(
        double y,
        double height,
        List<LaidOutTableCell> cells
) {
    public LaidOutTableRow {
        height = Math.max(0.0, height);
        cells = List.copyOf(Objects.requireNonNull(cells, "cells"));
    }
}
