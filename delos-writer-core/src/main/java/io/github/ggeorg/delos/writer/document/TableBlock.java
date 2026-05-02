package io.github.ggeorg.delos.writer.document;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Block-level table foundation.
 *
 * <p>Tables keep reporting-oriented metadata in the document model while the
 * layout/render layers decide how that metadata becomes pages, borders, and PDF
 * drawing commands.</p>
 */
public record TableBlock(
        List<TableRow> rows,
        List<TableColumnSpec> columns,
        int headerRowCount,
        TableStyle style
) implements Block {
    public TableBlock(List<TableRow> rows) {
        this(rows, equalColumnsFor(rows), 0, TableStyle.defaults());
    }

    public TableBlock(List<TableRow> rows, List<TableColumnSpec> columns) {
        this(rows, columns, 0, TableStyle.defaults());
    }

    public TableBlock(List<TableRow> rows, List<TableColumnSpec> columns, int headerRowCount) {
        this(rows, columns, headerRowCount, TableStyle.defaults());
    }

    public TableBlock {
        rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("table must contain at least one row");
        }
        int columnCount = rows.getFirst().cells().size();
        for (TableRow row : rows) {
            if (row.cells().size() != columnCount) {
                throw new IllegalArgumentException("all table rows must have the same number of cells");
            }
        }
        columns = normalizeColumns(columns, columnCount);
        if (headerRowCount < 0 || headerRowCount > rows.size()) {
            throw new IllegalArgumentException("headerRowCount must be between 0 and row count");
        }
        style = Objects.requireNonNullElse(style, TableStyle.defaults());
    }

    public static TableBlock blank(int rows, int columns) {
        if (rows <= 0) {
            throw new IllegalArgumentException("rows must be > 0");
        }
        if (columns <= 0) {
            throw new IllegalArgumentException("columns must be > 0");
        }
        return new TableBlock(IntStream.range(0, rows)
                .mapToObj(index -> TableRow.blank(columns))
                .toList());
    }

    public int rowCount() {
        return rows.size();
    }

    public int columnCount() {
        return rows.getFirst().cells().size();
    }

    public TableBlock withColumns(List<TableColumnSpec> columns) {
        return new TableBlock(rows, columns, headerRowCount, style);
    }

    public TableBlock withColumnWeights(double... weights) {
        Objects.requireNonNull(weights, "weights");
        return withColumns(IntStream.range(0, weights.length)
                .mapToObj(index -> new TableColumnSpec(weights[index]))
                .toList());
    }

    public TableBlock withHeaderRowCount(int headerRowCount) {
        return new TableBlock(rows, columns, headerRowCount, style);
    }

    public TableBlock withStyle(TableStyle style) {
        return new TableBlock(rows, columns, headerRowCount, style);
    }

    public boolean isHeaderRow(int rowIndex) {
        return rowIndex >= 0 && rowIndex < headerRowCount;
    }

    @Override
    public BlockKind kind() {
        return BlockKind.TABLE;
    }

    private static List<TableColumnSpec> equalColumnsFor(List<TableRow> rows) {
        Objects.requireNonNull(rows, "rows");
        if (rows.isEmpty()) {
            return List.of();
        }
        return IntStream.range(0, rows.getFirst().cells().size())
                .mapToObj(index -> TableColumnSpec.equal())
                .toList();
    }

    private static List<TableColumnSpec> normalizeColumns(List<TableColumnSpec> columns, int columnCount) {
        if (columnCount <= 0) {
            return List.of();
        }
        if (columns == null || columns.isEmpty()) {
            return IntStream.range(0, columnCount)
                    .mapToObj(index -> TableColumnSpec.equal())
                    .toList();
        }
        if (columns.size() != columnCount) {
            throw new IllegalArgumentException("column spec count must match table column count");
        }
        return List.copyOf(columns);
    }
}
