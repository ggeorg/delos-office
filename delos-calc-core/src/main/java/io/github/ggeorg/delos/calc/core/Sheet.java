package io.github.ggeorg.delos.calc.core;

import java.util.Collection;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Immutable sparse spreadsheet sheet.
 */
public final class Sheet {
    private final String name;
    private final NavigableMap<CellAddress, Cell> cells;

    public Sheet(String name) {
        this(name, new TreeMap<>());
    }

    private Sheet(String name, NavigableMap<CellAddress, Cell> cells) {
        this.name = normalizeName(name);
        this.cells = Collections.unmodifiableNavigableMap(new TreeMap<>(Objects.requireNonNull(cells, "cells")));
    }

    public static Sheet named(String name) {
        return new Sheet(name);
    }

    public String name() {
        return name;
    }

    public Collection<Cell> cells() {
        return cells.values();
    }

    public Optional<Cell> findCell(CellAddress address) {
        return Optional.ofNullable(cells.get(Objects.requireNonNull(address, "address")));
    }

    public Cell cellAt(CellAddress address) {
        return findCell(address).orElseGet(() -> Cell.blank(address));
    }

    public Sheet withCell(CellAddress address, CellContent content) {
        Objects.requireNonNull(address, "address");
        CellContent safeContent = Objects.requireNonNullElse(content, CellContent.blank());
        Cell current = cells.get(address);
        if (safeContent.isBlank() && current == null) {
            return this;
        }
        if (current != null && current.content().equals(safeContent)) {
            return this;
        }

        NavigableMap<CellAddress, Cell> updated = new TreeMap<>(cells);
        if (safeContent.isBlank()) {
            updated.remove(address);
        } else {
            updated.put(address, new Cell(address, safeContent));
        }
        return new Sheet(name, updated);
    }

    public Sheet withInput(CellAddress address, String input) {
        return withCell(address, CellContent.parseInput(input));
    }

    public Sheet clear(CellAddress address) {
        return withCell(address, CellContent.blank());
    }

    public int usedCellCount() {
        return cells.size();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Sheet sheet)) {
            return false;
        }
        return name.equals(sheet.name) && cells.equals(sheet.cells);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, cells);
    }

    @Override
    public String toString() {
        return "Sheet[name=" + name + ", usedCellCount=" + usedCellCount() + "]";
    }

    private static String normalizeName(String value) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Sheet name must not be blank");
        }
        if (normalized.length() > 31) {
            throw new IllegalArgumentException("Sheet name must be 31 characters or fewer: " + normalized);
        }
        if (normalized.indexOf('[') >= 0
                || normalized.indexOf(']') >= 0
                || normalized.indexOf(':') >= 0
                || normalized.indexOf('*') >= 0
                || normalized.indexOf('?') >= 0
                || normalized.indexOf('/') >= 0
                || normalized.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("Sheet name contains an invalid character: " + normalized);
        }
        return normalized;
    }
}
