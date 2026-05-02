package io.github.ggeorg.delos.calc.core;

import java.util.Locale;
import java.util.Objects;

/**
 * Zero-based spreadsheet cell address with A1 parsing/formatting helpers.
 */
public record CellAddress(int rowIndex, int columnIndex) implements Comparable<CellAddress> {
    public static final int MAX_ROWS = 1_048_576;
    public static final int MAX_COLUMNS = 16_384;

    public CellAddress {
        if (rowIndex < 0 || rowIndex >= MAX_ROWS) {
            throw new IllegalArgumentException("rowIndex out of range: " + rowIndex);
        }
        if (columnIndex < 0 || columnIndex >= MAX_COLUMNS) {
            throw new IllegalArgumentException("columnIndex out of range: " + columnIndex);
        }
    }

    public static CellAddress of(int rowIndex, int columnIndex) {
        return new CellAddress(rowIndex, columnIndex);
    }

    public static CellAddress parse(String a1Address) {
        String value = Objects.requireNonNull(a1Address, "a1Address").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Cell address must not be blank");
        }

        int split = 0;
        while (split < value.length() && isAsciiLetter(value.charAt(split))) {
            split++;
        }

        if (split == 0 || split == value.length()) {
            throw new IllegalArgumentException("Invalid A1 cell address: " + a1Address);
        }

        String columnPart = value.substring(0, split).toUpperCase(Locale.ROOT);
        String rowPart = value.substring(split);
        if (!rowPart.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Invalid A1 cell address: " + a1Address);
        }

        int columnIndex = parseColumnIndex(columnPart);
        int rowNumber = Integer.parseInt(rowPart);
        if (rowNumber < 1) {
            throw new IllegalArgumentException("Row number must be one-based: " + a1Address);
        }

        return new CellAddress(rowNumber - 1, columnIndex);
    }

    public String toA1() {
        return columnName(columnIndex) + (rowIndex + 1);
    }

    @Override
    public String toString() {
        return toA1();
    }

    @Override
    public int compareTo(CellAddress other) {
        Objects.requireNonNull(other, "other");
        int row = Integer.compare(rowIndex, other.rowIndex);
        return row != 0 ? row : Integer.compare(columnIndex, other.columnIndex);
    }

    public static String columnName(int zeroBasedColumnIndex) {
        if (zeroBasedColumnIndex < 0 || zeroBasedColumnIndex >= MAX_COLUMNS) {
            throw new IllegalArgumentException("columnIndex out of range: " + zeroBasedColumnIndex);
        }

        StringBuilder result = new StringBuilder();
        int value = zeroBasedColumnIndex + 1;
        while (value > 0) {
            value--;
            result.append((char) ('A' + value % 26));
            value /= 26;
        }
        return result.reverse().toString();
    }

    private static int parseColumnIndex(String columnPart) {
        int result = 0;
        for (int i = 0; i < columnPart.length(); i++) {
            char ch = columnPart.charAt(i);
            if (ch < 'A' || ch > 'Z') {
                throw new IllegalArgumentException("Invalid column name: " + columnPart);
            }
            result = result * 26 + (ch - 'A' + 1);
        }
        return result - 1;
    }

    private static boolean isAsciiLetter(char ch) {
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
    }
}
