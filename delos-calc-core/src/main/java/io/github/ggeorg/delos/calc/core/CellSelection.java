package io.github.ggeorg.delos.calc.core;

import java.util.Objects;

/**
 * Selection anchor/focus inside one sheet.
 */
public record CellSelection(String sheetName, CellAddress anchor, CellAddress focus) {
    public CellSelection {
        sheetName = normalizeSheetName(sheetName);
        anchor = Objects.requireNonNull(anchor, "anchor");
        focus = Objects.requireNonNull(focus, "focus");
    }

    public static CellSelection single(String sheetName, CellAddress address) {
        return new CellSelection(sheetName, address, address);
    }

    public CellRange range() {
        return CellRange.between(anchor, focus);
    }

    public boolean isSingleCell() {
        return anchor.equals(focus);
    }

    public CellSelection moveTo(CellAddress address) {
        return single(sheetName, address);
    }

    public CellSelection extendTo(CellAddress address) {
        return new CellSelection(sheetName, anchor, address);
    }

    private static String normalizeSheetName(String value) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("sheetName must not be blank");
        }
        return normalized;
    }
}
