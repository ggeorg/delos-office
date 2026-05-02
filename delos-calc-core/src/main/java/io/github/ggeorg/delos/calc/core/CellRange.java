package io.github.ggeorg.delos.calc.core;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Immutable rectangular range of cells, inclusive on all sides.
 */
public record CellRange(CellAddress first, CellAddress last) {
    public CellRange {
        first = Objects.requireNonNull(first, "first");
        last = Objects.requireNonNull(last, "last");
        int top = Math.min(first.rowIndex(), last.rowIndex());
        int left = Math.min(first.columnIndex(), last.columnIndex());
        int bottom = Math.max(first.rowIndex(), last.rowIndex());
        int right = Math.max(first.columnIndex(), last.columnIndex());
        first = CellAddress.of(top, left);
        last = CellAddress.of(bottom, right);
    }

    public static CellRange single(CellAddress address) {
        return new CellRange(address, address);
    }

    public static CellRange between(CellAddress anchor, CellAddress focus) {
        return new CellRange(anchor, focus);
    }

    public int topRowIndex() {
        return first.rowIndex();
    }

    public int leftColumnIndex() {
        return first.columnIndex();
    }

    public int bottomRowIndex() {
        return last.rowIndex();
    }

    public int rightColumnIndex() {
        return last.columnIndex();
    }

    public int rowCount() {
        return bottomRowIndex() - topRowIndex() + 1;
    }

    public int columnCount() {
        return rightColumnIndex() - leftColumnIndex() + 1;
    }

    public long cellCount() {
        return (long) rowCount() * (long) columnCount();
    }

    public boolean isSingleCell() {
        return first.equals(last);
    }

    public boolean contains(CellAddress address) {
        Objects.requireNonNull(address, "address");
        return address.rowIndex() >= topRowIndex()
                && address.rowIndex() <= bottomRowIndex()
                && address.columnIndex() >= leftColumnIndex()
                && address.columnIndex() <= rightColumnIndex();
    }

    public Stream<CellAddress> addresses() {
        Stream.Builder<CellAddress> builder = Stream.builder();
        for (int row = topRowIndex(); row <= bottomRowIndex(); row++) {
            for (int column = leftColumnIndex(); column <= rightColumnIndex(); column++) {
                builder.add(CellAddress.of(row, column));
            }
        }
        return builder.build();
    }

    public String toA1() {
        return isSingleCell() ? first.toA1() : first.toA1() + ":" + last.toA1();
    }

    @Override
    public String toString() {
        return toA1();
    }
}
