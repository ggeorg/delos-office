package io.github.ggeorg.delos.writer.document;

import java.util.List;
import java.util.Objects;

/**
 * One table row made of cells.
 */
public record TableRow(List<TableCell> cells) {
    public TableRow {
        cells = List.copyOf(Objects.requireNonNull(cells, "cells"));
        if (cells.isEmpty()) {
            throw new IllegalArgumentException("table row must contain at least one cell");
        }
    }

    public static TableRow blank(int columns) {
        if (columns <= 0) {
            throw new IllegalArgumentException("columns must be > 0");
        }
        return new TableRow(java.util.stream.IntStream.range(0, columns)
                .mapToObj(index -> TableCell.blank())
                .toList());
    }
}
