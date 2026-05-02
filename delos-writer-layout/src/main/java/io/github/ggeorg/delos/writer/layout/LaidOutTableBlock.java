package io.github.ggeorg.delos.writer.layout;

import java.util.List;
import java.util.Objects;

/**
 * Positioned table block.
 *
 * <p>v1 keeps tables deliberately simple: no merged cells and no row splitting
 * across pages yet. The block still carries model-derived style flags so render
 * and PDF paths consume the document model, not editor-only state.</p>
 */
public record LaidOutTableBlock(
        int sourceBlockIndex,
        double x,
        double y,
        double width,
        double height,
        List<LaidOutTableRow> rows,
        boolean bordersEnabled
) implements LaidOutAtomicBlock {
    public LaidOutTableBlock(double x, double y, double width, double height, List<LaidOutTableRow> rows) {
        this(-1, x, y, width, height, rows, true);
    }

    public LaidOutTableBlock(int sourceBlockIndex, double x, double y, double width, double height, List<LaidOutTableRow> rows) {
        this(sourceBlockIndex, x, y, width, height, rows, true);
    }

    public LaidOutTableBlock {
        width = Math.max(0.0, width);
        height = Math.max(0.0, height);
        rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
    }

    public int rowCount() {
        return rows.size();
    }

    public int columnCount() {
        return rows.isEmpty() ? 0 : rows.getFirst().cells().size();
    }

    @Override
    public LaidOutTableBlock withY(double y) {
        return new LaidOutTableBlock(sourceBlockIndex, x, y, width, height, rows, bordersEnabled);
    }
}
