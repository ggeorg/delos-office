package io.github.ggeorg.delos.calc.core;

import java.util.Objects;

/**
 * Immutable spreadsheet cell.
 */
public record Cell(CellAddress address, CellContent content) {
    public Cell {
        address = Objects.requireNonNull(address, "address");
        content = Objects.requireNonNullElse(content, CellContent.blank());
    }

    public static Cell blank(CellAddress address) {
        return new Cell(address, CellContent.blank());
    }

    public Cell withContent(CellContent content) {
        return new Cell(address, content);
    }

    public boolean isBlank() {
        return content.isBlank();
    }
}
